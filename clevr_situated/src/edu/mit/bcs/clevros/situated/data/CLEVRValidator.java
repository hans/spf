package edu.mit.bcs.clevros.situated.data;

import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.mit.bcs.clevros.data.CLEVRAnswer;
import edu.mit.bcs.clevros.data.CLEVRScene;

public class CLEVRValidator<DI extends LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>,
		                    LABEL extends LogicalExpression>
		implements IValidator<DI, LABEL> {

	@Override
	public boolean isValid(DI dataItem, LABEL label) {
	    final CLEVRAnswer ans;
	    final CLEVRScene scene = dataItem.getSample().getState();
        TypeRepository repo = LogicLanguageServices.getTypeRepository();

        // TODO support other answer types!
	    if (label.getType().equals(repo.getTruthValueType())) {
            ans = new CLEVRAnswer(label.equals(LogicLanguageServices.getTrue()));
	    } else {
	    	try {
				CLEVRAnswer evaluated = scene.evaluate(label);
				return dataItem.getLabel().equals(evaluated);
			} catch (RuntimeException e) {
				return false;
			}
			//throw new IllegalArgumentException("Unhandled parser output " + label.toString());
	    }

		return dataItem.getLabel().equals(ans);
	}
	
	public static class Creator<DI extends LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>,
                                LABEL extends LogicalExpression>
			implements IResourceObjectCreator<CLEVRValidator<DI, LABEL>> {
		
		private String	type;
		
		public Creator() {
			this("validator.clevr");
		}
		
		public Creator(String type) {
			this.type = type;
		}
		
		@Override
		public CLEVRValidator<DI, LABEL> create(Parameters params,
												IResourceRepository repo) {
			return new CLEVRValidator<>();
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), CLEVRValidator.class)
					.build();
		}
		
	}
	
}
