package edu.mit.bcs.clevros.situated.data;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.mit.bcs.clevros.data.IIndexableDataCollection;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stateful data collection wrapper which yields a fixed-size random subsample
 * of the wrapped data set on each epoch. (Order not enforced.)
 */
public class SubsamplingDataCollection<DI extends IDataItem<?>> implements IDataCollection<DI> {

    private static final ILogger LOG = LoggerFactory.create(SubsamplingDataCollection.class);

    private final IIndexableDataCollection<DI> collection;

    private final int subsampleSize;
    private Iterator<DI> currentSubsampleIterator;

    public SubsamplingDataCollection(IIndexableDataCollection<DI> collection, int subsampleSize) {
        this.collection = collection;
        this.subsampleSize = subsampleSize;
    }

    private class SubsampleIterator implements Iterator<DI> {
        private final Iterator<Integer> idxIterator;

        public SubsampleIterator(List<Integer> idxs) {
            this.idxIterator = idxs.iterator();
        }

        @Override
        public boolean hasNext() {
            return idxIterator.hasNext();
        }

        @Override
        public DI next() {
            return collection.get(idxIterator.next());
        }
    }

    private Iterator<DI> makeSubsampler() {
        if (subsampleSize > collection.size())
            return collection.iterator();

        List<Integer> idxs = IntStream.range(0, collection.size()).boxed()
                .collect(Collectors.toList());
        Collections.shuffle(idxs);

        LOG.info("new subsampler with first idx: " + idxs.get(0).toString());
        return new SubsampleIterator(idxs.subList(0, subsampleSize));
    }

    @Override
    public int size() {
        return subsampleSize;
    }

    @Override
    public Iterator<DI> iterator() {
        if (currentSubsampleIterator == null)
            currentSubsampleIterator = makeSubsampler();

        return currentSubsampleIterator;
    }

    @Override
    public void handleEpoch(int epoch) {
        // Reset subsampler.
        currentSubsampleIterator = makeSubsampler();
    }

    public static class Creator<DI extends IDataItem<?>>
            implements IResourceObjectCreator<SubsamplingDataCollection<DI>> {

        @Override
        public SubsamplingDataCollection<DI> create(ParameterizedExperiment.Parameters params,
                                                    IResourceRepository repo) {
            IIndexableDataCollection<DI> collection = (IIndexableDataCollection<DI>) repo.get(params.get("collection"));
            int subsampleSize = params.getAsInteger("subsampleSize");

            return new SubsamplingDataCollection<>(collection, subsampleSize);
        }

        @Override
        public String type() {
            return "data.subsampler";
        }

        @Override
        public ResourceUsage usage() {
            return new ResourceUsage.Builder(type(),
                    CurriculumDataCollection.class)
                    .setDescription("Sequence of datasets")
                    .addParam("collection", "id", "")
                    .addParam("subsampleSize", "int", "")
                    .build();
        }

    }

}
