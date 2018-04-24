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

            def runnerDir = new File("${project.buildDir}/test-runner/")
            runnerDir.mkdir()



            project.logger.lifecycle 'executing test cases'
            project.logger.lifecycle '...'
            TestRunner testRunner = new TestRunner(
                    jar.archivePath, runnerDir,
                    new File(project.buildDir, 'test-results/integrationTest'),
                    dockerLogsDir,
                    dockerCompose.network())
            copyFiles(runnerDir)
            testRunner.execute()
            project.logger.lifecycle 'finished executing test cases'
        }
    }

    def copyFiles(File target) {
        def classpathDir = new File(target, "classpath");
        classpathDir.mkdir()
        copyRunnerLibs(classpathDir)
        copyLibs(classpathDir)

        def classesDir = new File(target, "classes");
        classesDir.mkdir()
        copyClasses(classesDir)
    }

    def copyRunnerLibs(File target) {
        project.copy {
            from(project.configurations.integrationTestRunner) {
                include '**/*.jar'
            }
            into(target.absolutePath)
        }
    }

    def copyLibs(File target) {
        project.copy {
            from(project.configurations.integrationTestRuntime) {
                include '**/*.jar'
            }
            into(target.absolutePath)
        }
    }

    def copyClasses(File target) {
        project.copy {
            from(project.configurations.integrationTestRuntime) {
                exclude '**/*.jar'
            }
            into(target.absolutePath)
        }
    }
}
