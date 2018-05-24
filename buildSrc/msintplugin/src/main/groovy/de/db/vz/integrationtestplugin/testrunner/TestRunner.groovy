package de.db.vz.integrationtestplugin.testrunner

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class TestRunner {

    private final File testsArchive
    private final File runnerDir
    private final File resultDir
    private final File logsDir
    private final String network
    private String containerId
    static Logger logger = Logging.getLogger(TestRunner.class)

    TestRunner(File testsArchive, File runnerDir, File resultDir, File logsDir, String network) {
        this.testsArchive = testsArchive
        this.runnerDir = runnerDir
        this.resultDir = resultDir
        this.logsDir = logsDir
        this.network = network
    }

    def execute() {
        def dockerCreate = ['create', "--net=$network", IntegrationTestPlugin.integrationTestExtension.testRunnerImage] + IntegrationTestPlugin.integrationTestExtension.testRunnerCommand
        containerId = callDocker(dockerCreate as String[]).readLines()[0]

        try {
            for (File file : runnerDir.listFiles()) {
                callDocker('cp', file.absolutePath, containerId + ':/')
            }
            callDocker('cp', testsArchive.absolutePath, containerId + ':/')
            callDocker(true, 'start', '-ai', containerId)
            callDocker('cp', containerId + ':/reports/.', resultDir.absolutePath)
        } finally {
            try {
                archiveLogs()
            } finally {
                logger.lifecycle 'removing testrunner container'
                callDocker('rm', '-fv', containerId)
            }
        }
    }

    void archiveLogs() {
        def builder = new ProcessBuilder('docker', 'logs', containerId)
        builder.redirectOutput(new File(logsDir, 'testrunner.log'))
        Process process = builder.start()
        process.waitFor()
    }

    private InputStream callDocker(boolean ignoreExitCode = false, String... cmd) {
        def builder = new ProcessBuilder(['docker'] + Arrays.asList(cmd))

        Process process = builder.start()
        def exitCode = process.waitFor()

        if (!ignoreExitCode && exitCode != 0) {
            def error = process.getErrorStream().readLines()
            throw new DockerException('error executing test runner: ' + error.join(System.lineSeparator()))
        }

        return process.getInputStream()
    }
}
