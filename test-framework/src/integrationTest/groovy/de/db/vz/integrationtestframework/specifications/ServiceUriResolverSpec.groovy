package de.db.vz.integrationtestframework.specifications

import de.db.vz.integrationtestframework.mock.MockService
import de.db.vz.integrationtestframework.config.ServiceUriResolver
import groovy.json.JsonSlurper
import spock.lang.Specification

class ServiceUriResolverSpec extends Specification {

    def "verify url & port are resolved correctly"(def path, def response) {
        given:
        MockService mockService = new MockService(ServiceUriResolver.instance().resolveForServiceAndPort('mockservice', 8080))
        mockService.mockJsonResponse(path, response)

        when:
        def host = ServiceUriResolver.instance().resolveForServiceAndPort('mockservice', 8080).host
        def port = ServiceUriResolver.instance().resolveForServiceAndPort('mockservice', 8080).port
        def json = new JsonSlurper().parseText(new URL("http://$host:$port/$path").text)

        then:
        json == response

        where:
        path       | response
        '/default' | [key: 'value']
    }
}
