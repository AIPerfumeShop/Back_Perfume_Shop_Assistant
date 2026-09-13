package com.example.spring_boot_project_api.util;

import java.util.function.Supplier;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Component;

@Component
public class RetryUtil {

    private static final int MAX_ATTEMPTS = 4;
    private static final long INITIAL_DELAY_MS = 150;

    /**
     * Retries the supplied action when the database reports a deadlock (two
     * chat senders updating the same ticket row at the same time). The action
     * must go through a Spring proxy so each attempt runs in its own
     * transaction — a rolled-back attempt never poisons the next one.
     */
    public <T> T onDeadlock(Supplier<T> action) {
        long delay = INITIAL_DELAY_MS;
        Throwable last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (CannotAcquireLockException | DeadlockLoserDataAccessException e) {
                last = e;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                sleep(delay);
                delay *= 2;
            }
        }
        throw new IllegalStateException(
                "Could not complete the operation after " + MAX_ATTEMPTS + " attempts", last);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying", e);
        }
    }
}