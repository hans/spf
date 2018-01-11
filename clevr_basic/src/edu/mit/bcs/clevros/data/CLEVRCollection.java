package edu.mit.bcs.clevros.data;

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.ITokenizer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class CLEVRCollection<DI extends ILabeledDataItem<?, ?>>
        implements IIndexableDataCollection<DI> {

    protected final List<DI> entries;
    protected final boolean shuffle;

    protected CLEVRCollection(boolean shuffle) {
        this.entries = new ArrayList<>();
        this.shuffle = shuffle;
    }

    public CLEVRCollection(List<DI> entries, boolean shuffle) {
        this.entries = Collections.unmodifiableList(entries);
        this.shuffle = shuffle;
    }

    public void addAll(List<DI> entries) {
        this.entries.addAll(entries);
    }

    protected abstract DI readQuestionJSON(JSONObject questionData, CLEVRScene scene);

    public static <SAMPLE, LABEL, DI extends ILabeledDataItem<SAMPLE, LABEL>,
                   COLL extends CLEVRCollection<DI>> COLL
      read(Class<COLL> collType, File scenesFile, File questionsFile, boolean shuffle, int subsampleQuestions) {
        File curFile = null;

        List<DI> entries = new ArrayList<>();
        final COLL collection;

        try {
            collection = collType.getDeclaredConstructor(Boolean.TYPE).newInstance(shuffle);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        try {
            // Read scenes file.
            curFile = scenesFile;
            List<CLEVRScene> scenes = new ArrayList<>();
            JSONObject scenesObj = (JSONObject) new JSONParser()
                    .parse(new FileReader(scenesFile));
            JSONArray allScenes = (JSONArray) scenesObj.get("scenes");
            for (Object sceneData : allScenes) {
                CLEVRScene scene = CLEVRScene.buildFromJSON((JSONObject) sceneData);
                // sanity check: scenes should be sorted by idx in data
                assert scenes.size() == scene.getImageIndex();
                scenes.add(scene);
            }

            // Read questions file.
            curFile = questionsFile;
            JSONObject questionsObj = (JSONObject) new JSONParser()
                    .parse(new FileReader(questionsFile));
            JSONArray allQuestions = (JSONArray) questionsObj.get("questions");
            for (Object questionData : allQuestions) {
                JSONObject questionObj = (JSONObject) questionData;
                CLEVRScene scene = scenes.get(((Long) questionObj.get("image_index")).intValue());

                entries.add(collection.readQuestionJSON(questionObj, scene));
            }
        } catch (final Exception e) {
            throw new FileReadingException(e, 0, curFile.getName());
        }

        if (subsampleQuestions != 0) {
            if (!shuffle)
                throw new IllegalArgumentException("subsampling without shuffling not supported");

            Collections.shuffle(entries);
            entries = entries.subList(0, subsampleQuestions);
        }

        collection.addAll(entries);
        return collection;
    }

    public static class CLEVRTokenizer implements ITokenizer {
        @Override
        public TokenSeq tokenize(String sentence) {
            sentence = sentence.replaceAll("([.?!;])", " $1 ");
            final List<String> tokens = new ArrayList<String>();
            final StringTokenizer st = new StringTokenizer(sentence);
            while (st.hasMoreTokens()) {
                tokens.add(st.nextToken().trim());
            }
            return TokenSeq.of(tokens);
        }
    }

    public class RandomIterator<DI extends IDataItem<?>> implements Iterator<DI> {

        private Iterator<Integer> indices;

        IIndexableDataCollection<DI> delegate;

        public RandomIterator(IIndexableDataCollection<DI> delegate) {
            this.delegate = delegate;

            List<Integer> idxs = IntStream.range(0, delegate.size()).boxed().collect(Collectors.toList());
            Collections.shuffle(idxs);
            indices = idxs.iterator();
        }

        @Override
        public boolean hasNext() {
            return indices.hasNext();
        }

        @Override
        public DI next() {
            return delegate.get(indices.next());
        }
    }

    @Override
    public Iterator<DI> iterator() {
        if (shuffle) {
            return new RandomIterator<>(this);
        } else {
            return entries.iterator();
        }
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public DI get(int idx) {
        return entries.get(idx);
    }

}
