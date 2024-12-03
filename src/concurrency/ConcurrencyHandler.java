package concurrency;

import java.util.List;
import java.util.concurrent.*;

public class ConcurrencyHandler {
    private final ExecutorService executorService;

    // Constructor for initializing the thread pool
    public ConcurrencyHandler(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    // Expose the ExecutorService for external use
    public ExecutorService getExecutorService() {
        return executorService;
    }

    // Submit a Runnable task for execution
    public Future<?> submitTask(Runnable task) {
        return executorService.submit(task); // Returns a Future for task tracking
    }

    // Submit a Callable task for execution
    public <T> Future<T> submitTask(Callable<T> task) {
        return executorService.submit(task);
    }

    // Process a batch of tasks and wait for all to complete
    public <T> List<Future<T>> processBatch(List<Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks); // Returns a list of Futures for task results
    }

    // Gracefully shut down the executor service
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
