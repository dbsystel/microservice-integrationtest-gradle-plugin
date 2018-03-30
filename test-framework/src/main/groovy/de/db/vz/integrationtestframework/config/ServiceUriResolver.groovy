package de.db.vz.integrationtestframework.config

class ServiceUriResolver {

    private final static int HTTP_PORT = 8080
    private DockerClientWrapper dockerClient
    private final HashMap<Integer, URI> cache = new HashMap<Integer, URI>()

    private static ServiceUriResolver instance = null

    private ServiceUriResolver() {
        dockerClient = new DockerClientWrapper()
    }

    static ServiceUriResolver instance() {
        if (instance == null) {
            instance = new ServiceUriResolver()
        }
        return instance
    }

    URI resolveForService(String service) {
        return resolveForServiceAndPort(service, HTTP_PORT)
    }

    URI resolveForServiceAndPort(String service, int port) {
        return resolveForServiceAndPort(service, port, "http")
    }

    URI resolveForServiceAndPort(String service, int port, String scheme) {
        if (service == null || service.isEmpty()) {
            throw new IllegalArgumentException("Service must not be empty")
        }

        int hash = Objects.hash(service, port)

        if (!cache.containsKey(hash)) {
            if (DockerClientWrapper.inDocker()) {
                cache.put(hash, resolveServiceInDockerNetwork(service, port, scheme))
            } else {
                cache.put(hash, dockerClient.resolveServiceOnDockerHost(service, port, scheme))
            }
        }

        return cache.get(hash)
    }

    private static URI resolveServiceInDockerNetwork(String service, int port, String scheme) {
        try {
            return new URI(scheme, null, service, port, null, null, null)
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Host [" + service + "] or port [" + port + "] invalid", e)
        }
    }
}