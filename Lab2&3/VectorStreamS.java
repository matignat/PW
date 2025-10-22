import java.util.concurrent.Semaphore;
import java.util.function.IntBinaryOperator;

public class VectorStreamS {
    private static final int STREAM_LENGTH = 10;
    private static final int VECTOR_LENGTH = 100;

    /**
     * Function that defines how vectors are computed: the i-th element depends on
     * the previous sum and the index i.
     * The sum of elements in the previous vector is initially given as zero.
     */
    private final static IntBinaryOperator vectorDefinition = (previousSum, i) -> {
        int a = 2 * i + 1;
        return (previousSum / VECTOR_LENGTH + 1) * (a % 4 - 2) * a + 1;
    };

    private static void computeVectorStreamSequentially() {
        int[] vector = new int[VECTOR_LENGTH];
        int sum = 0;
        for (int vectorNo = 0; vectorNo < STREAM_LENGTH; ++vectorNo) {
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                vector[i] = vectorDefinition.applyAsInt(sum, i);
            }
            sum = 0;
            for (int x : vector) {
                sum += x;
            }
            System.out.println(vectorNo + " -> " + sum);
        }
    }

    //--------------------SEMAPHORE--------------------//
    static Semaphore waiting = new Semaphore(0, true);
    static Semaphore working = new Semaphore(0, true);
    private static int sum = 0;
    private static int counter = 0;
    private static int[] vector = new int[VECTOR_LENGTH];

    private static void computeVectorStreamInParallel() throws InterruptedException {
        Thread[] threads = new Thread[VECTOR_LENGTH];

        for (int i = 0; i < VECTOR_LENGTH; i++) {
            threads[i] = new Thread(new VectorStreamS.Helper(i));
        }

        for (int i = 0; i < VECTOR_LENGTH; i++) {
            threads[i].start();
        }

        try {
            for (int i = 0; i < STREAM_LENGTH; i++) {
                // Let threads run
                working.release(VECTOR_LENGTH);

                // Wait for all to finish
                waiting.acquire(VECTOR_LENGTH);

                // Update sum
                sum = 0;
                for (int x : vector) sum += x;

                System.out.println(counter + " -> " + sum);
                counter++;
            }

            for (int i = 0; i < VECTOR_LENGTH; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            for (Thread t : threads) t.interrupt();

            for (Thread t : threads) {
                while (t.isAlive()) {
                    try {
                        t.join();
                    } catch (InterruptedException e1) {
                        Thread.interrupted();
                    }
                }
            }

            System.out.println("Main method problem");

            throw e;
        }
    }

    // Helper with run method for all the threads
    private static class Helper implements Runnable {
        private final int idx;

        public Helper(int idx) {
            this.idx = idx;
        }

        @Override
        public void run() {

            for (int i = 0; i < STREAM_LENGTH; i++) {
                if (Thread.currentThread().isInterrupted()) return;

                try {
                    // Wait for start approval
                    working.acquire();
                    // Operations
                    vector[idx] = vectorDefinition.applyAsInt(sum, idx);
                    // Let main thread know you are ready
                    waiting.release();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Run method problem");
                    return;
                }
            }
        }
    }

    //---------------------------------------------------//
    public static void main(String[] args) {
        try {
            System.out.println("-- Sequentially --");
            computeVectorStreamSequentially();
            System.out.println("-- Parallel --");
            computeVectorStreamInParallel();
            System.out.println("-- End --");
        } catch (InterruptedException e) {
            System.err.println("Main interrupted.");
        }
    }
}
