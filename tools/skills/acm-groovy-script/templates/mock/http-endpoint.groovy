/*
---
version: '1.0'
category: mock
---
Simulates the ACME product API for test environments by answering `GET /mock/acme/products` with JSON. Needs the mock filter enabled.
*/

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

boolean request(HttpServletRequest request) {
    return request.method == 'GET' && request.requestURI == '/mock/acme/products'
}

void respond(HttpServletRequest request, HttpServletResponse response) {
    response.contentType = 'application/json'
    formatter.json.write(response.outputStream, [
        [id: 1, name: 'Product A'],
        [id: 2, name: 'Product B'],
    ])
}
