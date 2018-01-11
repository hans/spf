package edu.mit.bcs.clevros.situated.data;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.mit.bcs.clevros.data.IIndexableDataCollection;

import java.util.Iterator;
import java.util.List;

/**
 * Sequence of {@link edu.mit.bcs.clevros.data.IIndexableDataCollection}.
 * Clients can move forward or backward through the sequence of collection
 * based on external criteria -- loss, training time, etc.
 */
public class CurriculumDataCollection<DI extends IDataItem<?>> implements IIndexableDataCollection<DI> {

    private final List<IIndexableDataCollection<DI>> collections;
    private int collectionIdx = 0;

    public CurriculumDataCollection(List<IIndexableDataCollection<DI>> collections) {
        this.collections = collections;
    }

    private IIndexableDataCollection<DI> currentCollection() {
        return collections.get(collectionIdx);
    }

    public boolean previousCollection() {
        if (collectionIdx == 0)
            return false;

        collectionIdx--;
        return true;
    }

    public boolean nextCollection() {
        if (collectionIdx >= collections.size() - 1)
            return false;

        collectionIdx++;
        return true;
    }

    @Override
    public DI get(int idx) {
        return currentCollection().get(idx);
    }

    @Override
    public int size() {
        return currentCollection().size();
    }

    @Override
    public Iterator<DI> iterator() {
        return currentCollection().iterator();
    }
}
