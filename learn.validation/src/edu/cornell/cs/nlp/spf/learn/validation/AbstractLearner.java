/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.learn.validation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexiconImmutable;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.data.ILabeledDataItem;
import edu.cornell.cs.nlp.spf.data.collection.IDataCollection;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.genlex.ccg.LexiconGenerationServices;
import edu.cornell.cs.nlp.spf.learn.ILearner;
import edu.cornell.cs.nlp.spf.learn.LearningStats;
import edu.cornell.cs.nlp.spf.learn.validation.perceptron.ValidationPerceptron;
import edu.cornell.cs.nlp.spf.learn.validation.stocgrad.ValidationStocGrad;
import edu.cornell.cs.nlp.spf.parser.IDerivation;
import edu.cornell.cs.nlp.spf.parser.IOutputLogger;
import edu.cornell.cs.nlp.spf.parser.IParserOutput;
import edu.cornell.cs.nlp.spf.parser.ParsingOp;
import edu.cornell.cs.nlp.spf.parser.ccg.IWeightedParseStep;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IDataItemModel;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.spf.parser.filter.IParsingFilterFactory;
import edu.cornell.cs.nlp.utils.collections.CollectionUtils;
import edu.cornell.cs.nlp.utils.filter.IFilter;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.cornell.cs.nlp.utils.system.MemoryReport;

/**
 * Validation-based learner. See Artzi and Zettlemoyer 2013 for detailed
 * description. While the algorithm in the paper is situated, this one is not.
 * For a situated version see the package edu.uw.cs.lil.tiny.learn.situated.
 * <p>
 * The learner is insensitive to the syntactic category generated by the
 * inference procedure -- only the semantic portion is being validated. However,
 * parsers can be constrained to output only specific syntactic categories, see
 * the parser builders.
 * </p>
 * <p>
 * Parameter update step inspired by: Natasha Singh-Miller and Michael Collins.
 * 2007. Trigger-based Language Modeling using a Loss-sensitive Perceptron
 * Algorithm. In proceedings of ICASSP 2007.
 * </p>
 *
 * @author Yoav Artzi
 * @see ValidationPerceptron
 * @see ValidationStocGrad
 */
