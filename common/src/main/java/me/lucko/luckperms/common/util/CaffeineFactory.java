package me.lucko.luckperms.common.util;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.ForkJoinPool;

public final class CaffeineFactory {
    private CaffeineFactory() {}

    /**
     * Our own fork join pool for LuckPerms cache operations.
     *
     * By default, Caffeine uses the ForkJoinPool.commonPool instance.
     * However... ForkJoinPool is a fixed size pool limited by Runtime.availableProcessors.
     * Some (bad) plugins incorrectly use this pool for i/o operations, make calls to Thread.sleep
     * or otherwise block waiting for something else to complete. This prevents the LP cache loading
     * operations from running.
     *
     * By using our own pool, we ensure this will never happen.
     */
    private static final ForkJoinPool loaderPool = new ForkJoinPool();

    public static Caffeine<Object, Object> newBuilder() {
        return Caffeine.newBuilder().executor(loaderPool);
    }

}
