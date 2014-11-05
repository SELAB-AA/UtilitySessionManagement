package benchmark;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class LoremIpsumTest {

    private Random rng;

    @Before
    public void setup() {
        rng = new Random();
    }

    @Test
    public void testLoremIpsumBytes() {
        int amount = rng.nextInt(15) + 1;
        byte[] bytes = LoremIpsum.getLoremIpsumBytes(amount);

        assertEquals(amount, bytes.length);

        StringBuilder builder = new StringBuilder();

        for (byte b : bytes) {
            builder.append((char) b);
        }

        System.out.println(builder.toString());
    }

    @Test
    public void testLoremIpsumWords() {
        int amount = rng.nextInt(15) + 1;
        byte[] bytes = LoremIpsum.getLoremIpsumWords(amount);

        StringBuilder builder = new StringBuilder();

        for (byte b : bytes) {
            builder.append((char) b);
        }

        System.out.println(builder.toString());
    }
}
