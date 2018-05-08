package de.db.vz.integrationtestplugin.tasks

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerCompose
import de.db.vz.integrationtestplugin.service.ServiceAvailability
import org.gradle.api.DefaultTask

class IntegrationTestRefreshTask extends DefaultTask {

    IntegrationTestRefreshTask() {
        doFirst {
            def dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile, IntegrationTestPlugin.composeProjectFile.text)

            def serviceAvailability = new ServiceAvailability(dockerCompose, project)

            project.logger.lifecycle "refreshing integration test environment: ${IntegrationTestPlugin.composeFile}"
            dockerCompose.refresh()
            serviceAvailability.waitUntilAllServicesUp(IntegrationTestPlugin.integrationTestExtension.startUpTimeoutInSeconds)
        }
    }
}
