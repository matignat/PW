import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

public class ArrayRearrangement {
    private static final int MAX_DELAY_MS = 4000;
    private static final int THREADS_COUNT = 10;
    private static final int[] data = new int[THREADS_COUNT];

    private static int counter = 0;

    private static final CyclicBarrier barrier = new CyclicBarrier(THREADS_COUNT,
        ArrayRearrangement::printData);

    private static void printData() {
        // This method is always run by a single thread, with others waiting,
        // so it's safe to use non-atomic operations here.

        counter++;

        System.out.print("Now everyone has ");
        if (counter == 1) {
            System.out.println("set their cell to their id.");
        } else if (counter == 2) {
            System.out.println("read the value from the other end of the array and reset it to -1.");
        } else if (counter == 3) {
            System.out.println("set their cell to the previously read value.");
        } else {
            throw new RuntimeException();
        }

        System.out.println("The data is now: " + Arrays.toString(data));

    }

    private static void sleep() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(MAX_DELAY_MS));
        System.out.println(Thread.currentThread().getName() + "  is ready.");
    }

    public static void main(final String[] args) {
        Thread[] threads = new Thread[THREADS_COUNT];
        for (int i = 0; i < THREADS_COUNT; ++i) {
            threads[i] = new Thread(new Helper(i), "Helper" + i);
        }
        for (int i = 0; i < THREADS_COUNT; ++i) {
            threads[i].start();
        }
        try {
            for (int i = 0; i < THREADS_COUNT; ++i) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            System.err.println("Main interrupted");
        }
    }

    private static class Helper implements Runnable {
        private final int id;

        public Helper(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                sleep();
                data[id] = id;
                barrier.await();

                sleep();
                int value = data[THREADS_COUNT - 1 - id];
                data[THREADS_COUNT - 1 - id] = -1;
                barrier.await();

                sleep();
                data[id] = value;
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                System.err.println(Thread.currentThread().getName() + " interrupted.");
            }
        }
    }
}
