import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

@SuppressWarnings("unused")
public class Vector {
    private static final int SUM_CHUNK_LENGTH = 5;
    private static final int DOT_CHUNK_LENGTH = 5;

    private final int[] elements;

    public Vector(int length) {
        elements = new int[length];
    }

    public Vector(int[] elements) {
        this.elements = Arrays.copyOf(elements, elements.length);
    }

    private final Vector sumSequential(Vector other) {
        if (this.elements.length != other.elements.length) {
            throw new IllegalArgumentException("Vector lengths differ.");
        }
        Vector result = new Vector(this.elements.length);
        for (int i = 0; i < result.elements.length; ++i) {
            result.elements[i] = this.elements[i] + other.elements[i];
        }
        return result;
    }

    private final int dotSequential(Vector other) {
        if (this.elements.length != other.elements.length) {
            throw new IllegalArgumentException("Vector lengths differ.");
        }
        int result = 0;
        for (int i = 0; i < this.elements.length; ++i) {
            result += this.elements[i] * other.elements[i];
        }
        return result;
    }

    private static class SumHelper implements Runnable {
        private final Vector left;
        private final Vector right;
        private final Vector result;
        private final int begin;
        private final int end;

        public SumHelper(Vector left, Vector right, Vector result, int begin, int end) {
            this.left = left;
            this.right = right;
            this.result = result;
            this.begin = begin;
            this.end = end;
        }

        @Override
        public void run() {
            for (int i = begin; i < end; i++) {
                if (Thread.currentThread().isInterrupted()) return;
                result.elements[i] = left.elements[i] + right.elements[i];
            }
        }
    }

    public Vector sum(Vector v) throws InterruptedException {
        if (elements.length != v.elements.length) {
            throw new IllegalArgumentException("Vector lengths differ.");
        }
        Vector result = new Vector(elements.length);

        ArrayList<Thread> threads = new ArrayList<>();

        int nThreads = SUM_CHUNK_LENGTH;
        int section = elements.length / nThreads;

        for (int i = 0; i < nThreads; i++) {
            int begin = section * i;
            int end = (i == nThreads - 1) ? elements.length : begin + section;

            Thread t = new Thread(new SumHelper(this, v, result, begin, end));

            threads.add(t);

            t.start();
        }

        try {
            for (Thread t : threads) t.join();
            return result;

        } catch (InterruptedException e) {

            for (Thread t : threads) t.interrupt();

            for (Thread t : threads) wait_for_end(t);

            throw e;
        }
    }

    private static class DotHelper implements Runnable {
        private final Vector left;
        private final Vector right;
        private final int begin;
        private final int end;
        private final int[] result;
        private final int resultPosition;

        public DotHelper(Vector left, Vector right, int begin, int end, int[] result, int resultPosition) {
            this.left = left;
            this.right = right;
            this.begin = begin;
            this.end = end;
            this.result = result;
            this.resultPosition = resultPosition;
        }

        @Override
        public void run() {
            int sum = 0;
            for (int i = begin; i < end; i++) {
                if (Thread.currentThread().isInterrupted()) return;
                sum += left.elements[i] * right.elements[i];
            }

            result[resultPosition] = sum;
        }
    }

    public int dot(Vector v) throws InterruptedException {
        if (elements.length != v.elements.length) {
            throw new IllegalArgumentException("Vector lengths differ.");
        }

        int nThreads = DOT_CHUNK_LENGTH;
        int[] partialResults = new int[nThreads];
        int total = 0;
        Thread[] threads = new Thread[nThreads];

        int section = elements.length / nThreads;

        for (int i = 0; i < nThreads; i++) {
            int begin = section * i;
            int end = (i == nThreads - 1) ? elements.length : begin + section;

            Thread t = new Thread(new DotHelper(this, v, begin, end, partialResults, i));

            threads[i] = t;

            t.start();
        }

        try {
            for (Thread t : threads) t.join();

            for (int i : partialResults) {
                total += i;
            }

            return total;

        } catch (InterruptedException e) {

            for (Thread t : threads) t.interrupt();

            for (Thread t : threads) wait_for_end(t);

            throw e;
        }
    }

    public void wait_for_end(Thread t) {
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException c1) {
                Thread.interrupted();
            }
        }
    }

    // ----------------------- TESTS -----------------------

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector)) {
            return false;
        }
        Vector other = (Vector) obj;
        return Arrays.equals(this.elements, other.elements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.elements);
    }

    private static final Random random = new Random(42);

    private static Vector generateRandomVector(int length) {
        int[] a = new int[length];
        for (int i = 0; i < length; ++i) {
            a[i] = random.nextInt(10);
        }
        return new Vector(a);
    }

    public static void main(String[] args) {
        try {
            for (int length : new int[]{23, 20}) {
                Vector a = generateRandomVector(length);
                System.out.println("A:        " + a);
                Vector b = generateRandomVector(length);
                System.out.println("B:        " + b);

                Vector c = a.sum(b);
                Vector cSequential = a.sumSequential(b);

                if (!c.equals(cSequential)) {
                    System.out.println("Sum error!");
                    System.out.println("Expected: " + cSequential);
                    System.out.println("Got:      " + c);
                } else {
                    System.out.println("Sum OK:   " + c);
                }

                int d = a.dot(b);
                int dotSequential = a.dotSequential(b);
                if (d != dotSequential) {
                    System.out.println("Dot error! Expected " + dotSequential + ", got " + d + ".");
                } else {
                    System.out.println("Dot OK: " + d);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("computations interrupted");
        }
    }
}
