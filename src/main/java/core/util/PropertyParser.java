package core.util;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool meant for the parsing of very simple key-value configuration files.
 *
 * @author Sebastian Lindholm
 */
public class PropertyParser implements Closeable {

    private StreamTokenizer reader;
    private InputStream in;

    /**
     * Constructs a new PropertyParser based on the given file path.
     *
     * @param fileName Path object to the configuration file.
     * @throws FileNotFoundException
     */
    public PropertyParser(Path fileName) throws FileNotFoundException {
        File file = fileName.toFile();
        in = new FileInputStream(file);
        Reader isr = new BufferedReader(new InputStreamReader(in));
        reader = new StreamTokenizer(isr);
    }

    /**
     * Parses the configuration file, returning the key-value pairs found.
     *
     * @return A map of key-value pairs.
     * @throws IOException
     */
    public Map<String, String> parse() throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        reader.eolIsSignificant(true);
        reader.slashSlashComments(true);
        reader.slashStarComments(true);
        reader.parseNumbers();
        Property property = Property.KEY;

        int token;
        String key = null;
        Double base = null;
        do {
            token = reader.nextToken();
            switch (property) {

                case KEY: {
                    if (token == StreamTokenizer.TT_WORD) {
                        key = reader.sval;
                        property = Property.VALUE;
                    } else if (token == StreamTokenizer.TT_EOF || token == StreamTokenizer.TT_EOL) {
                        break;
                    } else {
                        throw new PropertyParserException();
                    }

                    break;
                }

                case VALUE: {
                    if (token == StreamTokenizer.TT_WORD) {
                        properties.put(key, reader.sval);
                        property = Property.EOL;
                    } else if (token == StreamTokenizer.TT_NUMBER) {
                        base = reader.nval;
                        property = Property.DOUBLE;
                    } else {
                        throw new PropertyParserException();
                    }

                    break;
                }

                case DOUBLE: {
                    String baseString = String.valueOf(base);
                    if (token == StreamTokenizer.TT_WORD) {
                        try {
                            Double number = Double.parseDouble(baseString + reader.sval);
                            properties.put(key, String.valueOf(number));
                            property = Property.EOL;
                        } catch (NumberFormatException e) {
                            throw new PropertyParserException();
                        }
                    } else if (token == StreamTokenizer.TT_EOL || token == StreamTokenizer.TT_EOF) {
                        properties.put(key, baseString);
                        property = Property.KEY;
                    }

                    break;
                }

                case EOL: {
                    if (token == StreamTokenizer.TT_EOL || token == StreamTokenizer.TT_EOF) {
                        property = Property.KEY;
                    } else {
                        throw new PropertyParserException();
                    }

                    break;
                }

                default: {
                    break;
                }

            }

        }
        while (token != StreamTokenizer.TT_EOF);

        return properties;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    private enum Property {KEY, VALUE, DOUBLE, EOL}

    @SuppressWarnings("serial")
    private class PropertyParserException extends RuntimeException {
        public PropertyParserException() {
            super("Error parsing file at line " + reader.lineno() + ".");
        }
    }

}
