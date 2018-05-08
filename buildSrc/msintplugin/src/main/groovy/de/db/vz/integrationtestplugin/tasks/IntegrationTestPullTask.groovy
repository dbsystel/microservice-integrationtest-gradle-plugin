package de.db.vz.integrationtestplugin.tasks

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerCompose
import org.gradle.api.DefaultTask

class IntegrationTestPullTask extends DefaultTask {

    IntegrationTestPullTask() {
        doFirst {
            def dockerCompose
            if (IntegrationTestPlugin.composeProjectFile.exists()) {
                dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile, IntegrationTestPlugin.composeProjectFile.text)
            } else {
                dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile)
            }

            project.logger.lifecycle "pulling images in ${IntegrationTestPlugin.composeFile}"
            dockerCompose.pull()
        }
    }
}
