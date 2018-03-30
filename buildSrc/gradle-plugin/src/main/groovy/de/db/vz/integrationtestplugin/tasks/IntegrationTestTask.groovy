package de.db.vz.integrationtestplugin.tasks

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerCompose
import de.db.vz.integrationtestplugin.testrunner.TestRunner
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Jar

class IntegrationTestTask extends DefaultTask {

    @Internal
    Jar jar

    IntegrationTestTask() {
        doFirst {
            def dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile,
                    IntegrationTestPlugin.composeProjectFile.readLines()[0])
            project.logger.lifecycle "loaded project: ${dockerCompose.composeProject}"

            def dockerLogsDir = new File(project.buildDir, 'docker-logs')
            dockerLogsDir.mkdir()

            def libsDir = new File("${project.buildDir}/classpath/")
            libsDir.mkdir()

            project.logger.lifecycle 'executing test cases'
            project.logger.lifecycle '...'
            TestRunner testRunner = new TestRunner(
                    jar.archivePath, libsDir,
                    new File(project.buildDir, 'test-results'),
                    dockerLogsDir,
                    dockerCompose.network())
            copyDependencies(libsDir)
            testRunner.execute()
            project.logger.lifecycle 'finished executing test cases'
        }
    }

    def copyDependencies(File target) {
        project.copy {
            from(project.configurations.integrationTestRuntime) {
                include '**/*.jar'
            }
            into(target.absolutePath)
        }
        project.copy {
            from(project.configurations.integrationTestRuntime) {
                exclude '**/*.jar'
            }
            into(new File(target, 'classes').absolutePath)
        }
    }
}
