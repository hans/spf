package edu.mit.bcs.clevros.data;

import edu.cornell.cs.nlp.spf.base.exceptions.FileReadingException;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.ITokenizer;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRAnswer;
import edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRScene;
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

/**
 * Collection of {@link SituatedSentence}.
 *
 */
public class CLEVRCollection
		implements IDataCollection<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> {

	private static final long					serialVersionUID	= -3259824918810436454L;
	private final List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>>	entries;

	public CLEVRCollection(List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> entries) {
		this.entries = Collections.unmodifiableList(entries);
	}

	public static CLEVRCollection read(File scenesFile, File questionsFile) {
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

        return new CLEVRCollection(entries);
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

	@Override
	public Iterator<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> iterator() {
		return entries.iterator();
	}

	@Override
	public int size() {
		return entries.size();
	}

	public static class Creator
		implements IResourceObjectCreator<CLEVRCollection> {

		@Override
		public CLEVRCollection create(ParameterizedExperiment.Parameters params,
                                      IResourceRepository repo) {
			return CLEVRCollection.read(params.getAsFile("scenesFile"),
					params.getAsFile("questionsFile"));
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
