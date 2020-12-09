
package util

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.language.postfixOps
import scala.util.parsing.combinator._

sealed trait Selector

final case class ArrayOrObjectSelector(key: String, selectors: List[Selector] = Nil)
  extends Selector

final case class KeySelector(key: String) extends Selector

object JsonTransformer {

  def filter(originalJson: JsValue,
             include: Option[String]): Either[String, JsValue] = {

    val requestedMeta = include match {
      case Some(fields) if fields.nonEmpty =>
        SelectorParser.parseSelector(fields) match {
          case Some(sel) => sel
          case None      => List.empty
        }
      case _ => List.empty
    }

    originalJson match {
      case jsObject: JsObject => {
        jsObject.transform(getTransformers(JsPath, requestedMeta)) match {
          case JsError(_) =>
            Left(
              "Error while transforming json"
            )
          case JsSuccess(js, _) => Right(js)
        }
      }

      case jsArray: JsArray => {
        JsObject(List(("key", jsArray)))
          .transform(
            getTransformers(__, List(ArrayOrObjectSelector("key", requestedMeta)))
          ) match {
          case JsError(_) =>
            Left(
              "Error while transforming json"
            )
          case JsSuccess(js, _) => Right(js.value.getOrElse("key", originalJson))
        }
      }
      case _ => Right(originalJson)
    }
  }

  def getTransformers(jsPath: JsPath,
                      selectors: List[Selector]): Reads[JsObject] =
    mergePickBranches(buildJsonTransformers(__, selectors))

  private def buildJsonTransformers(
                                     jsPath: JsPath,
                                     selectors: List[Selector]
                                   ): List[Reads[JsObject]] =
    selectors.map { selector =>
      jsPath.json.pickBranch(__.read[JsValue].map {
        case jsObject: JsObject => filterJsonObject(jsObject, selector)
        case jsArray: JsArray   => filterJsonArray(jsArray, selectors)
        case jsValue: JsValue   => jsValue
      })
    }

  private def filterJsonObject(jsObject: JsObject,
                               selector: Selector): JsObject =
    selector match {
      case sel: ArrayOrObjectSelector =>
        jsObject.transform(
          mergePickBranches(
            buildJsonTransformers(__ \ sel.key, sel.selectors)
          )
        ) match {
          case JsError(_)       => JsObject(List.empty)
          case JsSuccess(js, _) => js
        }
      case sel: KeySelector =>
        jsObject.transform((JsPath \ sel.key).json.pickBranch) match {
          case JsError(_)       => JsObject(List.empty)
          case JsSuccess(js, _) => js
        }
    }

  private def filterJsonArray(jsArray: JsArray,
                              selectors: List[Selector]): JsArray =
    JsArray(
      jsArray.value
        .map {
          case jsObject: JsObject =>
            jsObject.transform(
              mergePickBranches(buildJsonTransformers(JsPath, selectors))
            ) match {
              case JsError(_)       => JsObject(List.empty)
              case JsSuccess(js, _) => js
            }
          case jsArr: JsArray =>
            selectors.filter {
              case sel: ArrayOrObjectSelector => sel.key.isEmpty
              case _                  => false
            } match {
              case head :: _ =>
                filterJsonArray(jsArr, head match {
                  case selector: ArrayOrObjectSelector => selector.selectors
                  case _: KeySelector          => Nil
                })
              case Nil => JsArray()
            }
          case jsValue: JsValue => jsValue
        }
        .filter {
          case jsObject: JsObject => jsObject.keys.nonEmpty
          case jsArr: JsArray     => jsArr.value.nonEmpty
          case _                  => true
        }
    )

  private def mergePickBranches(paths: List[Reads[JsObject]]): Reads[JsObject] =
    paths reduceOption (_ and _ reduce) getOrElse JsPath.json.prune

  object SelectorParser extends JavaTokenParsers {

    def members: Parser[List[Selector]] =
      repsep(member, ",") ^^ (l => l flatten)

    def member: Parser[List[Selector]] =
      identifier ~ opt("(" ~> members <~ ")") ^^ {
        case name ~ memOpt =>
          memOpt match {
            case None    => List(KeySelector(name))
            case Some(l) => List(ArrayOrObjectSelector(name, l))
          }
      }

    def identifier: Parser[String] =
      """[a-zA-Z_]*""".r

    def parseSelector(s: String): Option[List[Selector]] =
      parseAll(members, s) match {
        case Success(matched, _) => Some(matched)
        case Failure(_, _)     => None
      }
  }
}

