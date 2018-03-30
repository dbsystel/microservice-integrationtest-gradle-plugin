package de.db.vz.integrationtestframework.async

import java.util.concurrent.TimeUnit

class Retry {

    static void withRetry(long timeout, TimeUnit unit, Closure closure) {
        PollingConditions pollingConditions = [timeout: TimeUnit.SECONDS.convert(timeout, unit)]
        pollingConditions.eventually closure
    }
}
