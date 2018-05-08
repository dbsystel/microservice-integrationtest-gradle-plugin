package de.db.vz.integrationtestplugin

class IntegrationTestExtension {
    static final DEFAULT_STARTUP_TIMEOUT = 180

    String testRunnerImage
    int startUpTimeoutInSeconds = DEFAULT_STARTUP_TIMEOUT
}
