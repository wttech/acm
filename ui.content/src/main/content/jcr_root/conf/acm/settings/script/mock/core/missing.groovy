import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

void respond(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(404)
    formatter.template.render(mock.resource.sibling("missing.html").readFileAsStream(), [
            "request": request,
            "response": response,
            "logo": formatter.base64.encodeToString(mock.resource.sibling("logo-text.png").readFileAsStream()),
    ], response.outputStream)
}