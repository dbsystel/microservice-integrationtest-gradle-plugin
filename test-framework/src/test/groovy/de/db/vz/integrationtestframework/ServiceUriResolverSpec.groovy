package de.db.vz.integrationtestframework

import de.db.vz.integrationtestframework.config.ServiceUriResolver
import spock.lang.Specification

class ServiceUriResolverSpec extends Specification {

    def "not in docker network: verify service does not exist"() {
        when:
        ServiceUriResolver.instance().resolveForService('notExisting')

        then:
        thrown RuntimeException.class
    }
}