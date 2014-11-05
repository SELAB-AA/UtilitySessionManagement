package benchmark;

import java.util.Random;

public class ZeroKiloPayload implements Payload {

    private static final int UNIT = 1024; // KB

    private final Random rng = new Random();

    @Override
    public Object getPayload() {
        return new byte[rng.nextInt(4 * UNIT) + UNIT];
    }

}
