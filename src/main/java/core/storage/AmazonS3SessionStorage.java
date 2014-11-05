package core.storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import core.util.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A customized Amazon S3 storage class, originally written by mrosin,
 * TODO: Untested
 *
 * @author Sebastian Lindholm
 */
public class AmazonS3SessionStorage extends AbstractSessionStorage {

    private static final Logger logger = LoggerFactory.getLogger(AmazonS3SessionStorage.class);

    private static final String amazonConfig = "amazon.conf";
    private static final String accessParam = "accessKey";
    private static final String secretParam = "secretKey";
    private static final String bucketParam = "bucketName";

    private String accessKey;
    private String secretKey;
    private String bucketName;
    private AmazonS3 client;

    public AmazonS3SessionStorage() {
        try (
                PropertyParser parser = new PropertyParser(Paths.get("WebContent", "UTIL-CONF", amazonConfig))
        ) {
            Map<String, String> config = parser.parse();
            parser.close();
            initClient(config);

        } catch (FileNotFoundException e) {
            logger.warn("No {} found!", amazonConfig);
        } catch (IOException e) {
            logger.warn("Failed to open {}!", amazonConfig);
        }
    }

    public boolean store(SessionData session) {
        String fileName = session + ".session";

        if (client == null)
            return false;

        File file = null;
        try {
            file = File.createTempFile(session.getClusterId(), ".session");
        } catch (IOException e) {
            logger.warn("Error creating temporary file.", e);
            return false;
        }

        boolean success = false;
        try (
                OutputStream out = getCompressor().compress(new BufferedOutputStream(new FileOutputStream(file), 51200))
        ) {
            getSerializer().writeSessionData(session, out);
            out.flush();
            out.close();
            client.putObject(new PutObjectRequest(bucketName, fileName, file));
            file.delete();
            success = true;
        } catch (IOException e) {
            logger.warn("Error while storing a session.", e);
        }

        return success;
    }

    public SessionData load(String id) {
        String fileName = id + ".session";

        if (client == null)
            return null;

        S3Object object = client.getObject(new GetObjectRequest(bucketName, fileName));
        S3ObjectInputStream objectIn = object.getObjectContent();
        SessionData data = null;

        try (InputStream in = getCompressor().decompress(new BufferedInputStream(objectIn, objectIn.available()))) {
            data = getSerializer().readSessionData(in);
            in.close();
        } catch (IOException e) {
            logger.warn("Error while loading object.", e);
        }

        return data;
    }

    private void initClient(Map<String, String> config) {
        Set<String> keys = config.keySet();

        for (String key : keys) {
            if (key.equalsIgnoreCase(accessParam)) {
                accessKey = config.get(key);
            } else if (key.equalsIgnoreCase(secretParam)) {
                secretKey = config.get(key);
            } else if (key.equalsIgnoreCase(bucketParam)) {
                bucketName = config.get(key);
            }
        }

        if (accessKey != null && secretKey != null && bucketName != null) {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            client = new AmazonS3Client(credentials);
            logger.info("Initialized AWS Client with parameters:\n" +
                            "Access Key: \t {}\n" +
                            "Secret Key: \t {}\n" +
                            "Bucket Name: \t {}",
                    accessKey, secretKey, bucketName);
        } else {
            logger.warn("Failed to initialize AWS Client: Invalid {}.", amazonConfig);
        }
    }

    public void remove(String id) {
        String fileName = id + ".session";

        if (client == null)
            return;

        client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
    }

    public List<StoredSession> stored() {
        List<StoredSession> sessions = new ArrayList<StoredSession>();
        ListObjectsRequest request = new ListObjectsRequest().withBucketName(bucketName);
        ObjectListing listing;

        do {
            listing = client.listObjects(request);
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                String key = summary.getKey();
                sessions.add(new StoredSession(key.substring(0, key.lastIndexOf(".session")), summary.getSize()));
            }

            request.setMarker(listing.getNextMarker());
        }
        while (listing.isTruncated());
        return sessions;
    }

    public long capacity() {
        return Long.MAX_VALUE;
    }

}
