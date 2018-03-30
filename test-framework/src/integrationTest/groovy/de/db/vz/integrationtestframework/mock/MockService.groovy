package de.db.vz.integrationtestframework.mock

import groovy.json.JsonBuilder
import org.mockserver.client.server.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.Header

import static org.mockserver.model.HttpRequest.request
import static org.mockserver.model.HttpResponse.response

class MockService {

    private host
    private port

    MockService(URI uri) {
        this.host = uri.host
        this.port = uri.port
    }

    def mockJsonResponse(def path, def respo) {
        new MockServerClient(host, port).when(request().withMethod("GET")
                .withPath(path), Times.unlimited())
                .respond(response().withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                .withBody(new JsonBuilder(respo).toPrettyString()))
    }
}
