package br.unb.cic.tdp.proof;

import lombok.val;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SortingCoordinator<K, Sorting> {

    private final ConcurrentHashMap<K, CompletableFuture<Optional<Sorting>>> inFlight = new ConcurrentHashMap<>();

    /**
     * Result of tryCompute indicating whether computation was performed.
     */
    public static class ComputeResult<T> {
        private final ResultType type;
        private final T value;

        private enum ResultType {
            COMPUTED, ALREADY_IN_PROGRESS, NOT_FOUND
        }

        private ComputeResult(ResultType type, T value) {
            this.type = type;
            this.value = value;
        }

        public static <T> ComputeResult<T> computed(T value) {
            return new ComputeResult<>(ResultType.COMPUTED, value);
        }

        public static <T> ComputeResult<T> alreadyInProgress() {
            return new ComputeResult<>(ResultType.ALREADY_IN_PROGRESS, null);
        }

        public static <T> ComputeResult<T> notFound() {
            return new ComputeResult<>(ResultType.NOT_FOUND, null);
        }

        public boolean isComputed() {
            return type == ResultType.COMPUTED;
        }

        public boolean isAlreadyInProgress() {
            return type == ResultType.ALREADY_IN_PROGRESS;
        }

        public boolean isNotFound() {
            return type == ResultType.NOT_FOUND;
        }

        public T getValue() {
            if (type != ResultType.COMPUTED) {
                throw new IllegalStateException("No value available for " + type);
            }
            return value;
        }
    }

    /**
     * Attempts to compute the result for the given key if no other thread is already computing it.
     *
     * @param key the key to compute
     * @param compute the computation to perform
     * @return ComputeResult indicating:
     *         - COMPUTED with value if this thread performed the computation and found a result
     *         - NOT_FOUND if this thread performed the computation but found no result
     *         - ALREADY_IN_PROGRESS if another thread is already computing this key
     */
    public ComputeResult<Sorting> tryCompute(final K key, final Callable<Optional<Sorting>> compute) {
        // Fast path: exit early if computation already in progress
        var existing = inFlight.get(key);
        if (existing != null) {
            return ComputeResult.alreadyInProgress();
        }

        // Try to become the leader by installing our future
        val leaderFuture = new CompletableFuture<Optional<Sorting>>();
        existing = inFlight.putIfAbsent(key, leaderFuture);
        if (existing != null) {
            // Another thread won the race, exit early
            return ComputeResult.alreadyInProgress();
        }

        // We are the leader
        try {
            val result = compute.call();
            leaderFuture.complete(result);
            return result.isPresent() ? ComputeResult.computed(result.get()) : ComputeResult.notFound();
        } catch (final Throwable t) {
            // Propagate error to all waiters
            leaderFuture.completeExceptionally(t);
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        } finally {
            // Important: remove to avoid memory leak and allow retry after completion/failure
            inFlight.remove(key, leaderFuture);
        }
    }
}
