package de.db.vz.integrationtestplugin.tasks

import org.gradle.api.DefaultTask
// TODO! is there a gradle way for help / usage of plugins?
class IntegrationTestHelpTask extends DefaultTask {

    IntegrationTestHelpTask() {
        doFirst {
            logger.lifecycle(indent('Usage:'))
            logger.lifecycle(indent('integrationTest', 'run integrationTestUp, run tests, run integrationTestDown'))
            logger.lifecycle(indent('integrationTestUp', 'start up & wait for services'))
            logger.lifecycle(indent('integrationTestDown', 'shut down & remove containers'))
            logger.lifecycle(indent('integrationTestPull', 'pull images as they are defined in the compose file'))
            logger.lifecycle(indent('integrationTestBuild', 'rebuild images as they are defined in the compose file'))
            logger.lifecycle(indent('integrationTestRefresh', 'refresh containers with changed images'))
            logger.lifecycle(indent('integrationTestLog', 'follow real time compose logs'))
        }
    }

    static String indent(String... blocks) {
        blocks.collect { it.padRight(it.size() > 3 ? 25 : 6) }.join('')
    }
}