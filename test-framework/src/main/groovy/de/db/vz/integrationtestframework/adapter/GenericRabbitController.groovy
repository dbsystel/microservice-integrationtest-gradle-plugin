package de.db.vz.integrationtestframework.adapter

import de.db.vz.integrationtestframework.config.ServiceUriResolver
import de.db.vz.integrationtestframework.adapter.rabbit.RabbitGroovyClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin

class GenericRabbitController {

    protected String exchange
    protected String routingKey
    protected String testQueue
    protected def last

    protected RabbitGroovyClient rabbit
    protected JsonSlurper jsonSlurper = new JsonSlurper()

    GenericRabbitController(String exchange, String routingKey, String testQueuePrefix = null) {
        this.exchange = exchange
        this.routingKey = routingKey
        this.testQueue = testQueuePrefix ? "${testQueuePrefix}.$routingKey" : null

        rabbit = new RabbitGroovyClient()
        rabbit.setConnectionFactory(new CachingConnectionFactory(ServiceUriResolver.instance().resolveForServiceAndPort('rabbitmq', 5672, 'amqp')))
        rabbit.setAdmin(new RabbitAdmin(rabbit.getConnectionFactory()))

        waitForRabbitUp()

        if (testQueue) {
            declareQueue(testQueue)
        }
    }

    protected void declareQueue(queue) {
        rabbit.declareQueueAndBindToDirectExchange(queue,
                this.exchange,
                this.routingKey)
        rabbit.purgeQueue(queue, false)
    }


    protected void send(def data) {
        rabbit.sendJson(exchange, routingKey, new JsonBuilder(data).toString())
    }

    protected def receiveFromTestQueue() {
        receiveFrom(testQueue)
    }

    protected boolean verifyReceived(def data) {
        rabbit.verifyJsonReceived(testQueue, data, JSONCompareMode.LENIENT)
        true
    }

    protected def receiveFrom(String queue) {
        def res = rabbit.receive(queue)
        res == null ? null : jsonSlurper.parseText(new String(res.body))
    }

    protected void waitForRabbitUp() {
        rabbit.waitUntilConnectionFactoryAvailable()
    }
}
