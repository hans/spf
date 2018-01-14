package edu.mit.bcs.clevros.situated.data;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.learn.validation.AbstractLearner;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.mit.bcs.clevros.data.IIndexableDataCollection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sequence of {@link edu.mit.bcs.clevros.data.IIndexableDataCollection}.
 * Clients can move forward or backward through the sequence of collection
 * based on external criteria -- loss, training time, etc.
 */
public class CurriculumDataCollection<DI extends IDataItem<?>> implements IIndexableDataCollection<DI> {

    public static final ILogger LOG	= LoggerFactory.create(CurriculumDataCollection.class);

    private final List<IIndexableDataCollection<DI>> collections;
    private final List<Integer> stageEpochs;
    private int collectionIdx = 0;

    public CurriculumDataCollection(List<IIndexableDataCollection<DI>> collections, List<Integer> stageEpochs) {
        Set<Integer> epochsSet = new HashSet<>(stageEpochs);
        if (epochsSet.size() != stageEpochs.size())
            throw new IllegalArgumentException("stageEpochs list has duplicates");

        this.collections = collections;
        this.stageEpochs = stageEpochs;
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

    @Override
    public void handleEpoch(int epoch) {
        int idx = stageEpochs.indexOf(epoch);
        if (idx != -1) {
            LOG.info("Changed from collection %d to %d on epoch %d",
                     collectionIdx, idx, epoch);
            collectionIdx = idx;
        }
    }

    public static class Creator<DI extends IDataItem<?>>
            implements IResourceObjectCreator<CurriculumDataCollection<DI>> {

        @Override
        public CurriculumDataCollection<DI> create(ParameterizedExperiment.Parameters params, IResourceRepository repo) {
            List<IIndexableDataCollection<DI>> stages = params.getSplit("stages").stream()
                    .map((id) -> (IIndexableDataCollection<DI>) repo.get(id))
                    .collect(Collectors.toList());
            List<Integer> stageEpochs = params.getSplit("stageEpochs").stream()
                    .map(Integer::parseInt).collect(Collectors.toList());

            return new CurriculumDataCollection<>(stages, stageEpochs);
        }

        @Override
        public String type() {
            return "data.curriculum";
        }

        @Override
        public ResourceUsage usage() {
            return new ResourceUsage.Builder(type(),
                    CurriculumDataCollection.class)
                    .setDescription("Sequence of datasets")
                    .build();
        }

    }

}
