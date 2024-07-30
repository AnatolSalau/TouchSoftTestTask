import java.util.concurrent.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map;
import java.util.Random;

public class ConsumerTest {

    private final ConcurrentNavigableMap<Long, Integer> numberMap = new ConcurrentSkipListMap<>();
    private final ExecutorService executorService = ForkJoinPool.commonPool();;
    private final long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;

    /**
     * Called periodically to obtain an integer.
     */
    public void accept(int number) {
        long currentTime = System.currentTimeMillis();
        numberMap.put(currentTime, number);
    }

    /**
     * Returns the average (that is, the mean) of the numbers used in
     * the last 5 minutes.
     */
    public CompletableFuture<Double> mean() {
        long startTime = System.nanoTime(); // Start time in nanoseconds

        return CompletableFuture.supplyAsync(() -> {
            long currentTime = System.currentTimeMillis();
            long cutOffTime = currentTime - FIVE_MINUTES_IN_MILLIS;

            // Retrieve all numbers within the last 5 minutes
            Map<Long, Integer> subMap = numberMap.tailMap(cutOffTime);
            long sum = 0;
            int count = 0;

            for (int number : subMap.values()) {
                sum += number;
                count++;
            }

            return count == 0 ? 0.0 : sum / (double) count;
        }, executorService).thenApply(mean -> {
            long endTime = System.nanoTime(); // End time in nanoseconds
            long duration = endTime - startTime;
            System.out.println("Mean calculation executed in " + duration + " ns, result: " + mean);
            return mean;
        });
    }

    public static void main(String[] args) {
        ConsumerTest consumer = new ConsumerTest();
        Random random = new Random();

        // Start time
        long startTime = System.currentTimeMillis();

        // Simulate large load: Accept a large number of integers
        for (int i = 0; i < 100000; i++) {
            int number = random.nextInt(1000); // Random number between 0 and 999
            consumer.accept(number);

            // Periodically calculate the mean
            if (i % 1000 == 0) {
                CompletableFuture<Double> futureMean = consumer.mean();
                int finalI = i;
                futureMean.thenAccept(mean ->
                      System.out.println("Mean at " + finalI + " accepts: " + mean));
            }
        }

        // End time
        long endTime = System.currentTimeMillis();
        System.out.println("Completed large load in " + (endTime - startTime) + " ms");

        // Ensure all tasks are completed and shut down the executor service
        consumer.executorService.shutdown();
        try {
            if (!consumer.executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                consumer.executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            consumer.executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}