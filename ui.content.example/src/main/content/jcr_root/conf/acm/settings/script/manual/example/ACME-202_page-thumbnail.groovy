/*
---
author: <john.doe@acme.com>
---
Updates the thumbnail of a page in the repository.
Deletes the existing thumbnails and saves a new one.
File must be a JPEG image.
*/

void describeRun() {
    inputs.path("pagePath") { rootPathExclusive = '/' }
    inputs.file("pageThumbnailFile") { mimeTypes = ["image/jpeg"] }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def pageThumnail = repo.get(inputs.value("pageThumbnailFile"))
    def page = repo.get(inputs.value("pagePath"))
    try {
        def pageImage = page.child("jcr:content/image").ensure("nt:unstructured")
        pageImage.child("file").saveFile("image/jpeg", pageThumnail.readFileAsStream())
    } finally {
        pageThumnail.delete()
    }
}