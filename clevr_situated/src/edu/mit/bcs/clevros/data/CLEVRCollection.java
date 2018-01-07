package edu.mit.bcs.clevros.data;

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.ITokenizer;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CLEVRCollection
		implements IIndexableDataCollection<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> {

	private static final long					serialVersionUID	= -3259824918810436454L;
	private final List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>>	entries;
	private final boolean shuffle;

	public CLEVRCollection(List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> entries, boolean shuffle) {
		this.entries = Collections.unmodifiableList(entries);
		this.shuffle = shuffle;
	}

	public static CLEVRCollection read(File scenesFile, File questionsFile, boolean shuffle) {
	    File curFile = null;

	    List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> entries = new ArrayList<>();

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

				String questionStr = ((String) questionObj.get("question")).toLowerCase();
                Sentence sentence = new Sentence(questionStr, new CLEVRTokenizer());

                entries.add(new LabeledSituatedSentence<>(
                        new SituatedSentence<>(sentence, scene),
                        CLEVRAnswer.valueOf(questionObj.get("answer"), scene)
                ));
            }
        } catch (final Exception e) {
	        throw new FileReadingException(e, 0, curFile.getName());
        }

        return new CLEVRCollection(entries, shuffle);
    }

    private static class CLEVRTokenizer implements ITokenizer {
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
	public Iterator<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> iterator() {
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
	public LabeledSituatedSentence<CLEVRScene, CLEVRAnswer> get(int idx) {
		return entries.get(idx);
	}

	public static class Creator
		implements IResourceObjectCreator<CLEVRCollection> {

		@Override
		public CLEVRCollection create(ParameterizedExperiment.Parameters params,
                                      IResourceRepository repo) {
			return CLEVRCollection.read(params.getAsFile("scenesFile"),
					params.getAsFile("questionsFile"), params.getAsBoolean("shuffle", false));
		}

		@Override
		public String type() {
			return "data.clevr";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					CLEVRCollection.class)
					.setDescription("Collection of CLEVR questions+scenes")
					.build();
		}
	}

}
