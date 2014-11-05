package benchmark;

import java.util.Random;

public class RandomWordsPayload implements Payload {

    private final Random rng = new Random();

    @Override
    public Object getPayload() {
        int words = rng.nextInt(100000)+50000;
        return LoremIpsum.getLoremIpsumWords(words);
    }
}
