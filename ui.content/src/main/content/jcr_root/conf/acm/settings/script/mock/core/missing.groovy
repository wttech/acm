import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

void respond(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(404)
    formatter.template.render(repo.get(mock.resolvePath("missing.html")).readFileAsStream(), [
            "request": request,
            "response": response,
            "logo": formatter.base64.encodeToString(repo.get(mock.resolvePath("logo-text.png")).readFileAsStream()),
    ], response.outputStream)
}