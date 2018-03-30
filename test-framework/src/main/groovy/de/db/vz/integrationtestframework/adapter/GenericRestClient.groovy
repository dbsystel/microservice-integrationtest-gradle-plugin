package de.db.vz.integrationtestframework.adapter

import de.db.vz.integrationtestframework.config.ServiceUriResolver
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient


// TODO! RabbitController, RestClient & Polling Conditions should not be part of plugin
abstract class GenericRestClient extends RESTClient {

    protected String host
    protected String port
    protected String basePath
    protected String baseUri

    private JsonSlurper jsonSlurper = []

    GenericRestClient(String service, String defaultBasePath = null) {
        this.host = ServiceUriResolver.instance().resolveForService(service).host
        this.port = ServiceUriResolver.instance().resolveForService(service).port
        this.basePath = defaultBasePath ?: service
        this.baseUri = "http://${host}:$port/"
        setUri(baseUri)
    }

    protected HttpResponseDecorator get(String path = '', Map params = null, String contentType = null) {
        get(path: path, query: params, contentType: contentType) as HttpResponseDecorator
    }

    protected def getByPath(String path) {
        get(path).data
    }

    protected def getJsonByPath(String path) {
        get(path, null, 'application/json').data
    }

    def getInfo() {
        getJsonByPath('info')
    }

    def getMetrics() {
        getJsonByPath('metrics')
    }

    private def conditionalJsonParsing(def data) {
        data instanceof ByteArrayInputStream ? jsonSlurper.parse(data) : data
    }

    protected def getByPathOnDefaultBasePath(String path) {
        getByPath("$basePath/$path")
    }
}
