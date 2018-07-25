package de.db.vz.integrationtestplugin.service.healthcheck

import de.db.vz.integrationtestplugin.docker.Docker
import de.db.vz.integrationtestplugin.docker.DockerException
import groovy.json.JsonSlurper

abstract class HealthCheck {

    protected final String service
    protected final String containerId
    protected final String network
    protected final def env
    protected Status status
    protected String error
    protected final String serviceVersion

    // TODO implicitly go through health check precedence instead of relying on configuration

    // TODO support docker health checks
    static HealthCheck from(String service, String containerId, String network) {
        Map<String, String> env = env(containerId, service)

        // TODO support SERVICE_CHECK_TCP
        if (env.SERVICE_CHECK_HTTP != null) {
            new HttpHealthCheck(service, containerId, network, env.SERVICE_CHECK_HTTP, env)
        } else if (env.SERVICE_CHECK_EXIT) {
            new ExitCodeHealthCheck(service, containerId, network, env)
        } else {
            new NoHealthCheck(service, containerId, network, env)
        }
    }

    protected HealthCheck(String service, String containerId, String network, def env) {
        this.service = service
        this.containerId = containerId
        this.network = network
        this.env = env
        this.serviceVersion = resolveServiceVersion()
    }

    String resolveServiceVersion() {
        resolveFromLabel() ?: env.VERSION
    }

    String resolveFromLabel() {
        def docker = new Docker(this.containerId)
        ['version', 'VERSION', 'Version'].findResult {
            docker.inspect("{{ index .Config.Labels \"$it\" }}") ?: null
        }
    }

    Status execute() {
        if (inFinalState()) {
            return status
        }
        performHealthCheck()
    }

    protected abstract Status performHealthCheck()

    boolean isOk() {
        status == Status.OK
    }

    boolean isPending() {
        status == Status.PENDING
    }

    boolean hasFailed() {
        status == Status.FAILED
    }


    boolean hasContainerExited() {
        resolveContainerStatus() == 'exited'
    }

    boolean hasContainerExitedSuccessfully() {
        hasContainerExited() &&
                resolveContainerStateExitCode() == '0'
    }

    def service() {
        service
    }

    String status() {
        status.toString()
    }

    String serviceVersion() {
        serviceVersion
    }

    protected boolean inFinalState() {
        status in [Status.OK, Status.UNSUPPORTED, Status.FAILED]
    }

    protected def resolveContainerStateExitCode() {
        trimQuotes inspectContainer('"{{ .State.ExitCode }}"')[0]
    }

    protected def resolveContainerStatus() {
        trimQuotes inspectContainer('"{{ .State.Status }}"')[0]
    }

    protected Map env() {
        env(containerId, service)
    }

    static protected Map env(String containerId, String service) {
        String output = inspectContainer('{{ json .Config.Env }}', containerId, service).join(System.lineSeparator())

        def result = new JsonSlurper().parseText(output)

        result.collectEntries {
            it.tokenize('=')
        }
    }

    protected def inspectContainer(String format) {
        inspectContainer(format, containerId, service)
    }

    static protected def inspectContainer(String format, String containerId, String service) {
        def builder = new ProcessBuilder('docker',
                'inspect',
                '--format', format,
                containerId)
        Process process = builder.start()
        def exitCode = process.waitFor()

        if (exitCode != 0) {
            def error = process.getErrorStream().readLines()
            throw new DockerException("error calling docker inspect on service $service: " + error.join(System.lineSeparator()))
        }

        process.getInputStream().readLines()
    }

    protected static def trimQuotes(String s) {
        s.replaceAll('"(.+)"', '$1')
    }

    enum Status {
        OK, FAILED, PENDING, UNSUPPORTED
    }
}
