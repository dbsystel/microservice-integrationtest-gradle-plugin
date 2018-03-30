package de.db.vz.integrationtestplugin.tasks

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerCompose
import de.db.vz.integrationtestplugin.service.ServiceAvailability
import org.gradle.api.DefaultTask

class IntegrationTestUpTask extends DefaultTask {
    IntegrationTestUpTask() {
        doFirst {
            IntegrationTestPlugin.composeProjectFile.parentFile.mkdirs()

            def dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile)
            def serviceAvailability = new ServiceAvailability(dockerCompose, project)

            persistComposeProject(dockerCompose)

            project.logger.lifecycle "starting integration test environment: ${IntegrationTestPlugin.composeFile}"
            dockerCompose.up()

            serviceAvailability.waitUntilAllServicesUp(IntegrationTestPlugin.integrationTestExtension.startUpTimeoutInSeconds)
        }
    }

    static void persistComposeProject(DockerCompose dockerCompose) {
        IntegrationTestPlugin.composeProjectFile.text = dockerCompose.composeProject
    }
}
