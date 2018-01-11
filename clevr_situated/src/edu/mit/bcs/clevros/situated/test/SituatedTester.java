package edu.mit.bcs.clevros.situated.test;

import edu.cornell.cs.nlp.spf.base.hashvector.IHashVector;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IOutputLogger;
import edu.cornell.cs.nlp.spf.parser.IParser;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ccg.IWeightedParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.test.Tester;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.util.List;

public class SituatedTester<STATE, MR, ANSWER>
        implements ISituatedTester<SituatedSentence<STATE>, MR, ANSWER, LabeledSituatedSentence<STATE, ANSWER>> {

    public static final ILogger LOG = LoggerFactory.create(SituatedTester.class.getName());

    private final IOutputLogger<MR> outputLogger;

    private final IParser<SituatedSentence<STATE>, MR> parser;

    private final IValidator<LabeledSituatedSentence<STATE, ANSWER>, MR> validator;

    private final IFilter<SituatedSentence<STATE>> skipParsingFilter;

    private final IDataCollection<? extends LabeledSituatedSentence<STATE, ANSWER>> testData;

    private SituatedTester(IDataCollection<? extends LabeledSituatedSentence<STATE, ANSWER>> testData,
                           IFilter<SituatedSentence<STATE>> skipParsingFilter, IParser<SituatedSentence<STATE>, MR> parser,
                           IValidator<LabeledSituatedSentence<STATE, ANSWER>, MR> validator,
                           IOutputLogger<MR> outputLogger) {
        this.testData = testData;
        this.skipParsingFilter = skipParsingFilter;
        this.parser = parser;
        this.validator = validator;
        this.outputLogger = outputLogger;
        LOG.info("Init Tester:  testData.size()=%d", testData.size());
    }


    @Override
    public void test(IModelImmutable<SituatedSentence<STATE>, MR> model,
                     ISituatedTestingStatistics<SituatedSentence<STATE>, MR, ANSWER, LabeledSituatedSentence<STATE, ANSWER>> stats) {
        test(testData, model, stats);
    }

    private void logDerivation(IDerivation<MR> derivation,
                               IDataItemModel<MR> dataItemModel) {
        LOG.info("[%.2f] %s", derivation.getScore(), derivation);
        for (final IWeightedParseStep<MR> step : derivation.getMaxSteps()) {
            LOG.info("\t%s",
                    step.toString(false, false, dataItemModel.getTheta()));
        }
    }

    private void logParse(LabeledSituatedSentence<STATE, ANSWER> dataItem,
                          IDerivation<MR> parse, boolean logLexicalItems, String tag,
                          IModelImmutable<SituatedSentence<STATE>, MR> model) {
        boolean valid = validator.isValid(dataItem, parse.getSemantics());
        LOG.info("%s%s[S%.2f] %s",
                valid ? "* " : "  ",
                tag == null ? "" : tag + " ", parse.getScore(), parse);
        LOG.info("Calculated score: %f",
                model.score(parse.getAverageMaxFeatureVector()));
        LOG.info("Features: %s", model.getTheta()
                .printValues(parse.getAverageMaxFeatureVector()));
        if (logLexicalItems) {
            for (final LexicalEntry<MR> entry : parse.getMaxLexicalEntries()) {
                LOG.info("\t[%f] %s", model.score(entry), entry);
            }
        }
    }

    private void processSingleBestParse(final LabeledSituatedSentence<STATE, ANSWER> dataItem,
                                        IDataItemModel<MR> dataItemModel,
                                        final IParserOutput<MR> modelParserOutput,
                                        final IDerivation<MR> parse, boolean withWordSkipping,
                                        ISituatedTestingStatistics<SituatedSentence<STATE>, MR, ANSWER, LabeledSituatedSentence<STATE, ANSWER>> stats) {
        final MR label = parse.getSemantics();

        // Update statistics
        if (withWordSkipping) {
            stats.recordParseWithSkipping(dataItem, label);
        } else {
            stats.recordParse(dataItem, label);
        }

        if (validator.isValid(dataItem, label)) {
            // A correct parse
            LOG.info("CORRECT");
            logDerivation(parse, dataItemModel);
        } else {
            // One parse, but a wrong one
            LOG.info("WRONG", label);
            logDerivation(parse, dataItemModel);

            // Check if we had the correct parse and it just wasn't the best
            final List<? extends IDerivation<MR>> correctParses = modelParserOutput
                    .getMaxDerivations(
                            e -> dataItem.getLabel().equals(e.getSemantics()));
            LOG.info("Had correct parses: %s", !correctParses.isEmpty());
            if (!correctParses.isEmpty()) {
                for (final IDerivation<MR> correctParse : correctParses) {
                    LOG.info("Correct derivation:");
                    logDerivation(correctParse, dataItemModel);
                    final IHashVector diff = correctParse
                            .getAverageMaxFeatureVector()
                            .addTimes(-1.0, parse.getAverageMaxFeatureVector());
                    diff.dropNoise();
                    LOG.info("Diff: %s",
                            dataItemModel.getTheta().printValues(diff));
                }
            }
            LOG.info("Feats: %s", dataItemModel.getTheta()
                    .printValues(parse.getAverageMaxFeatureVector()));
        }
    }

    private void test(IDataCollection<? extends LabeledSituatedSentence<STATE, ANSWER>> dataset,
                      IModelImmutable<SituatedSentence<STATE>, MR> model,
                      ISituatedTestingStatistics<SituatedSentence<STATE>, MR, ANSWER, LabeledSituatedSentence<STATE, ANSWER>> stats) {
        int itemCounter = 0;
        for (final LabeledSituatedSentence<STATE, ANSWER> item : dataset) {
            ++itemCounter;
            test(itemCounter, item, model, stats);
        }
    }

    private void test(int itemCounter, final LabeledSituatedSentence<STATE, ANSWER> dataItem,
                      IModelImmutable<SituatedSentence<STATE>, MR> model,
                      ISituatedTestingStatistics<SituatedSentence<STATE>, MR, ANSWER, LabeledSituatedSentence<STATE, ANSWER>> stats) {
        LOG.info("%d : ==================", itemCounter);
        LOG.info("%s", dataItem);

        final IDataItemModel<MR> dataItemModel = model
                .createDataItemModel(dataItem.getSample());

        // Try a simple model parse
        final IParserOutput<MR> modelParserOutput = parser
                .parse(dataItem.getSample(), dataItemModel);
        LOG.info("Test parsing time %.2fsec",
                modelParserOutput.getParsingTime() / 1000.0);
        outputLogger.log(modelParserOutput, dataItemModel,
                String.format("test-%d", itemCounter));

        final List<? extends IDerivation<MR>> bestModelParses = modelParserOutput
                .getBestDerivations();
        if (bestModelParses.size() == 1) {
            // Case we have a single parse
            processSingleBestParse(dataItem, dataItemModel, modelParserOutput,
                    bestModelParses.get(0), false, stats);
        } else if (bestModelParses.size() > 1) {
            // Multiple top parses

            // Update statistics
            stats.recordParses(dataItem,
                    ListUtils.map(bestModelParses, IDerivation::getSemantics));

            // There are more than one equally high scoring
            // logical forms. If this is the case, we abstain
            // from returning a result.
            LOG.info("too many parses");
            LOG.info("%d parses:", bestModelParses.size());
            for (final IDerivation<MR> parse : bestModelParses) {
                logParse(dataItem, parse, false, null, model);
            }
            // Check if we had the correct parse and it just wasn't the best
            final List<? extends IDerivation<MR>> correctParses = modelParserOutput
                    .getMaxDerivations(
                            e -> dataItem.getLabel().equals(e.getSemantics()));

            LOG.info("Had correct parses: %s", !correctParses.isEmpty());
            if (!correctParses.isEmpty()) {
                for (final IDerivation<MR> correctParse : correctParses) {
                    logDerivation(correctParse, dataItemModel);
                }
            }
        } else {
            // No parses
            LOG.info("no parses");

            // Update stats
            stats.recordNoParse(dataItem);

            // Potentially re-parse with word skipping
            if (skipParsingFilter.test(dataItem.getSample())) {
                final IParserOutput<MR> parserOutputWithSkipping = parser
                        .parse(dataItem.getSample(), dataItemModel, true);
                LOG.info("EMPTY Parsing time %fsec",
                        parserOutputWithSkipping.getParsingTime() / 1000.0);
                outputLogger.log(parserOutputWithSkipping, dataItemModel,
                        String.format("test-%d-sloppy", itemCounter));
                final List<? extends IDerivation<MR>> bestEmptiesParses = parserOutputWithSkipping
                        .getBestDerivations();

                if (bestEmptiesParses.size() == 1) {
                    processSingleBestParse(dataItem, dataItemModel,
                            parserOutputWithSkipping, bestEmptiesParses.get(0),
                            true, stats);
                } else if (bestEmptiesParses.isEmpty()) {
                    // No parses
                    LOG.info("no parses");

                    stats.recordNoParseWithSkipping(dataItem);
                } else {
                    // too many parses or no parses
                    stats.recordParsesWithSkipping(dataItem, ListUtils
                            .map(bestEmptiesParses, obj -> obj.getSemantics()));

                    LOG.info("WRONG: %d parses", bestEmptiesParses.size());
                    for (final IDerivation<MR> parse : bestEmptiesParses) {
                        logParse(dataItem, parse, false, null, model);
                    }
                    // Check if we had the correct parse and it just wasn't
                    // the best
                    final List<? extends IDerivation<MR>> correctParses = parserOutputWithSkipping
                            .getMaxDerivations(e -> dataItem.getLabel()
                                    .equals(e.getSemantics()));
                    LOG.info("Had correct parses: %s",
                            !correctParses.isEmpty());
                    if (!correctParses.isEmpty()) {
                        for (final IDerivation<MR> correctParse : correctParses) {
                            logDerivation(correctParse, dataItemModel);
                        }
                    }
                }
            } else {
                LOG.info("Skipping word-skip parsing due to length");
                stats.recordNoParseWithSkipping(dataItem);
            }
        }
    }

    public static class Builder<STATE, MR, ANSWER> {

        private IOutputLogger<MR> outputLogger = new IOutputLogger<MR>() {
            private static final long serialVersionUID = -2828347737693835555L;

            @Override
            public void log(IParserOutput<MR> output,
                            IDataItemModel<MR> dataItemModel, String tag) {
                // Stub.
            }
        };

        private final IParser<SituatedSentence<STATE>, MR> parser;

        private final IValidator<LabeledSituatedSentence<STATE, ANSWER>, MR> validator;

        /** Filters which data items are valid for parsing with word skipping */
        private IFilter<SituatedSentence<STATE>> skipParsingFilter = e -> true;

        private final IDataCollection<? extends LabeledSituatedSentence<STATE, ANSWER>> testData;

        public Builder(IDataCollection<? extends LabeledSituatedSentence<STATE, ANSWER>> testData,
                       IParser<SituatedSentence<STATE>, MR> parser,
                       IValidator<LabeledSituatedSentence<STATE, ANSWER>, MR> validator) {
            this.testData = testData;
            this.parser = parser;
            this.validator = validator;
        }

        public SituatedTester<STATE, MR, ANSWER> build() {
            return new SituatedTester<>(testData, skipParsingFilter,
                    parser, validator, outputLogger);
        }

        public SituatedTester.Builder<STATE, MR, ANSWER> setOutputLogger(
                IOutputLogger<MR> outputLogger) {
            this.outputLogger = outputLogger;
            return this;
        }

        public SituatedTester.Builder<STATE, MR, ANSWER> setSkipParsingFilter(
                IFilter<SituatedSentence<STATE>> skipParsingFilter) {
            this.skipParsingFilter = skipParsingFilter;
            return this;
        }
    }

    public static class Creator<STATE, MR, ANSWER>
            implements IResourceObjectCreator<SituatedTester<STATE, MR, ANSWER>> {

        @SuppressWarnings("unchecked")
        @Override
        public SituatedTester<STATE, MR, ANSWER> create(ParameterizedExperiment.Parameters parameters,
                                                IResourceRepository resourceRepo) {

            // Get the testing set
            final IDataCollection<LabeledSituatedSentence<STATE, ANSWER>> testSet;
            {
                // [yoav] [17/10/2011] Store in Object to javac known bug
                final Object dataCollection = resourceRepo
                        .get(parameters.get("data"));
                if (dataCollection == null
                        || !(dataCollection instanceof IDataCollection<?>)) {
                    throw new RuntimeException(
                            "Unknown or non labeled dataset: "
                                    + parameters.get("data"));
                } else {
                    testSet = (IDataCollection<LabeledSituatedSentence<STATE, ANSWER>>) dataCollection;
                }
            }

            if (!parameters.contains("parser")) {
                throw new IllegalStateException(
                        "tester now requires you to provide a parser");
            }

            if (!parameters.contains("validator")) {
                throw new IllegalStateException("situated tester requires a validator");
            }

            final SituatedTester.Builder<STATE, MR, ANSWER> builder = new SituatedTester.Builder<>(
                    testSet,
                    (IParser<SituatedSentence<STATE>, MR>) resourceRepo.get(parameters.get("parser")),
                    (IValidator<LabeledSituatedSentence<STATE, ANSWER>, MR>) resourceRepo.get(parameters.get("validator")));

            if (parameters.get("skippingFilter") != null) {
                builder.setSkipParsingFilter((IFilter<SituatedSentence<STATE>>) resourceRepo
                        .get(parameters.get("skippingFilter")));
            }

            return builder.build();
        }

        @Override
        public String type() {
            return "tester.situated";
        }

        @Override
        public ResourceUsage usage() {
            return new ResourceUsage.Builder(type(), Tester.class)
                    .setDescription(
                            "Situated model tester. Tests inference using the model on some distantly supervised testing data")
                    .addParam("data", "id",
                            "IDataCollection that holds ILabaledDataItem entries")
                    .addParam("parser", "id", "Parser object")
                    .addParam("validator", "id", "Validator object")
                    .addParam("skippingFilter", "id",
                            "IFilter used to decide which data items to skip")
                    .build();
        }

    }

}
