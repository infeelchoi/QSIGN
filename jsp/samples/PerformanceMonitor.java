
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceMonitor {

  private static final int timeInterval = 20; // in seconds

  // we can use a vanilla array list because nothing will be removed.
  private static List<Integer> transactionTimes = Collections.synchronizedList(new ArrayList<Integer>());

  // for measuring the starting time when things are placed in our array of transaction times
  private static long nanoStart = 0;
  private static int threadCount;

  // add a record to the list of transaction times.
  // we're doing it this way so we can track max / min / average and various percentiles.
  public static void addRecord(Integer transactionTime) {
    transactionTimes.add(transactionTime);
  }

  // this method is where the initial thread will live.
  // it will continue to monitor things until the applicationis terminated
  public static void StartMonitor(int threads) {
    threadCount = threads;
    try {
      while (true) {

        System.out.println(getLog());

        // sleep for "timeInterval" seconds..
        Thread.sleep(timeInterval * 1000);
      }

    } catch (Exception e) {
      System.out.println("Time: " + new java.util.Date());
      e.printStackTrace();
    }
  }

  public static double round(double value, int places) {
      if (places < 0) throw new IllegalArgumentException();

      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(places, RoundingMode.HALF_UP);
      return bd.doubleValue();
  }

  private static String getLog() {

    long start = nanoStart;

    // first get our arrayList and make a new one
    List<Integer> newList = Collections.synchronizedList(new ArrayList<Integer>());
    List<Integer> ops = transactionTimes;
    transactionTimes = newList;
    long end = System.nanoTime();
    // reset the starting point for the next display
    nanoStart = end;

    Collections.sort(ops);

    // calculate per second:
    double elapsedSeconds = (end - start) / 1000000000.0;
    double elapsedMilliseconds = (end - start) / 1000000.0;
    double throughput = ops.size() / elapsedSeconds;

    if (ops.size() == 0) return ("Background monitor:\n*** No work done yet\n");
    int totalOps = ops.size();

    String date = new java.util.Date().toString();
    int tps30 = ops.get(PercentileIndex(30, totalOps));
    int tps50 = ops.get(PercentileIndex(50, totalOps));
    int tps75 = ops.get(PercentileIndex(75, totalOps));
    int tps90 = ops.get(PercentileIndex(90, totalOps));
    int tps99 = ops.get(PercentileIndex(99, totalOps));
    int tps999 = ops.get(PercentileIndex(99.9, totalOps));

    double sum = 0;
    for(int x : ops) {
      sum += x;
    }
    double avg = sum / ops.size();
    avg = round(avg,2);
    throughput = round(throughput,2);

    String result = "Background monitor:\n--- THROUGHPUT RESULT ---" + date + "\n" + "Thread Count: " + threadCount
        + "  Total Operations: " + totalOps + " Report Time: " + (int) elapsedMilliseconds + "ms\n"
//        + "Throughput: " + (int) Math.floor(throughput) + "ops/s  MIN latency: " + ops.get(0)
        + "Throughput: " + throughput + "ops/s  MIN latency: " + ops.get(0)
        + "ms  MAX latency: " + ops.get(totalOps - 1) + "ms\n" + "AVG latency: " + avg + "ms\n" + "30%: "
        + tps30 + "ms 50%: " + tps50 + "ms 75%: " + tps75 + "ms 90%: " + tps90 + "ms 99%: " + tps99
        + "ms 99.9%: " + tps999 + "ms\n\n";

    return result;
  }

  public static int PercentileIndex(double percentile, int dataSize) {
    int value = ((int) (Math.ceil(percentile * dataSize) / 100) - 1);

    return (value < 0) ? 0 : value;
  }

}
