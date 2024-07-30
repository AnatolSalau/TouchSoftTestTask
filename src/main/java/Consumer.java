import java.util.Map;
import java.util.concurrent.*;

class Consumer
{
	private final ConcurrentNavigableMap<Long, Integer> numberMap = new ConcurrentSkipListMap<>();
	private final ExecutorService executorService = ForkJoinPool.commonPool();
	private final long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;

	/**
	 * Called periodically to consume an integer.
	 */

	public void accept( int number ) {
		long currentTime = System.currentTimeMillis();
		numberMap.put(currentTime, number);
	};

	/**
	 * Returns the mean (aka average) of numbers consumed in the 
       * last 5 minute period.
	 */

	public CompletableFuture<Double> mean() {
		return CompletableFuture.supplyAsync(() -> {
			long currentTime = System.currentTimeMillis();
			long cutOffTime = currentTime - FIVE_MINUTES_IN_MILLIS;

			Map<Long, Integer> subMap = numberMap.tailMap(cutOffTime);
			long sum = 0;
			int count = 0;

			for (int number : subMap.values()) {
				sum += number;
				count++;
			}

			return count == 0 ? 0.0 : sum / (double) count;
		}, executorService);
	}
}
