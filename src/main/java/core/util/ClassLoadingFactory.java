package core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that instantiates objects via reflection.
 *
 * @author Sebastian Lindholm
 */
public class ClassLoadingFactory {

    private static Logger logger = LoggerFactory.getLogger(ClassLoadingFactory.class);

    /**
     * Tries to instantiate a class by name.
     *
     * @param name       Qualified name of the class.
     * @param classSuper Class of the object to be returned.
     * @param <T>        Type of the object to be returned.
     * @return An instance of the class identified by the name, and of the type T.
     * Returns null if the loaded class cannot be cast to T.
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public static <T> T loadClass(String name, Class<T> classSuper) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Class<?> classObject = Class.forName(name);
        if (classSuper.isAssignableFrom(classObject)) {
            return (T) classObject.newInstance();
        } else {
            return null;
        }
    }

    /**
     * Convenience method that tries to instantiate a class by name, and if unsuccessful tries a fallback.
     * This method does not throw any exceptions, but logs them instead.
     * Null wil be returned if both the primary and fallback classes can not be instantiated.
     *
     * @param name     Qualified name of the primary class.
     * @param fallback Qualified name of the fallback class.
     * @param clazz    Class-object of the type T.
     * @param <T>      Type of class to load.
     * @return An instance of the primary or fallback classes of the type T. Or null if unsuccessful.
     */
    public static <T> T tryLoadClass(String name, String fallback, Class<T> clazz) {
        T object = null;

        if (name != null) {
            logger.debug("Loading object: {}.", name);
            try {
                object = loadClass(name, clazz);
            } catch (Exception e) {
                logger.warn("Could not load class.", e);
            }
        }

        if (object == null && fallback != null) {
            logger.debug("Loading fallback: {}.", fallback);
            try {
                object = loadClass(fallback, clazz);
            } catch (Exception e) {
                logger.warn("Could not load class.", e);
            }
        }

        if (object == null) {
            logger.warn("Could not load any object for class {}!", clazz.getSimpleName());
        } else {
            logger.info("Loaded : {} as {}.", object.getClass().getName(), clazz.getSimpleName());
        }

        return object;
    }

}