public abstract class AbstractLearner<SAMPLE extends IDataItem<?>, DI extends ILabeledDataItem<SAMPLE, ?>, PO extends IParserOutput<MR>, MR>
		implements ILearner<SAMPLE, DI, Model<SAMPLE, MR>> {
	public static final ILogger												LOG					= LoggerFactory
			.create(AbstractLearner.class);

	protected static final String											GOLD_LF_IS_MAX		= "G";
	protected static final String											HAS_VALID_LF		= "V";
	protected static final String											TRIGGERED_UPDATE	= "U";

	private final ICategoryServices<MR>										categoryServices;

	/**
	 * Recycle the lexical induction parser output as the pruned one for
	 * parameter update.
	 */
	private final boolean													conflateGenlexAndPrunedParses;

	/**
	 * Number of training epochs.
	 */
	private final int														epochs;

	/**
	 * Limit on number of training iterations.
	 */
	private final int                                                       maxIterations;

	/**
	 * The learner is error driven, meaning: if it can parse a sentence, it will
	 * skip lexical induction.
	 */
	private final boolean													errorDriven;

	/**
	 * GENLEX procedure. If 'null', skip lexicon learning.
	 */
	private final ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>>	genlex;

	/**
	 * Parser beam size for lexical generation.
	 */
	private final Integer													lexiconGenerationBeamSize;

	private final IParsingFilterFactory<DI, MR>								parsingFilterFactory;

	private final IFilter<DI>												processingFilter;

	/**
	 * Training data.
	 */
	private final IDataCollection<DI>										trainingData;

	/**
	 * Mapping of training data samples to their gold labels.
	 */
	private final Map<DI, MR>												trainingDataDebug;

	/**
	 * Parser output logger.
	 */
	protected final IOutputLogger<MR>										parserOutputLogger;

	/**
	 * Learning statistics.
	 */
	protected final LearningStats											stats;

	protected AbstractLearner(int epochs, int maxIterations,
			IDataCollection<DI> trainingData, Map<DI, MR> trainingDataDebug,
			int lexiconGenerationBeamSize, IOutputLogger<MR> parserOutputLogger,
			boolean conflateGenlexAndPrunedParses, boolean errorDriven,
			ICategoryServices<MR> categoryServices,
			ILexiconGenerator<DI, MR, IModelImmutable<SAMPLE, MR>> genlex,
			IFilter<DI> processingFilter,
			IParsingFilterFactory<DI, MR> parsingFilterFactory) {
		this.epochs = epochs;
		this.maxIterations = maxIterations;
		this.trainingData = trainingData;
		this.trainingDataDebug = trainingDataDebug;
		this.lexiconGenerationBeamSize = lexiconGenerationBeamSize;
		this.parserOutputLogger = parserOutputLogger;
		this.conflateGenlexAndPrunedParses = conflateGenlexAndPrunedParses;
		this.errorDriven = errorDriven;
		this.categoryServices = categoryServices;
		this.genlex = genlex;
		this.processingFilter = processingFilter;
		this.parsingFilterFactory = parsingFilterFactory;
		this.stats = new LearningStats.Builder(trainingData.size())
				.addStat(HAS_VALID_LF, "Has a valid parse")
				.addStat(TRIGGERED_UPDATE, "Sample triggered update")
				.addStat(GOLD_LF_IS_MAX,
						"The best-scoring LF equals the provided GOLD debug LF")
				.setNumberStat("Number of new lexical entries added").build();
	}

	@Override
	public void train(Model<SAMPLE, MR> model) {

		// Init GENLEX.
		LOG.info("Initializing GENLEX ...");
		genlex.init(model);

		int iterations = 0;

		// Epochs
		outer: for (int epochNumber = 0; epochNumber < epochs; ++epochNumber) {
			// Training epoch, iterate over all training samples
			LOG.info("=========================");
			LOG.info("Training epoch %d", epochNumber);
			LOG.info("=========================");
			int itemCounter = -1;

			// Iterating over training data
			for (final DI dataItem : trainingData) {
				// Process a single training sample

				// Record start time
				final long startTime = System.currentTimeMillis();

				// Log sample header
				LOG.info("%d : ================== [%d]", ++itemCounter,
						epochNumber);
				LOG.info("Sample type: %s",
						dataItem.getClass().getSimpleName());
				LOG.info("%s", dataItem);

				// Skip sample, if over the length limit
				if (!processingFilter.test(dataItem)) {
					LOG.info(
							"Skipped training sample, due to processing filter");
					continue;
				}

				stats.count("Processed", epochNumber);

				try {
					// Data item model
					final IDataItemModel<MR> dataItemModel = model
							.createDataItemModel(dataItem.getSample());

					// ///////////////////////////
					// Step I: Parse with current model. If we get a valid
					// parse, update parameters.
					// ///////////////////////////

					// Parse with current model and record some statistics
					final PO parserOutput = parse(dataItem, dataItemModel);
					stats.mean("Model parse",
							parserOutput.getParsingTime() / 1000.0, "sec");
					parserOutputLogger.log(parserOutput, dataItemModel, String
							.format("train-%d-%d", epochNumber, itemCounter));

					final List<? extends IDerivation<MR>> modelParses = parserOutput
							.getAllDerivations();

					LOG.info("Model parsing time: %.4fsec",
							parserOutput.getParsingTime() / 1000.0);
					LOG.info("Output is %s",
							parserOutput.isExact() ? "exact" : "approximate");
					LOG.info("Created %d model parses for training sample:",
							modelParses.size());
					for (final IDerivation<MR> parse : modelParses) {
						logParse(dataItem, parse,
								validate(dataItem, parse.getSemantics()), true,
								dataItemModel);
					}

					// Create a list of all valid parses
					final List<? extends IDerivation<MR>> validParses = getValidParses(
							parserOutput, dataItem);

					// If has a valid parse, call parameter update procedure
					// and continue
					if (!validParses.isEmpty() && errorDriven) {
						parameterUpdate(dataItem, parserOutput, parserOutput,
								model, itemCounter, epochNumber);
						continue;
					}

					// ///////////////////////////
					// Step II: Generate new lexical entries, prune and update
					// the model. Keep the parser output for Step III.
					// ///////////////////////////

					if (genlex == null) {
						// Skip the example if not doing lexicon learning
						continue;
					}

					final PO generationParserOutput = lexicalInduction(dataItem,
							itemCounter, dataItemModel, model, epochNumber);

					// ///////////////////////////
					// Step III: Update parameters
					// ///////////////////////////

					if (conflateGenlexAndPrunedParses
							&& generationParserOutput != null) {
						parameterUpdate(dataItem, parserOutput,
								generationParserOutput, model, itemCounter,
								epochNumber);
					} else {
						final PO prunedParserOutput = parse(dataItem,
								parsingFilterFactory.create(dataItem),
								dataItemModel);
						LOG.info("Conditioned parsing time: %.4fsec",
								prunedParserOutput.getParsingTime() / 1000.0);
						parserOutputLogger.log(prunedParserOutput,
								dataItemModel,
								String.format("train-%d-%d-conditioned",
										epochNumber, itemCounter));
						parameterUpdate(dataItem, parserOutput,
								prunedParserOutput, model, itemCounter,
								epochNumber);
					}

				} finally {
					// Record statistics.
					stats.mean("Sample processing",
							(System.currentTimeMillis() - startTime) / 1000.0,
							"sec");
					LOG.info("Total sample handling time: %.4fsec",
							(System.currentTimeMillis() - startTime) / 1000.0);
				}

                iterations++;
                if (iterations >= maxIterations) {
                    LOG.info("Reached max iteration count of %d. Ending.", iterations);
                    break outer;
                }
			}

			// Output epoch statistics
			LOG.info("System memory: %s", MemoryReport.generate());
			LOG.info("Epoch stats:");
			LOG.info(stats);
		}
	}

	private List<? extends IDerivation<MR>> getValidParses(PO parserOutput,
			final DI dataItem) {
		final List<? extends IDerivation<MR>> parses = new LinkedList<IDerivation<MR>>(
				parserOutput.getAllDerivations());

		// Use validation function to prune generation parses. Syntax is not
		// used to distinguish between derivations.
		CollectionUtils.filterInPlace(parses,
				e -> validate(dataItem, e.getSemantics()));
		return parses;
	}

	private PO lexicalInduction(final DI dataItem, int dataItemNumber,
			IDataItemModel<MR> dataItemModel, Model<SAMPLE, MR> model,
			int epochNumber) {
		// Generate lexical entries
		final ILexiconImmutable<MR> generatedLexicon = genlex.generate(dataItem,
				model, categoryServices);
		LOG.info("Generated lexicon size = %d", generatedLexicon.size());

		if (generatedLexicon.size() > 0) {
			// Case generated lexical entries

			// Create pruning filter, if the data item fits
			final Predicate<ParsingOp<MR>> pruningFilter = parsingFilterFactory
					.create(dataItem);

			// Parse with generated lexicon
			final PO parserOutput = parse(dataItem, pruningFilter,
					dataItemModel, generatedLexicon, lexiconGenerationBeamSize);

			// Log lexical generation parsing time
			stats.mean("genlex parse", parserOutput.getParsingTime() / 1000.0,
					"sec");
			LOG.info("Lexicon induction parsing time: %.4fsec",
					parserOutput.getParsingTime() / 1000.0);
			LOG.info("Output is %s",
					parserOutput.isExact() ? "exact" : "approximate");

			// Log generation parser output
			parserOutputLogger.log(parserOutput, dataItemModel, String
					.format("train-%d-%d-genlex", epochNumber, dataItemNumber));

			LOG.info("Created %d lexicon generation parses for training sample",
					parserOutput.getAllDerivations().size());

			// Get valid lexical generation parses
			final List<? extends IDerivation<MR>> validParses = getValidParses(
					parserOutput, dataItem);
			LOG.info("Removed %d invalid parses",
					parserOutput.getAllDerivations().size()
							- validParses.size());

			// Collect max scoring valid generation parses
			final List<IDerivation<MR>> bestGenerationParses = new LinkedList<IDerivation<MR>>();
			double currentMaxModelScore = -Double.MAX_VALUE;
			for (final IDerivation<MR> parse : validParses) {
				if (parse.getScore() > currentMaxModelScore) {
					currentMaxModelScore = parse.getScore();
					bestGenerationParses.clear();
					bestGenerationParses.add(parse);
				} else if (parse.getScore() == currentMaxModelScore) {
					bestGenerationParses.add(parse);
				}
			}
			LOG.info("%d valid best parses for lexical generation:",
					bestGenerationParses.size());
			for (final IDerivation<MR> parse : bestGenerationParses) {
				logParse(dataItem, parse, true, true, dataItemModel);
			}

			// Update the model's lexicon with generated lexical
			// entries from the max scoring valid generation parses
			int newLexicalEntries = 0;
			for (final IDerivation<MR> parse : bestGenerationParses) {
				for (final LexicalEntry<MR> entry : parse
						.getMaxLexicalEntries()) {
					if (genlex.isGenerated(entry)) {
						if (model.addLexEntry(
								LexiconGenerationServices.unmark(entry))) {
							++newLexicalEntries;
							LOG.info("Added LexicalEntry to model: %s [%s]",
									entry, model.getTheta().printValues(
											model.computeFeatures(entry)));
						}
						// Lexical generators might link related lexical
						// entries, so if we add the original one, we
						// should also add all its linked ones
						for (final LexicalEntry<MR> linkedEntry : entry
								.getLinkedEntries()) {
							if (model.addLexEntry(LexiconGenerationServices
									.unmark(linkedEntry))) {
								++newLexicalEntries;
								LOG.info(
										"Added (linked) LexicalEntry to model: %s [%s]",
										linkedEntry,
										model.getTheta().printValues(model
												.computeFeatures(linkedEntry)));
							}
						}
					}
				}
			}
			// Record statistics
			if (newLexicalEntries > 0) {
				stats.appendSampleStat(dataItemNumber, epochNumber,
						newLexicalEntries);
			}

			return parserOutput;
		} else {
			// Skip lexical induction
			LOG.info("Skipped GENLEX step. No generated lexical items.");
			return null;
		}
	}

	protected boolean isGoldDebugCorrect(DI dataItem, MR label) {
		if (trainingDataDebug.containsKey(dataItem)) {
			return trainingDataDebug.get(dataItem).equals(label);
		} else {
			return false;
		}
	}

	protected void logParse(DI dataItem, IDerivation<MR> parse, Boolean valid,
			boolean verbose, IDataItemModel<MR> dataItemModel) {
		logParse(dataItem, parse, valid, verbose, null, dataItemModel);
	}

	protected void logParse(DI dataItem, IDerivation<MR> parse, Boolean valid,
			boolean verbose, String tag, IDataItemModel<MR> dataItemModel) {
		final boolean isGold;
		if (isGoldDebugCorrect(dataItem, parse.getSemantics())) {
			isGold = true;
		} else {
			isGold = false;
		}
		LOG.info("%s%s[%.2f%s] %s", isGold ? "* " : "  ",
				tag == null ? "" : tag + " ", parse.getScore(),
				valid == null ? "" : valid ? ", V" : ", X", parse);
		if (verbose) {
			for (final IWeightedParseStep<MR> step : parse.getMaxSteps()) {
				LOG.info("\t%s",
						step.toString(false, false, dataItemModel.getTheta()));
			}
		}
	}

	/**
	 * Parameter update method.
	 */
	protected abstract void parameterUpdate(DI dataItem, PO realOutput,
			PO goodOutput, Model<SAMPLE, MR> model, int itemCounter,
			int epochNumber);

	/**
	 * Unconstrained parsing method.
	 */
	protected abstract PO parse(DI dataItem, IDataItemModel<MR> dataItemModel);

	/**
	 * Constrained parsing method.
	 */
	protected abstract PO parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter,
			IDataItemModel<MR> dataItemModel);

	/**
	 * Constrained parsing method for lexical generation.
	 */
	protected abstract PO parse(DI dataItem,
			Predicate<ParsingOp<MR>> pruningFilter,
			IDataItemModel<MR> dataItemModel,
			ILexiconImmutable<MR> generatedLexicon, Integer beamSize);

	/**
	 * Validation method.
	 */
	abstract protected boolean validate(DI dataItem, MR hypothesis);
}
