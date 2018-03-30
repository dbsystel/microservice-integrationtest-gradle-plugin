package de.db.vz.integrationtestframework.async

import org.spockframework.lang.ConditionBlock
import org.spockframework.runtime.GroovyRuntimeUtil
import org.spockframework.runtime.SpockTimeoutError
import org.spockframework.util.Beta

class PollingConditions {
    private double timeout = 1
    private double initialDelay = 0
    private double delay = 0.1
    private double factor = 1.0

    /**
     * Returns the timeout (in seconds) until which the conditions have to be satisfied.
     * Defaults to one second.
     *
     * @return the timeout (in seconds) until which the conditions have to be satisfied
     */
    double getTimeout() {
        return timeout
    }

    /**
     * Sets the timeout (in seconds) until which the conditions have to be satisfied.
     * Defaults to one second.
     *
     * @param seconds the timeout (in seconds) until which the conditions have to be satisfied
     */
    void setTimeout(double seconds) {
        timeout = seconds
    }

    /**
     * Returns the initial delay (in seconds) before first evaluating the conditions.
     * Defaults to zero seconds.
     */
    double getInitialDelay() {
        return initialDelay
    }

    /**
     * Sets the initial delay (in seconds) before first evaluating the conditions.
     * Defaults to zero seconds.
     *
     * @param seconds the initial delay (in seconds) before first evaluating the conditions
     */
    void setInitialDelay(double seconds) {
        initialDelay = seconds
    }

    /**
     * Returns the delay (in seconds) between successive evaluations of the conditions.
     * Defaults to 0.1 seconds.
     */
    double getDelay() {
        return delay
    }

    /**
     * Sets the delay (in seconds) between successive evaluations of the conditions.
     * Defaults to 0.1 seconds.
     *
     * @param seconds the delay (in seconds) between successive evaluations of the conditions.
     */
    void setDelay(double seconds) {
        this.delay = seconds
    }

    /**
     * Returns the factor by which the delay grows (or shrinks) after each evaluation of the conditions.
     * Defaults to 1.
     */
    double getFactor() {
        return factor
    }

    /**
     * Sets the factor by which the delay grows (or shrinks) after each evaluation of the conditions.
     * Defaults to 1.
     *
     * @param factor the factor by which the delay grows (or shrinks) after each evaluation of the conditions
     */
    void setFactor(double factor) {
        this.factor = factor
    }

    /**
     * Repeatedly evaluates the specified conditions until they are satisfied or the timeout has elapsed.
     *
     * @param conditions the conditions to evaluate
     *
     * @throws InterruptedException if evaluation is interrupted
     */
    @ConditionBlock
    void eventually(Closure<?> conditions) throws InterruptedException {
        within(timeout, conditions)
    }

    /**
     * Repeatedly evaluates the specified conditions until they are satisfied or the specified timeout (in seconds) has elapsed.
     *
     * @param conditions the conditions to evaluate
     *
     * @throws InterruptedException if evaluation is interrupted
     */
    @ConditionBlock
    void within(double seconds, Closure<?> conditions) throws InterruptedException {
        long timeoutMillis = toMillis(seconds)
        long start = System.currentTimeMillis()
        long lastAttempt = 0
        Thread.sleep(toMillis(initialDelay))

        long currDelay = toMillis(delay)
        int attempts = 0

        while (true) {
            try {
                attempts++
                lastAttempt = System.currentTimeMillis()
                GroovyRuntimeUtil.invokeClosure(conditions)
                return
            } catch (Throwable e) {
                long elapsedTime = lastAttempt - start
                if (elapsedTime >= timeoutMillis) {
                    String msg = String.format("Condition not satisfied after %1.2f seconds and %d attempts", elapsedTime / 1000d, attempts)
                    throw new SpockTimeoutError(seconds, msg, e)
                }
                final long timeout = Math.min(currDelay, start + timeoutMillis - System.currentTimeMillis())
                if (timeout > 0) {
                    Thread.sleep(timeout)
                }
                currDelay *= factor
            }
        }
    }

    /**
     * Alias for {@link #eventually(groovy.lang.Closure)}.
     */
    @ConditionBlock
    void call(Closure<?> conditions) throws InterruptedException {
        eventually(conditions)
    }

    /**
     * Alias for {@link #within(double, groovy.lang.Closure)}.
     */
    @ConditionBlock
    void call(double seconds, Closure<?> conditions) throws InterruptedException {
        within(seconds, conditions)
    }

    private long toMillis(double seconds) {
        return (long) (seconds * 1000)
    }
}
