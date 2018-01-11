package edu.mit.bcs.clevros.situated.data;

import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.mit.bcs.clevros.data.CLEVRAnswer;
import edu.mit.bcs.clevros.data.CLEVRCollection;
import edu.mit.bcs.clevros.data.CLEVRScene;
import org.json.simple.JSONObject;

import java.util.List;

public class SituatedCLEVRCollection extends CLEVRCollection<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> {

	private static final long					serialVersionUID	= -3259824918810436454L;

    public SituatedCLEVRCollection(boolean shuffle) {
        super(shuffle);
    }

    public SituatedCLEVRCollection(List<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> entries, boolean shuffle) {
		super(entries, shuffle);
	}

	@Override
	protected LabeledSituatedSentence<CLEVRScene, CLEVRAnswer> readQuestionJSON(JSONObject questionData, CLEVRScene scene) {
		String questionStr = ((String) questionData.get("question")).toLowerCase();
		Sentence sentence = new Sentence(questionStr, new CLEVRCollection.CLEVRTokenizer());

		return new LabeledSituatedSentence<>(
				new SituatedSentence<>(sentence, scene),
				CLEVRAnswer.valueOf(questionData.get("answer"), scene)
		);
	}

	public static class Creator
		implements IResourceObjectCreator<SituatedCLEVRCollection> {

		@Override
		public SituatedCLEVRCollection create(ParameterizedExperiment.Parameters params,
											  IResourceRepository repo) {
			return CLEVRCollection.read(
					SituatedCLEVRCollection.class,
					params.getAsFile("scenesFile"),
					params.getAsFile("questionsFile"),
					params.getAsBoolean("shuffle", false),
                    params.getAsInteger("subsampleQuestions", 0));
		}

		@Override
		public String type() {
			return "data.clevr.situated";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					SituatedCLEVRCollection.class)
					.setDescription("Collection of CLEVR questions+scenes")
					.build();
		}
	}

}
