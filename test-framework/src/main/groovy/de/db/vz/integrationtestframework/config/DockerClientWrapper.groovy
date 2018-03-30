package de.db.vz.integrationtestframework.config

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Container
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory


// TODO use docker remote api
// TODO move to plugin, which adds it to integrationTest configuration for the project it is applied on
class DockerClientWrapper {

    private Logger LOGGER = LoggerFactory.getLogger(DockerClientWrapper.class)

    private static final String DOCKER_HOST_PROPERTY = "docker.host"

    private String composeProjectFile

    private String composeProject = null
    private DockerClient dockerClient

    private String resolveComposeProjectFile() {
        getClass().getClassLoader().getResource(".compose-project")?.path
    }

    URI resolveServiceOnDockerHost(String service, int port, String scheme) {
        composeProjectFile = resolveComposeProjectFile()
        String dockerHost = resolveDockerHost()
        String containerId = resolveContainerId(service)
        int containerPort = resolveContainerPort(containerId, port)

        return createURI(dockerHost, containerPort, scheme)
    }

    static boolean inDocker() {
        return new File("/.dockerenv").exists()
    }


    private String resolveComposeProject() {
        if (composeProject != null) {
            return composeProject
        } else {
            try {
                composeProject = inDocker() ? "" : composeProjectOnNode()
                return composeProject
            } catch (IOException e) {
                LOGGER.warn("Cannot resolve docker compose project in $composeProjectFile")
                composeProject = ""
                return composeProject
            }
        }
    }

    private String composeProjectOnNode() {
        File composeProjectFile = new File(composeProjectFile)
        composeProjectFile.text
    }

    private DockerClient getEnvironmentSpecificDockerClient() {
        if (dockerClient == null) {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
            if (System.getProperty(DOCKER_HOST_PROPERTY) != null && !System.getProperty(DOCKER_HOST_PROPERTY).contains("localhost")) {
                configBuilder = configBuilder.withDockerHost(fixProtocol(System.getProperty(DOCKER_HOST_PROPERTY)))
            }
            dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build()
        }

        return dockerClient
    }

    private int resolveContainerPort(String containerId, int port) {
        DockerClient dockerClient = getEnvironmentSpecificDockerClient()
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec()

        // TODO: should we: .filter(binding -> "0.0.0.0".equals(binding.getHostIp()))
        // TODO: handle port not being mapped: eg 8080->null
        Integer binding = (response.networkSettings.ports.bindings
                .findAll { it.key.port == port }
                .collect { it.value } - null)
                .collect { it.first().hostPortSpec }
                .first()
                .toInteger()
        if (!binding) {
            throw new RuntimeException("No uniquely matching port [$port] found for container: $containerId")
        }
        binding
    }

    private static String extractHostName(String dockerHost) {
        return dockerHost.replaceAll("(.+://)|(:\\d+)", "")
    }

    private String resolveContainerId(String serviceName) {
        DockerClient dockerClient = getEnvironmentSpecificDockerClient()

        List<Container> containers = dockerClient.listContainersCmd().exec()
        Set<String> ids = containers
                .findAll { c -> Objects.equals(c.getLabels().get("com.docker.compose.service"), serviceName) }
                .findAll { this.isSameProject(it) }
                .collect { it.id }

        if (ids.isEmpty()) {
            throw new RuntimeException("No container match found for Service $serviceName and docker compose project ${resolveComposeProject()}")
        } else if (ids.size() > 1) {
            throw new RuntimeException("More than one instance of service $serviceName running, found instances: $ids")
        }

        return ids.iterator().next()
    }

    private static URI createURI(String host, int port, String scheme) {
        try {
            return new URI(scheme, null, host, port, null, null, null)
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Host [$host] or port [$port] invalid", e)
        }
    }

    private static String fixProtocol(String dockerHost) {
        if (dockerHost == null || dockerHost.isEmpty()) {
            throw new RuntimeException(DOCKER_HOST_PROPERTY + " property is empty!")
        }
        return dockerHost.contains("://") ?
                dockerHost.replaceFirst("^.*//", "tcp://") :
                "tcp://" + dockerHost
    }

    private static String resolveDockerHost() {
        String dockerHost = System.getProperty(DOCKER_HOST_PROPERTY, System.getenv("DOCKER_HOST"))

        if (dockerHost == null || dockerHost.isEmpty()) {
            dockerHost = "localhost"
        }
        return extractHostName(dockerHost)
    }

    private boolean isSameProject(Container container) {
        if (resolveComposeProject().equals("")) {
            LOGGER.warn("Cannot evaluate compose project -> ignoring compose project filter!")
            return true
        }
        return Objects.equals(container.getLabels().get("com.docker.compose.project"), resolveComposeProject())
    }
}
