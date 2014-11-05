package benchmark;

import java.util.Random;

public class ObjectHierarchyPayload implements Payload {

    private static Random rng = new Random();

    @Override
    public Object getPayload() {
        return getPayload(100, 3, 1000);
    }

    public Object getPayload(int objects, int maxReferences, int maxWords) {
        DummyObject[] dummies = new DummyObject[objects];

        for (int i = 0; i < dummies.length; i++) {
            dummies[i] = new DummyObject(maxWords);
            int refs = rng.nextInt(maxReferences + 1);

            for (int j = 0; j < refs; j++) {
                dummies[i].getReferences().add(dummies[rng.nextInt(dummies.length)]);
            }
        }

        return dummies;
    }

}
