package benchmark;

public class WordsMegaPayload implements Payload {

    private static final int UNIT = 1000000;

    @Override
    public Object getPayload() {
        return LoremIpsum.getLoremIpsumWords(UNIT);
    }

}
