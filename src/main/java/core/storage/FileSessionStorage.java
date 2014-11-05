package core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * SessionStorage saving sessions to the local filesystem.
 *
 * @author Sebastian Lindholm
 */
public class FileSessionStorage extends AbstractSessionStorage {

    private static final Logger logger = LoggerFactory.getLogger(FileSessionStorage.class);

    private static final String suffix = ".session";
    private static final String directoryName = "sessions";

    private final Path directory;

    public FileSessionStorage() throws IOException {
        this.directory = Paths.get(directoryName);
        Files.createDirectories(directory);
    }

    public boolean store(SessionData session) {
        String fileName = session.getClusterId() + suffix;
        File file = Paths.get(directory.toString(), fileName).toFile();
        boolean success = false;

        try (
                OutputStream out = getCompressor().compress(new BufferedOutputStream(new FileOutputStream(file), 51200))
        ) {
            getSerializer().writeSessionData(session, out);
            out.flush();
            success = true;
        } catch (FileNotFoundException e) {
            logger.warn("Could not find file when writing session to disk", e);
        } catch (IOException e) {
            logger.warn("Error when storing session {}.", e);
        }

        return success;
    }

    public SessionData load(String id) {
        String fileName = id + suffix;
        File file = Paths.get(directory.toString(), fileName).toFile();
        SessionData data = null;

        if (file.exists()) {
            try (
                    InputStream fileIn = new FileInputStream(file);
                    InputStream in = getCompressor().decompress(new BufferedInputStream(fileIn, fileIn.available()))
            ) {
                data = getSerializer().readSessionData(in);
                in.close();
            } catch (FileNotFoundException e) {
                logger.debug("Could not find file when attempting to load session.", e);
            } catch (IOException e) {
                logger.warn("Error while attempting to load a session from disk.", e);
            }
        }

        return data;
    }

    public void remove(String id) {
        String fileName = id + suffix;
        Path path = Paths.get(directory.toString(), fileName);
        File file = path.toFile();
        if (file.delete()) {
            logger.warn("Failed to delete file for session {}.", id);
        }
    }

    public List<StoredSession> stored() {
        File folder = directory.toFile();
        File[] files = folder.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(suffix);
            }

        });
        List<StoredSession> sessions = new ArrayList<StoredSession>(files.length);

        for (File file : files) {
            sessions.add(new StoredSession(file.getName().substring(0, file.getName().lastIndexOf(".session")), file.length()));
        }

        return sessions;
    }

    public long capacity() {
        return directory.toFile().getTotalSpace();
    }

}
