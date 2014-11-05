package benchmark;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class LoremIpsum {

    private static final String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque vitae " +
            "posuere metus. Sed ligula mauris, iaculis vitae lobortis vel, tristique ac metus. Donec suscipit vel tortor " +
            "ullamcorper pharetra. Praesent faucibus elit a elit semper, id semper est adipiscing. Curabitur dignissim " +
            "tincidunt erat, eu lacinia mauris elementum at. Donec tempus erat sed dignissim malesuada. Praesent ut " +
            "vehicula justo. Cras eget dignissim ipsum. Nulla accumsan risus eu neque eleifend, eu rutrum augue congue. " +
            "Etiam laoreet ipsum eget est commodo, eu consectetur lorem ultrices. Donec placerat hendrerit lorem, congue " +
            "ultricies orci dignissim ac. Duis a nulla eget leo venenatis luctus ut eu quam. In hac habitasse platea " +
            "dictumst. Sed in posuere tellus. Nulla tincidunt, libero id volutpat porttitor, nulla ligula posuere nulla, " +
            "ac vulputate est odio auctor sapien. Ut a porta velit. Nunc quis posuere mauris, eu varius enim. Vestibulum " +
            "sem lorem, accumsan et placerat vitae, euismod sed est. Quisque fermentum ipsum id nisl dignissim amet.";

    private static byte[] loremIpsumBytes;
    private static String[] loremIpsumWords;

    private static byte[] getBytes() {
        if (loremIpsumBytes == null) {
            try {
                loremIpsumBytes = loremIpsum.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                loremIpsumBytes = loremIpsum.getBytes();
            }
        }

        return loremIpsumBytes;
    }

    private static String[] getWords() {
        if (loremIpsumWords == null) {
            loremIpsumWords = loremIpsum.split(" ");
        }

        return loremIpsumWords;
    }

    public static byte[] getLoremIpsumBytes(int bytes) {
        byte[] loremIpsum = getBytes();
        byte[] result = new byte[bytes];
        Random rng = new Random();

        for (int i = 0; i < bytes; i++) {
            result[i] = loremIpsum[rng.nextInt(loremIpsum.length)];
        }

        return result;
    }

    public static byte[] getLoremIpsumWords(int amount) {
        String[] words = getWords();
        Random rng = new Random();
        StringBuilder builder = new StringBuilder();

        builder.append(words[rng.nextInt(words.length)]);
        for (int i = 1; i < amount; i++) {
            builder.append(" ");
            builder.append(words[rng.nextInt(words.length)]);
        }

        try {
            return builder.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return builder.toString().getBytes();
        }

    }

}
