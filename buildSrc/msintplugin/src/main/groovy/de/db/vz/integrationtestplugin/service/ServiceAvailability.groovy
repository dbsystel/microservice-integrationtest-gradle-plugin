package de.db.vz.integrationtestplugin.service

import de.db.vz.integrationtestplugin.docker.DockerCompose
import de.db.vz.integrationtestplugin.service.healthcheck.HealthCheck
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class ServiceAvailability {

    private Project project
    private Logger logger
    private DockerCompose dockerCompose
    private long startTime
    private boolean availabilityHasBeenLoggedOnce = false


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

        startTime = System.currentTimeMillis()

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

            StringBuilder strb = new StringBuilder()

            healthChecks.each {
                it.execute()
                strb.append("│ " + getStatusMessage(it)).append("\n")
            }
            strb.append("╰─── ◷ waiting for " + (System.currentTimeMillis() - startTime) / 1000 + "s\n")

            printNonRepeatingLogs(strb.toString())

            someHealthChecksFailed = haveSomeHealthChecksFailed(healthChecks)
            allHealthChecksOk = areAllHealthChecksOk(healthChecks)

            Thread.sleep(1000)
        }
        allHealthChecksOk
    }

    private void printNonRepeatingLogs(String statusLog) {
        if (!availabilityHasBeenLoggedOnce) {
            logger.lifecycle statusLog
            availabilityHasBeenLoggedOnce = true
        } else {
            int statusLogLines = countLinesInString(statusLog) + 1
            println '\r\033[' + statusLogLines + 'A\033[0K' + statusLog
        }

    }

    private int countLinesInString(String statusLog) {
        def statusLogLines = statusLog.split(System.getProperty("line.separator"))
        if (statusLogLines.last().empty) {
            return statusLogLines.length + 1
        } else {
            return statusLogLines.length
        }
    }

    private static String getStatusMessage(HealthCheck healthCheck) {
        String statusMessage = "${healthCheck.service()}...".padRight(50, '.')

        if (healthCheck.serviceVersion() == null) {
            return statusMessage + healthCheck.status()
        } else {
            return statusMessage + healthCheck.status().padRight(15, '.') + healthCheck.serviceVersion()
        }
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
