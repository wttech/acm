/**
 * Updates the thumbnail of a page in the repository.
 * Deletes the existing thumbnails and saves a new one.
 * File must be a JPEG image.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */

void describeRun() {
    arguments.path("pagePath") { rootPathExclusive = '/' }
    arguments.file("pageThumbnailFile") { mimeTypes = ["image/jpeg"] }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def page = repo.get(arguments.value("pagePath"))
    def pageImage = page.child("jcr:content/image").ensure("nt:unstructured")
    pageImage.child("file/jcr:content/dam:thumbnails").delete()
    pageImage.child("file").saveFile("image/jpeg", arguments.value("pageThumbnailFile"))
}