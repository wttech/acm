import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

void respond(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(404)
    formatter.template.render(mock.resolvePath(repo.get("missing.html")).readFile(), [
            "request": request,
            "response": response,
            "logo": formatter.base64.encodeToString(repo.get(mock.resolvePath("logo-text.png")).readFile()),
    ], response.outputStream)
}