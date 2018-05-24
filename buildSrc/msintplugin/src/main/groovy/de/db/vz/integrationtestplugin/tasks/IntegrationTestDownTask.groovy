package de.db.vz.integrationtestplugin.tasks

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerCompose
import de.db.vz.integrationtestplugin.parse.JunitXmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel

class IntegrationTestDownTask extends DefaultTask {

    IntegrationTestDownTask() {
        doFirst {
            if (!IntegrationTestPlugin.composeProjectFile.exists()) {
                project.logger.log LogLevel.WARN, "no compose project file found at ${IntegrationTestPlugin.composeProjectFile.absolutePath}"
                return
            }

            def dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile,
                    IntegrationTestPlugin.composeProjectFile.text)
            project.logger.lifecycle "loaded project: ${dockerCompose.composeProject}"

            def dockerLogsDir = new File(project.buildDir, 'docker-logs')
            dockerLogsDir.mkdirs()

            project.logger.lifecycle 'stopping containers'
            dockerCompose.stop()
            project.logger.lifecycle 'archiving log files'
            dockerCompose.archiveLogs(dockerLogsDir)
            project.logger.lifecycle 'stopping integration test environment'
            project.logger.lifecycle 'removing service containers and network'
            dockerCompose.down()
            IntegrationTestPlugin.composeProjectFile.delete()

            def testrunnerLog = new File(dockerLogsDir, 'testrunner.log')
            if (testrunnerLog.exists()) {
                project.logger.info testrunnerLog.text
            }
            String htmlreport = "${project.buildDir.absolutePath}/test-results/index.html"
            if (!isSuccessfull(new File(project.buildDir, 'test-results/integrationTest'))) {
                // TODO generate html report
                throw new GradleException("There are failed test cases! See ${htmlreport} for details")
            } else {
                project.logger.lifecycle "See test result in $htmlreport"
            }
        }
    }

    static boolean isSuccessfull(File testResultDir) {
        if (!testResultDir.exists())
            return true

        JunitXmlParser parser = []

        parser.isSuccessful(testResultDir)
    }
}