package edu.mit.bcs.clevros.data;

import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.Simplify;
import org.json.simple.JSONObject;

import java.util.List;

public class BasicCLEVRCollection extends CLEVRCollection<SingleSentence> {

	private static final String SEXPR_KEY = "program_sexpr";

    public BasicCLEVRCollection(boolean shuffle) {
        super(shuffle);
    }

    public BasicCLEVRCollection(List<SingleSentence> entries, boolean shuffle) {
		super(entries, shuffle);
	}

	@Override
	protected SingleSentence readQuestionJSON(JSONObject questionData, CLEVRScene scene) {
		if (!questionData.containsKey(SEXPR_KEY)) {
			throw new IllegalArgumentException("Question data is missing logical form sexpr");
		}

		String questionStr = ((String) questionData.get("question")).toLowerCase();
		Sentence sentence = new Sentence(questionStr, new CLEVRTokenizer());

		String sexprStr = (String) questionData.get(SEXPR_KEY);
		LogicalExpression label = Simplify.of(LogicalExpression.read(sexprStr));

		return new SingleSentence(sentence, label);
	}

	public static class Creator
		implements IResourceObjectCreator<BasicCLEVRCollection> {

		@Override
		public BasicCLEVRCollection create(ParameterizedExperiment.Parameters params,
										   IResourceRepository repo) {
			return CLEVRCollection.read(
					BasicCLEVRCollection.class,
					params.getAsFile("scenesFile"),
					params.getAsFile("questionsFile"),
					params.getAsBoolean("shuffle", false),
                    params.getAsInteger("subsampleQuestions", 0));
		}

		@Override
		public String type() {
			return "data.clevr.basic";
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(),
					BasicCLEVRCollection.class)
					.setDescription("Collection of CLEVR questions+scenes")
					.build();
		}
	}

}
