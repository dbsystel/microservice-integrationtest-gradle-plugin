package de.db.vz.integrationtestplugin.tasks

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerCompose
import org.gradle.api.DefaultTask

class IntegrationTestLogTask extends DefaultTask {

    IntegrationTestLogTask() {
        doFirst {
            def dockerCompose = new DockerCompose(IntegrationTestPlugin.composeFile, IntegrationTestPlugin.composeProjectFile.text)

            dockerCompose.followLogs()
        }
    }
}
