import java.util.concurrent.*;

public class ConcurrencyHandler {
    private final ExecutorService executorService;

    // Constructor for initializing the thread pool
    public ConcurrencyHandler(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    // Submit a task for execution
    public void submitTask(Runnable task) {
        executorService.submit(task);
    }

    // Gracefully shutdown the executor service
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
