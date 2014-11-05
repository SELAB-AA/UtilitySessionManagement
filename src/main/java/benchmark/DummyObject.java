package benchmark;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DummyObject implements Serializable {

    private static Random rng = new Random();

    private byte[] data;
    private Set<DummyObject> references = new HashSet<>(16);

    public DummyObject() {

    }

    public DummyObject(int maxWords) {
        data = LoremIpsum.getLoremIpsumWords(rng.nextInt(maxWords) + 1);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Set<DummyObject> getReferences() {
        return references;
    }

    public void setReferences(Set<DummyObject> references) {
        this.references = references;
    }

    @Override
    public String toString() {
        return "{ DummyObject: data: " + data.length + " refs: " + references.size() + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DummyObject) {
            DummyObject other = (DummyObject) o;
            if (Arrays.equals(this.getData(), other.getData())) {
                return this.getReferences().size() == other.getReferences().size();
            }
        }

        return false;
    }

}
