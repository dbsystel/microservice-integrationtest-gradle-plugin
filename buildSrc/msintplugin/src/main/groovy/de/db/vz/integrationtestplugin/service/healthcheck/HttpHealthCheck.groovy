package de.db.vz.integrationtestplugin.service.healthcheck

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class HttpHealthCheck extends HealthCheck {

    protected final String healthEndpoint
    static Logger logger = Logging.getLogger(HttpHealthCheck.class)


    protected HttpHealthCheck(String service, String containerId, String network, String healthEndpoint, def env) {
        super(service, containerId, network, env)
        this.healthEndpoint = healthEndpoint
    }

    @Override
    protected Status performHealthCheck() {
        if (!validHealthEndpoint(healthEndpoint)) {
            logger.error "Malformed healthEndpoint: $healthEndpoint should match :<port></path (optional)>, eg: :8080/health"
            status = Status.FAILED
            return status
        } else if (hasContainerExited()) {
            logger.error "Container $containerId exited unexpectedly"
            status = Status.FAILED
            return status
        }

        def builder = new ProcessBuilder('docker', 'run', '--rm', "--net=$network",
                'appropriate/curl',
                '-s', '-o', '/dev/null', '-w', '%{http_code}',
                "http://${service}${healthEndpoint}")
        logger.debug("health check docker command: ${builder.command()}")
        Process process = builder.start()
        process.waitFor()

        def healthStatus = process.inputStream.readLines()[0]

        // it could be argued that PENDING is not specific enough.
        // in the future we might consider evaluating a more elaborate state at this point.
        status = httpResponseOk(healthStatus) ? Status.OK : Status.PENDING
    }

    static boolean validHealthEndpoint(String healthEndpoint) {
        healthEndpoint ==~ /:\d{2,5}(\/.*)?/
    }

    private static boolean httpResponseOk(String healthStatus) {
        healthStatus ==~ /[23]\d\d/
    }
}
