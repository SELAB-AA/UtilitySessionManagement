package core.storage;

import java.io.Serializable;
import java.util.Map;

/**
 * A stripped-down version of the BasicSession, holding serialization-relevant data.
 *
 * @author Sebastian Lindholm
 */
public class SessionData implements Serializable {

    private String clusterId;
    private long created;
    private int requests;
    private Map<String, Object> attributes;
    private int maxIdle;

    public SessionData() {

    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

}
