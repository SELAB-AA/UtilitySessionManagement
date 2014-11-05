package core.optimizer;

import core.BasicSession;
import core.storage.SessionStorage;

public interface SizeEvaluator {

    public long evaluateLocal(BasicSession session);
    public long evaluateRemote(BasicSession session, SessionStorage storage);

}
