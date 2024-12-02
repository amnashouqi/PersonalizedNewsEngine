import java.util.concurrent.*;

public class ConcurrencyHandler {
    private final ExecutorService executorService;

    // Constructor for initializing the thread pool
    public ConcurrencyHandler(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    // Submit a task for execution
    public Future<?> submitTask(Runnable task) {
        return executorService.submit(task); // Returns a Future for task tracking
    }


}
