package benchmark;

public class WordsKiloPayload implements Payload {

    private static final int UNIT = 1000;

    @Override
    public Object getPayload() {
        return LoremIpsum.getLoremIpsumWords(UNIT);
    }

}
