package de.db.vz.integrationtestplugin.service.healthcheck

class ExitCodeHealthCheck extends HealthCheck {

    protected ExitCodeHealthCheck(String service, String containerId, String network, def env) {
        super(service, containerId, network, env)
    }

    @Override
    protected Status performHealthCheck() {
        if (hasContainerExited()) {
            status = hasContainerExitedSuccessfully() ? Status.OK : Status.FAILED
        } else {
            status = Status.PENDING
        }
        status
    }
}
