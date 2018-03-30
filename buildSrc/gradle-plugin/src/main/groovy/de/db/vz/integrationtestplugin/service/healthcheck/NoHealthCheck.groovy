package de.db.vz.integrationtestplugin.service.healthcheck

class NoHealthCheck extends HealthCheck {
    protected NoHealthCheck(String service, String containerId, String network, def env) {
        super(service, containerId, network, env)
    }

    @Override
    protected Status performHealthCheck() {
        status = Status.UNSUPPORTED
    }

    @Override
    boolean isOk() {
        true
    }

    @Override
    boolean hasFailed() {
        false
    }

    @Override
    boolean isPending() {
        false
    }
}
