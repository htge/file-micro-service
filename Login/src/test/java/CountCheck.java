import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

public class CountCheck {
    private final AtomicInteger count = new AtomicInteger();

    public void initialize(int number) {
        count.set(number);
    }

    public void assertTrue(boolean value) {
        if (value == false) {
            Assert.assertTrue(value);
            count.incrementAndGet();
        }
    }

    public int getCount() {
        return count.get();
    }
}
