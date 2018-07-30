package de.db.vz.integrationtestplugin

class IntegrationTestExtension {
    static final DEFAULT_STARTUP_TIMEOUT = 180
    static final DEFAULT_DOCKER_COMMAND_TIMEOUT = 600

    String testRunnerImage
    List<String> testRunnerCommand
    int startUpTimeoutInSeconds = DEFAULT_STARTUP_TIMEOUT
    int dockerCommandTimeoutInSeconds = DEFAULT_DOCKER_COMMAND_TIMEOUT

    def useJUnitPlatformConsoleRunner() {
        testRunnerCommand = [
                'java', '-cp', '/classpath/*:/classes', 'org.junit.platform.console.ConsoleLauncher',
                '--classpath', '/tests.jar', '--scan-classpath', '--include-classname', '^.*Test.*$', '--include-classname', '^.*Spec.*$',
                '--reports-dir', '/reports']
    }
}
