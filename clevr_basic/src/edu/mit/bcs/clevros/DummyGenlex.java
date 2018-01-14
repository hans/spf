package edu.mit.bcs.clevros;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.AbstractLexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;

public class DummyGenlex<SAMPLE extends Sentence, DI extends ILabeledDataItem<SAMPLE, LogicalExpression>>
        extends AbstractLexiconGenerator<DI, LogicalExpression, IModelImmutable<Sentence, LogicalExpression>> {

    public DummyGenlex(String origin, boolean mark) {
        super(origin, mark);
    }

    @Override
    public ILexiconImmutable<LogicalExpression> generate(DI dataItem, IModelImmutable<Sentence, LogicalExpression> sentenceLogicalExpressionIModelImmutable, ICategoryServices<LogicalExpression> categoryServices) {
        return new Lexicon<>();
    }

    @Override
    public void init(IModelImmutable<Sentence, LogicalExpression> sentenceLogicalExpressionIModelImmutable) {
        // pass
    }

    @Override
    public boolean isGenerated(LexicalEntry<LogicalExpression> entry) {
        return false;
    }

    public static class Creator<SAMPLE extends Sentence, DI extends ILabeledDataItem<SAMPLE, LogicalExpression>>
            implements
            IResourceObjectCreator<DummyGenlex<SAMPLE, DI>> {

        private final String type;

        public Creator() {
            this("genlex.dummy");
        }

        public Creator(String type) {
            this.type = type;
        }

        @Override
        public DummyGenlex<SAMPLE, DI> create(ParameterizedExperiment.Parameters params,
                                              IResourceRepository repo) {
            return new DummyGenlex<>(
                    params.get("origin", ILexiconGenerator.GENLEX_LEXICAL_ORIGIN),
                    params.getAsBoolean("mark", false));
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return new ResourceUsage.Builder(type(),
                    DummyGenlex.class)
                    .build();
        }

    }

}
