package core.util;

import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PropertyParserTest {

    private static final String config = "storage.conf";
    private PropertyParser parser;
    private Map<String, String> properties;

    @Before
    public void setUp() {
        try {
            parser = new PropertyParser(Paths.get("WebContent", "UTIL-CONF", config));
        } catch (FileNotFoundException e) {
            fail(e.toString());
        }
        properties = null;
    }

    @Test
    public void testParse() {
        try {
            properties = parser.parse();
        } catch (IOException e) {
            fail(e.toString());
        }

        assertTrue(properties != null);

        Set<String> keys = properties.keySet();

        for (String key : keys) {
            String value = properties.get(key);
            assertTrue(value != null);

            System.out.println(key + ": " + value);
        }
    }

}
