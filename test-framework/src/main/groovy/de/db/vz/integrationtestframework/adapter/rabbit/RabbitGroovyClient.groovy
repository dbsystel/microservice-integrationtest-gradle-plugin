package de.db.vz.integrationtestframework.adapter.rabbit

import de.db.vz.integrationtestframework.async.Retry
import groovy.json.JsonBuilder
import org.json.JSONException
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.Connection
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.MessageConversionException

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class RabbitGroovyClient extends RabbitTemplate implements AmqpAdmin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitGroovyClient.class)
    protected static final int DEFAULT_TIMEOUT = 2
    protected static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS

    private final MessageBuffer buffer = new MessageBuffer()

    private volatile RabbitAdmin admin

    void declareQueueAndBindToDirectExchange(String queueName, String exchangeName) {
        DirectExchange exchange = new DirectExchange(exchangeName)
        Queue queue = new Queue(queueName)
        declareExchange(exchange)
        declareQueue(queue)
        declareBinding(BindingBuilder.bind(queue).to(exchange).withQueueName())
    }

    void declareQueueAndBindToDirectExchange(String queueName, String exchangeName, String routingKey) {
        DirectExchange exchange = new DirectExchange(exchangeName)
        Queue queue = new Queue(queueName)
        declareExchange(exchange)
        declareQueue(queue)
        declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey))
    }

    void sendJson(String json) {
        send(jsonMessage(json))
    }

    void sendJson(def json) {
        send(objectToMessage(json))
    }

    void sendJson(String exchange, String routingKey, String json) {
        send(exchange, routingKey, jsonMessage(json))
    }

    void sendJson(String exchange, String routingKey, def json) {
        send(exchange, routingKey, objectToMessage(json))
    }

    void verifyJsonReceived(String queueName, String expectedJson) {
        verifyJsonReceived(queueName, expectedJson, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
    }

    boolean verifyJsonReceived(String queueName,
                               def expectedJson,
                               JSONCompareMode compareMode = JSONCompareMode.STRICT,
                               long timeout = DEFAULT_TIMEOUT,
                               TimeUnit unit = DEFAULT_TIMEOUT_UNIT) {
        verifyJsonReceived(queueName, new JsonBuilder(expectedJson).toString(), compareMode, timeout, unit)
        true
    }

    void verifyJsonReceived(String queueName, String expectedJson, JSONCompareMode compareMode) {
        verifyJsonReceived(queueName, expectedJson, compareMode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
    }

    void verifyJsonReceived(String queueName, String expectedJson,
                            long timeout, TimeUnit unit) {
        verifyJsonReceived(queueName, expectedJson, JSONCompareMode.STRICT_ORDER, timeout, unit)
    }

    void verifyJsonReceived(String queueName, String expectedJson,
                            JSONCompareMode compareMode, long timeout, TimeUnit unit) {
        Retry.withRetry(timeout, unit) { context ->
            Message received = receive(queueName)
            logReceivedMessage(queueName, received)
            owner.verifyMessageBufferContainsExpectedMessage(queueName, expectedJson, compareMode)
            return null
        }
    }

    RabbitAdmin getAdmin() {
        return admin
    }

    void setAdmin(RabbitAdmin admin) {
        this.admin = admin
    }

    protected static Message jsonMessage(String json)
            throws MessageConversionException {
        byte[] bytes = json.replace('\'', '"').getBytes(StandardCharsets.UTF_8)
        MessageProperties messageProperties = new MessageProperties()
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON)
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.displayName())
        messageProperties.setContentLength(bytes.length)
        return new Message(bytes, messageProperties)
    }

    protected static void logReceivedMessage(String queueName, Message received) {
        if (received != null) {
            Map<String, Object> headers = received.getMessageProperties().getHeaders()
            String messageString = new String(received.getBody(), StandardCharsets.UTF_8)
            if (!headers.get("content_encoding").equals("UTF-8")) {
                LOGGER.error("message with wrong content_encoding {} in queue {}: {}", headers.get("content_encoding"), queueName, messageString)
            }
            if (!headers.get("content_type").equals("application/json")) {
                LOGGER.error("message with wrong content_type {} in queue {}: {}", headers.get("content_type"), queueName, messageString)
            }
            LOGGER.info("verify {}: {}", queueName, messageString)
        }
    }

    protected void verifyMessageBufferContainsExpectedMessage(String queueName, String expectedJson, JSONCompareMode compareMode) {
        boolean passed = false
        List<JSONCompareResult> failures = new ArrayList<>()

        for (Message message : buffer.getMessages(queueName).findAll { owner.isJSONMessage(it) }) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8)
            JSONCompareResult result
            try {
                result = JSONCompare.compareJSON(expectedJson, body, compareMode)
            } catch (JSONException e) {
                throw new IllegalArgumentException("malformed json", e)
            }
            if (result.passed()) {
                passed = true
                buffer.remove(message)
            } else {
                failures.add(result)
            }
        }

        if (!passed) {
            throw new AssertionError(failureMessage(failures))
        }
    }


    protected static boolean isJSONMessage(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders()
        if (headers.get("content_encoding") != null && !headers.get("content_encoding").equals("UTF-8")) {
            return false
        }
        if (headers.get("content_type") != null && !headers.get("content_type").equals("application/json")) {
            return false
        }
        return true
    }

    protected static String failureMessage(List<JSONCompareResult> failures) {
        if (failures.isEmpty()) {
            return "no messages received but expected at least one"
        }
        if (failures.size() == 1) {
            return failures.get(0).getMessage()
        }
        StringBuilder failMessage = new StringBuilder("received " + failures.size() + " messages but none did match\n")
        for (int i = 0; i < failures.size(); i++) {
            failMessage
                    .append("message ").append(i + 1).append(" ")
                    .append(failures.get(i).getMessage())
                    .append("\n")
        }
        return failMessage.toString()
    }

    private static Message objectToMessage(def object) {
        if (!(object instanceof String)) {
            object = new JsonBuilder(object).toString()
        }
        jsonMessage object
    }


    @Override
    void declareExchange(Exchange exchange) {
        admin.declareExchange(exchange)
    }


    @Override
    boolean deleteExchange(String exchangeName) {
        return admin.deleteExchange(exchangeName)
    }


    @Override
    String declareQueue(Queue queue) {
        return admin.declareQueue(queue)
    }


    @Override
    Queue declareQueue() {
        return admin.declareQueue()
    }


    @Override
    boolean deleteQueue(String queueName) {
        return admin.deleteQueue(queueName)
    }


    @Override
    void deleteQueue(String queueName, boolean unused, boolean empty) {
        admin.deleteQueue(queueName, unused, empty)
    }

    @Override
    void purgeQueue(String queueName, boolean noWait) {
        admin.purgeQueue(queueName, noWait)
        buffer.clear(queueName)
    }

    @Override
    void declareBinding(Binding binding) {
        admin.declareBinding(binding)
    }


    @Override
    void removeBinding(Binding binding) {
        admin.removeBinding(binding)
    }


    @Override
    Properties getQueueProperties(String queueName) {
        return admin.getQueueProperties(queueName)
    }

    @Override
    Message receive(String queueName) {
        Message message = super.receive(queueName)
        if (message != null) {
            message.getMessageProperties().setConsumerQueue(queueName)
            buffer.add(message)
        }
        return message
    }

    void waitUntilConnectionFactoryAvailable() throws IOException {
        Retry.withRetry(20, TimeUnit.SECONDS) { context ->
            Connection connection = createConnection()
            connection.close()
            return null
        }
    }

    protected static class MessageBuffer {
        private final List<Message> messages = new ArrayList<>()

        void add(Message message) {
            messages.add(message)
        }

        List<Message> getMessages(String queueName) {
            return messages
                    .findAll { m -> m.getMessageProperties().getConsumerQueue().equals(queueName) }
        }

        void remove(Message message) {
            messages.remove(message)
        }

        void clear(String queueName) {
            getMessages(queueName).each { this.remove(it) }
        }

        void clear() {
            messages.clear()
        }
    }

}
