```bash
http://localhost:9000/users?fields=id,name,addresses(city,pinCode)
```
Fields parameter syntax summary:

* Use a comma separated delimiter to separate fields. ex: fields=id,name
* Use fields=X(Y) for nested array and nexted object
* numbers are invalid after "fields="

eg: { a: { b:[ [ { c:val } ] ] } }, fields=a(b((c))).
