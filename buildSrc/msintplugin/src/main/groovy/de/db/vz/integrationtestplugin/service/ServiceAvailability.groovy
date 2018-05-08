package de.db.vz.integrationtestplugin.service

import de.db.vz.integrationtestplugin.docker.DockerCompose
import de.db.vz.integrationtestplugin.service.healthcheck.HealthCheck
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class ServiceAvailability {

    private Project project
    private Logger logger
    private DockerCompose dockerCompose


    ServiceAvailability(DockerCompose dockerCompose, Project project) {
        this.project = project
        this.logger = project.logger
        this.dockerCompose = dockerCompose
    }


    void waitUntilAllServicesUp(Integer startupTimeout) {
        logger.lifecycle "waiting ${startupTimeout}s until all services are up and healthy..."

        def services = dockerCompose.services()
        def healthChecks = services.collect {
            HealthCheck.from(it, dockerCompose.containerId(it), dockerCompose.network())
        }

        def startTime = System.currentTimeMillis()

        boolean allHealthChecksOk = waitForHealthChecks(startTime, startupTimeout, healthChecks)

        healthChecks.each {
            if (it.isPending() || it.hasFailed()) {
                logger.lifecycle "health check failed on service ${it.service()}"
            }
        }

        if (!allHealthChecksOk) {
            def dockerLogsDir = new File(project.buildDir, 'docker-logs')
            dockerLogsDir.mkdirs()
            logger.lifecycle 'stopping containers'
            dockerCompose.stop()
            logger.lifecycle 'archiving log files'
            dockerCompose.archiveLogs(dockerLogsDir)
            logger.lifecycle 'stopping integration test environment'
            logger.lifecycle 'removing service containers and network'
            dockerCompose.down()

            throw new IllegalStateException("failed to start all services within ${startupTimeout}s")
        }
        logger.lifecycle "all services started up after ${(System.currentTimeMillis() - startTime) / 1000}s"
    }

    private boolean waitForHealthChecks(long startTime, int startupTimeoutInSeconds, List<HealthCheck> healthChecks) {
        boolean someHealthChecksFailed = false
        boolean allHealthChecksOk = false

        while (!allHealthChecksOk &&
                !someHealthChecksFailed &&
                !timeoutIsExceeded(startTime, startupTimeoutInSeconds * 1000)) {

            healthChecks.each {
                it.execute()
                logger.lifecycle "${it.service()}...".padRight(50, '.') + it.status()
            }
            logger.lifecycle('...')

            someHealthChecksFailed = haveSomeHealthChecksFailed(healthChecks)
            allHealthChecksOk = areAllHealthChecksOk(healthChecks)

            Thread.sleep(1000)
        }
        allHealthChecksOk
    }

    private static boolean timeoutIsExceeded(def startTime, def timeout) {
        System.currentTimeMillis() > startTime + timeout.toLong()
    }

    private static boolean areAllHealthChecksOk(List<HealthCheck> healthChecks) {
        healthChecks.every {
            it.isOk()
        }
    }

    private static boolean haveSomeHealthChecksFailed(List<HealthCheck> healthChecks) {
        healthChecks.find {
            it.hasFailed()
        }
    }
}
