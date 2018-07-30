package de.db.vz.integrationtestplugin.docker

import de.db.vz.integrationtestplugin.IntegrationTestPlugin
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class DockerCompose {

    String composeProject
    File composeFile
    private static Logger logger = Logging.getLogger(DockerCompose.class)


    DockerCompose(File composeFile, String composeProject = randomString(8)) {
        this.composeFile = composeFile
        this.composeProject = composeProject
    }

    void up() {
        logger.lifecycle "setting up docker project: $composeProject"
        callDockerCompose('up', '-d')
    }

    void refresh() {
        logger.lifecycle "refreshing docker project: $composeProject"
        callDockerCompose('up', '-d')
    }

    void build() {
        logger.lifecycle "building docker project: $composeProject"
        callDockerCompose('build')
    }

    void down() {
        callDockerCompose('down', '-v', '--remove-orphans')
    }

    void start() {
        callDockerCompose('start')
    }

    void stop() {
        callDockerCompose('stop')
    }

    void followLogs() {
        callDockerComposeLive('logs', '--tail', '500', '-f', '-t')
    }

    void pull() {
        callDockerComposeLive('pull')
    }

    def services() {
        callDockerCompose('config', '--services')
    }

    String network() {
        composeProject + '_default'
    }

    def containerId(String service) {
        callDockerCompose('ps', '-q', service)[0]
    }

    def archiveLogs(File dir) {
        services().each {
            // for some reason 'docker-compose logs' is very slow for large log files
            // so we use docker logs instead
            def containerId = containerId(it)
            def builder = new ProcessBuilder('docker', 'logs', containerId)
            logger.debug("running docker command: ${builder.command()}")
            builder.redirectOutput(new File(dir, "${it}.log"))
            Process process = builder.start()
            process.waitFor()
        }
    }

    private def callDockerCompose(String... cmd) {
        Process process = createDockerComposeProcess(cmd)
        process.waitFor(IntegrationTestPlugin.integrationTestExtension.dockerCommandTimeoutInSeconds, TimeUnit.SECONDS)
        def exitCode = process.exitValue()

        def error = process.err.readLines()
        if (exitCode != 0) {
            throw new DockerException('error calling docker-compose: ' + error)
        }
        // apparently docker compose logs some regular output to stderr
        logger.info error.join(System.lineSeparator())

        def out = process.in.readLines()
        logger.info out.join(System.lineSeparator())
        out
    }

    private def callDockerComposeLive(String... cmd) {
        createDockerComposeProcess(cmd).waitForProcessOutput(System.out, System.err)
    }

    private Process createDockerComposeProcess(String... cmd) {
        List baseCmd = ['docker-compose',
                        '-p', composeProject,
                        '-f', composeFile.absolutePath]

        def builder = new ProcessBuilder(baseCmd + cmd.toList())
        builder.directory(composeFile.parentFile.absoluteFile)
        builder.redirectInput()
        logger.debug("running docker compose command: ${builder.command()}")
        Process process = builder.start()
        process
    }

    private static def randomString(int length) {
        def chars = ('a'..'z').join('')
        def random = new SecureRandom()

        def sb = new StringBuilder(length)
        (0..length).each {
            sb.append(chars.charAt(random.nextInt(chars.length())))
        }
        return sb.toString()
    }
}
