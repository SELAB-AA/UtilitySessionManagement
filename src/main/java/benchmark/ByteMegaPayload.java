package benchmark;

import java.util.Random;

public class ByteMegaPayload implements Payload {

    private static final int UNIT = 1048576; // MB

    private final Random rng = new Random();

    @Override
    public Object getPayload() {
        return LoremIpsum.getLoremIpsumBytes(rng.nextInt(4 * UNIT) + UNIT);
    }

}
