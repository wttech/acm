/*
A quick look at repository content, as bare statements for the ACM console or Run Selection, not for a stored script.

Lists pages below a path.
*/

def pages = repo.get('/content/acme/en').query('cq:Page').toList()
println "Pages: ${pages.size()}"
pages.take(10).each { println it.path }
