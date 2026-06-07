package com.thy.fss.common.inmemory.testmodel;

import java.time.Duration;

public class TestUtil {

    private TestUtil() {
    }

    public static void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void await(Duration duration) {
        await(duration.toMillis());
    }
}
