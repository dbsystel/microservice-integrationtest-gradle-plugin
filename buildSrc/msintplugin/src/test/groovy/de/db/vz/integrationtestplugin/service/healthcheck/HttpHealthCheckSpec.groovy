package de.db.vz.integrationtestplugin.service.healthcheck

import spock.lang.Specification

class HttpHealthCheckSpec extends Specification {

    def "well formed http endpoint"(String healthEndpoint, boolean valid) {
        expect:
        HttpHealthCheck.validHealthEndpoint(healthEndpoint) == valid

        where:
        healthEndpoint      | valid
        ':8080'             | true
        ':8080/'            | true
        ':8080/health'      | true
        ':8080/info/health' | true
        ':80/health'        | true
        ':38123/health123'  | true
        ':1'                | false
        ':123456'           | false
        '/health123'        | false
        '/'                 | false
    }
}
