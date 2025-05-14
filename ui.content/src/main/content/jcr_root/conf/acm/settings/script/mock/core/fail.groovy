import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.apache.commons.lang3.exception.ExceptionUtils

void fail(HttpServletRequest request, HttpServletResponse response, Exception exception) {
    response.setStatus(500)
    formatter.template.render(repo.get(mock.resolvePath("fail.html")).readFile(), [
            "request": request,
            "response": response,
            "exception": exception,

            "logo": repository.readAsBase64("logo-text.png"),
            "stackTrace": ExceptionUtils.getStackTrace(exception),
            "rootCause": ExceptionUtils.getRootCause(exception),
    ], response.outputStream)
}