package de.db.vz.integrationtestplugin.docker

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Docker {

    private Logger logger = LoggerFactory.getLogger(Docker.class)

    private static final String DOCKER_HOST_PROPERTY = "docker.host"

    private String id

    Docker(String id) {
        this.id = id
    }

    Docker() {
    }

    String run(String image) {
        this.id = stdout(docker('run', '-d', image))
        this.id
    }

    boolean stop() {
        if (!id) {
            logger.error "No running container registered (missing id)!"
            return false
        }
        return stdout(docker('stop', id)) == id
    }

    boolean rm(boolean force = true, boolean volumes = true) {
        if (!id) {
            logger.error "No running container registered (missing id)!"
            return false
        }
        String[] properties = []
        if (force) {
            properties += ['-f']
        }
        if (volumes) {
            properties += ['-v']
        }
        return stdout(docker('rm', *properties, id)) == id
    }

    String inspect(String format) {
        String[] parameters = ['inspect', id]
        if (format) {
            parameters += ["--format=$format".toString()]
        }
        stdout(docker(parameters))
    }

    private static String stdout(Process process) {
        process.in.text.trim()
    }

    private Process docker(String[] parameters) {
        String[] command = ['docker']

        if (System.properties.DOCKER_HOST_PROPERTY) {
            command += ['-H', System.properties.DOCKER_HOST_PROPERTY]
        }

        command += parameters
        def builder = new ProcessBuilder(command)

        logger.debug builder.command().join(' ')

        Process process = builder.start()
        process.waitFor()
        process
    }
}
