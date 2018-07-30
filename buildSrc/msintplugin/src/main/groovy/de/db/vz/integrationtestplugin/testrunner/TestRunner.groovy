package de.db.vz.integrationtestplugin.testrunner

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.concurrent.TimeUnit

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
            callDocker(true, 'start', '-a', containerId)
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

    private StringBuffer callDocker(boolean ignoreExitCode = false, String... cmd) {
        def builder = new ProcessBuilder(['docker'] + Arrays.asList(cmd))

        logger.debug("calling docker with command: ${builder.command()}")

        Process process = builder.start()
        def out = new StringBuffer()
        def err = new StringBuffer()
        process.consumeProcessOutput(out, err)
        process.waitFor(IntegrationTestPlugin.integrationTestExtension.dockerCommandTimeoutInSeconds, TimeUnit.SECONDS)
        def exitCode = process.exitValue()

        if (!ignoreExitCode && exitCode != 0) {
            logger.info out.readLines().join('\n')
            logger.error err.readLines().join('\n')
            throw new DockerException("test runner terminated with exitCode $exitCode")
        }

        return out
    }
}
