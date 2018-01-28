package edu.mit.bcs.clevros;

import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.learn.ILearnerListener;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.mit.bcs.clevros.data.CLEVRAnswer;
import edu.mit.bcs.clevros.data.CLEVRScene;
import edu.mit.bcs.clevros.situated.test.ExactMatchSituatedTestingStatistics;
import edu.mit.bcs.clevros.situated.test.SituatedTester;

public class OnlineTester implements ILearnerListener {

    private static final ILogger LOG = LoggerFactory.create(OnlineTester.class);

    private final IValidator<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>, LogicalExpression> validator;
    private ExactMatchSituatedTestingStatistics<SituatedSentence<CLEVRScene>, LogicalExpression, CLEVRAnswer, LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> stats;
    private final SituatedTester<CLEVRScene, LogicalExpression, CLEVRAnswer> tester;
    private final Model<SituatedSentence<CLEVRScene>, LogicalExpression> model;

    public OnlineTester(
            IValidator<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>, LogicalExpression> validator,
            SituatedTester<CLEVRScene, LogicalExpression, CLEVRAnswer> tester,
            Model<SituatedSentence<CLEVRScene>, LogicalExpression> model) {
        this.validator = validator;
        this.tester = tester;
        this.model = model;

        reset();
    }

    private void reset() {
        this.stats = new ExactMatchSituatedTestingStatistics<>(validator);
    }

    @Override
    public void finishedDataItem(IDataItem<?> dataItem) {
        // HACK: disable logging during this testing
        tester.LOG.setCustomLevel(LogLevel.NO_LOG);
        tester.test(model, stats);
        tester.LOG.clearCustomLevel();

        LOG.info("Online test results: \n%s", stats.toTabDelimitedString());
    }
}
