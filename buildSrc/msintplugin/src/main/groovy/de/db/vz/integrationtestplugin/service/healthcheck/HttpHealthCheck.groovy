package de.db.vz.integrationtestplugin.service.healthcheck

class HttpHealthCheck extends HealthCheck {

    protected final String healthEndpoint

    protected HttpHealthCheck(String service, String containerId, String network, String healthEndpoint, def env) {
        super(service, containerId, network, env)
        this.healthEndpoint = healthEndpoint
    }

    @Override
    protected Status performHealthCheck() {
        if (hasContainerExited()) {
            status = Status.FAILED
            return status
        }

        def builder = new ProcessBuilder('docker', 'run', '--rm', "--net=$network",
                'appropriate/curl',
                '-s', '-o', '/dev/null', '-w', '%{http_code}',
                "http://${service}${healthEndpoint}")
        Process process = builder.start()
        process.waitFor()

        def healthStatus = process.inputStream.readLines()[0]

        // it could be argued that PENDING is not specific enough.
        // in the future we might consider evaluating a more elaborate state at this point.
        status = httpResponseOk(healthStatus) ? Status.OK : Status.PENDING
    }

    private static boolean httpResponseOk(String healthStatus) {
        healthStatus ==~ /[23]\d\d/
    }
}
