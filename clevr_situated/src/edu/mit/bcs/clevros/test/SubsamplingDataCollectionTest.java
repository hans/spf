package edu.mit.bcs.clevros.test;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.mit.bcs.clevros.data.CLEVRAnswer;
import edu.mit.bcs.clevros.data.CLEVRCollection;
import edu.mit.bcs.clevros.data.CLEVRScene;
import edu.mit.bcs.clevros.data.IIndexableDataCollection;
import edu.mit.bcs.clevros.situated.data.SituatedCLEVRCollection;
import edu.mit.bcs.clevros.situated.data.SubsamplingDataCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class SubsamplingDataCollectionTest {

    private CLEVRCollection<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> collection;
    private SubsamplingDataCollection<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> wrappedCollection;

    private static final int subsampleSize = 100;

    @BeforeEach
    public void setUp() {
        List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> entries = new ArrayList<>();
        for (int i = 0; i < 1000; i++)
            entries.add(null);

        collection = new SituatedCLEVRCollection(entries, false);
        wrappedCollection = new SubsamplingDataCollection<>(collection, subsampleSize);
    }

    @Test
    public void testSimpleConstruction() {
        assertEquals(subsampleSize, wrappedCollection.size());
    }

    @Test
    public void testSingleEpoch() {
        wrappedCollection.handleEpoch(1);
        List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> ret = new ArrayList<>();
        wrappedCollection.iterator().forEachRemaining(ret::add);
        assertEquals(subsampleSize, ret.size());
    }

    @Test
    public void testMultipleEpochs() {
        for (int e = 0; e < 12; e++) {
            wrappedCollection.handleEpoch(e);
            List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> ret = new ArrayList<>();
            wrappedCollection.iterator().forEachRemaining(ret::add);
            assertEquals(subsampleSize, ret.size());
        }
    }

}
