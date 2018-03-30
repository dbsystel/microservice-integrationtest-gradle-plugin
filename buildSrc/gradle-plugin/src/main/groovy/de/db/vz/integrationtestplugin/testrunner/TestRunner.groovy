package de.db.vz.integrationtestplugin.testrunner

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import de.db.vz.integrationtestplugin.docker.DockerException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class TestRunner {

    private final File testsArchive
    private final File libsDir
    private final File resultDir
    private final File logsDir
    private final String network
    private String containerId
    static Logger logger = Logging.getLogger(TestRunner.class)

    TestRunner(File testsArchive, File libsDir, File resultDir, File logsDir, String network) {
        this.testsArchive = testsArchive
        this.libsDir = libsDir
        this.resultDir = resultDir
        this.logsDir = logsDir
        this.network = network
    }

    def execute() {
        containerId = callDocker('create', "--net=$network", IntegrationTestPlugin.integrationTestExtension.testRunnerImage).readLines()[0]

        try {
            callDocker('cp', testsArchive.absolutePath, containerId + ':/')
            callDocker('cp', libsDir.absolutePath, "$containerId:/".toString())
            callDocker(true, 'start', '-ai', containerId)
            callDocker('cp', containerId + ':/reports/.', resultDir.absolutePath)
            archiveLogs()
        } finally {
            logger.lifecycle 'removing testrunner container'
            callDocker('rm', '-fv', containerId)
        }
    }

    void archiveLogs() {
        def builder = new ProcessBuilder('docker', 'logs', containerId)
        builder.redirectOutput(new File(logsDir, 'testrunner.log'))
        Process process = builder.start()
        process.waitFor()
    }

    private InputStream callDocker(boolean inheritIO = false, String... cmd) {
        def builder = new ProcessBuilder(['docker'] + Arrays.asList(cmd))
        if (inheritIO) {
            builder.inheritIO()
        }
        Process process = builder.start()
        def exitCode = process.waitFor()

        if (exitCode != 0) {
            def error = inheritIO ? '' : process.getErrorStream().readLines()
            throw new DockerException('error executing test runner: ' + error.join(System.lineSeparator()))
        }

        return process.getInputStream()
    }
}
