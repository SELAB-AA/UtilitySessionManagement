package core;

/**
 * Can receive events on session access.
 */
public interface SessionListener {

    public void sessionAccessed(BasicSession session);

}
