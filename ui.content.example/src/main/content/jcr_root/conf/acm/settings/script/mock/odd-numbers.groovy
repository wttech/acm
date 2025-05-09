import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


boolean request(HttpServletRequest request) {
    return request.getRequestURI() == "/mock/odd-numbers"
}

void respond(HttpServletRequest request, HttpServletResponse response) {
    def start = StringUtils.defaultIfBlank(request.getParameter("start"), "1") as int
    def end = StringUtils.defaultIfBlank(request.getParameter("end"), "100") as int
    def numbers = (start..end).findAll { it % 2 != 0 }

    response.setContentType("application/json; charset=UTF-8")
    response.getWriter().write(gson.toJson([
            "start": start,
            "end": end,
            "result": numbers
    ]))
}