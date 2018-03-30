package de.db.vz.integrationtestframework.specifications

import de.db.vz.integrationtestframework.config.ServiceUriResolver
import de.db.vz.integrationtestframework.adapter.rabbit.RabbitGroovyClient
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RabbitGroovyClientSpec extends Specification {

    @Shared
    RabbitGroovyClient rabbit = new RabbitGroovyClient()

    def setupSpec() {
        rabbit.setConnectionFactory(new CachingConnectionFactory(ServiceUriResolver.instance().resolveForServiceAndPort('rabbitmq', 5672, 'amqp')))
        rabbit.setAdmin(new RabbitAdmin(rabbit.getConnectionFactory()))

        rabbit.waitUntilConnectionFactoryAvailable()
        rabbit.declareQueueAndBindToDirectExchange("queue", "exchange")
    }

    def setup() {
        rabbit.purgeQueue("queue", true)
    }

    @Unroll("verify json sent #send and received #expect")
    def "verify json sent and received"(def send, def expect) {
        given:
        rabbit.sendJson("exchange", "queue", send)

        expect:
        rabbit.verifyJsonReceived("queue", expect)

        where:
        send              | expect
        "{'a':1,'b':'2'}" | "{'a':1,'b':'2'}"
        [a: 1, b: '2']    | [a: 1, b: '2']
        [a: 1, b: '2']    | "{'a':1,'b':'2'}"
        "{'a':1,'b':'2'}" | [a: 1, b: '2']
    }

    def "verify exception thrown when received json differs from sent"() {
        given:
        rabbit.sendJson("exchange", "queue", [a: '1', b: '2'])

        when:
        rabbit.verifyJsonReceived("queue", [a: '2', b: '1'])

        then:
        thrown AssertionError
    }
}
