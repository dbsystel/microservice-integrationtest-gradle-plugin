package de.db.vz.integrationtestplugin.service.healthcheck

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
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
        if (!IntegrationTestPlugin.integrationTestExtension.curlImage) {
            logger.error "Configure a non empty curl image!"
            return fail()
        } else if (!validHealthEndpoint(healthEndpoint)) {
            logger.error "Malformed healthEndpoint: $healthEndpoint should match :<port></path (optional)>, eg: :8080/health"
            return fail()
        } else if (hasContainerExited()) {
            logger.error "Container $containerId exited unexpectedly"
            return fail()
        }

        def builder = new ProcessBuilder('docker', 'run', '--rm', "--net=$network",
                IntegrationTestPlugin.integrationTestExtension.curlImage,
                '-sw','\'%{http_code}\n\'', '-o', '/dev/null',
                "http://${service}${healthEndpoint}")
        logger.debug("health check docker command: ${builder.command()}")
        Process process = builder.start()
        process.waitFor()

        def healthStatus = process.inputStream.readLines()[0]

        // it could be argued that PENDING is not specific enough.
        // in the future we might consider evaluating a more elaborate state at this point.
        status = httpResponseOk(healthStatus) ? Status.OK : Status.PENDING
    }

    private Status fail() {
        status = Status.FAILED
        return status
    }

    static boolean validHealthEndpoint(String healthEndpoint) {
        healthEndpoint ==~ /:\d{2,5}(\/.*)?/
    }

    private static boolean httpResponseOk(String healthStatus) {
        healthStatus ==~ /[23]\d\d/
    }
}
