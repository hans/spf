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
package edu.mit.bcs.clevros.situated;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory;
import edu.cornell.cs.nlp.spf.base.hashvector.HashVectorFactory.Type;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.Lexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry.Origin;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoringServices;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.data.utils.IValidator;
import edu.cornell.cs.nlp.spf.explat.DistributedExperiment;
import edu.cornell.cs.nlp.spf.explat.Job;
import edu.cornell.cs.nlp.spf.explat.resources.ResourceCreatorRepository;
import edu.cornell.cs.nlp.spf.genlex.ccg.ILexiconGenerator;
import edu.cornell.cs.nlp.spf.learn.ILearner;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.spf.parser.ccg.model.*;
import edu.cornell.cs.nlp.utils.collections.ListUtils;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LogLevel;
import edu.cornell.cs.nlp.utils.log.Logger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import edu.mit.bcs.clevros.data.CLEVRAnswer;
import edu.mit.bcs.clevros.data.CLEVRScene;
import edu.mit.bcs.clevros.genlex.ccg.template.coarse.SituatedTemplateCoarseGenlex;
import edu.mit.bcs.clevros.situated.test.ExactMatchSituatedTestingStatistics;
import edu.mit.bcs.clevros.situated.test.SituatedTester;

public class SituatedCLEVRExperiment extends DistributedExperiment {
	public static final ILogger						LOG	= LoggerFactory
																.create(SituatedCLEVRExperiment.class);

	private final LogicalExpressionCategoryServices	categoryServices;

	private final BayesianLexicalEntryScorer bayesianScorer;

	public SituatedCLEVRExperiment(File initFile) throws IOException {
		this(initFile, Collections.<String, String> emptyMap(),
				new CLEVRResourceRepo());
	}

	public SituatedCLEVRExperiment(File initFile, Map<String, String> envParams,
								   ResourceCreatorRepository creatorRepo) throws IOException {
		super(initFile, envParams, creatorRepo);

		LogLevel.DEV.set();
		Logger.setSkipPrefix(true);

		// //////////////////////////////////////////
		// Get parameters
		// //////////////////////////////////////////
		final File typesFile = globalParams.getAsFile("types");
		// Seed lexicon used solely to generate LF templates; lexical entries are not stored.
		final List<File> templateSeedlex = globalParams.getAsFiles("templateSeedlex");
		// Seed lexicon which is retained in full in the model lexicon.
		final List<File> seedlexFiles = globalParams.getAsFiles("seedlex");

		// //////////////////////////////////////////
		// Use tree hash vector
		// //////////////////////////////////////////

		HashVectorFactory.DEFAULT = Type.FAST_TREE;

		// //////////////////////////////////////////
		// Init lambda calculus system.
		// //////////////////////////////////////////

		try {
			// Init the logical expression type system
			LogicLanguageServices
					.setInstance(new LogicLanguageServices.Builder(
							new TypeRepository(typesFile),
							new FlexibleTypeComparator())
							.setNumeralTypeName("i")
							.setUseOntology(true)
							.addConstantsToOntology(
									globalParams.getAsFiles("ont"))
							.closeOntology(true).build());

			storeResource(ONTOLOGY_RESOURCE,
					LogicLanguageServices.getOntology());

		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// DEV: simple ontology for testing
		Set<LogicalConstant> ontSimple = new HashSet<>();
		ontSimple.add(LogicalConstant.read("scene:<e,t>"));
		storeResource("ontology_simple", new Ontology(ontSimple, true));

		// //////////////////////////////////////////////////
		// Category services for logical expressions
		// //////////////////////////////////////////////////

		this.categoryServices = new LogicalExpressionCategoryServices(true);
		storeResource(CATEGORY_SERVICES_RESOURCE, categoryServices);

		// //////////////////////////////////////////////////
		// Lexical factoring services
		// //////////////////////////////////////////////////

		FactoringServices.set(new FactoringServices.Builder()
				.addConstant(LogicalConstant.read("exists:<<e,t>,t>"))
				.addConstant(LogicalConstant.read("the:<<e,t>,e>")).build());

		// //////////////////////////////////////////////////
		// Initial lexicon
		// //////////////////////////////////////////////////

		Lexicon<LogicalExpression> templateSeeds = readSeedLexicons(templateSeedlex);
		Lexicon<LogicalExpression> seedLexicon = readSeedLexicons(seedlexFiles);

		storeResource("seedLexicon", seedLexicon);

		// //////////////////////////////////////////////////
		// Read resources
		// //////////////////////////////////////////////////

		readResrouces();

		// //////////////////////////////////////////////////
		// Register genlex as a model listener
		// //////////////////////////////////////////////////

		SituatedTemplateCoarseGenlex genlex = get("genlex");
		Model<?, LogicalExpression> model = get("model");
		model.registerListener(genlex);
		// Use seed lexicon to generate LF templates.
		genlex.addTemplates(templateSeeds.toCollection().stream()
				.map(entry -> FactoringServices.factor(entry).getTemplate())
				.collect(Collectors.toList()));

		// //////////////////////////////////////////////////
		// Fetch custom Bayesian scorer.
		// //////////////////////////////////////////////////
		bayesianScorer = get("bayesianScorer");
		// Disable during e.g. model init.
		bayesianScorer.disable();

		// //////////////////////////////////////////////////
		// Create jobs
		// //////////////////////////////////////////////////

		for (final Parameters params : jobParams) {
			addJob(createJob(params));
		}

	}

	/**
	 * Read and semi-factor entries from seed lexicon files.
	 * @param files
	 * @return
	 */
	private Lexicon<LogicalExpression> readSeedLexicons(List<File> files) {
		final Lexicon<LogicalExpression> readLexicon = new Lexicon<LogicalExpression>();
		for (final File file : files) {
			readLexicon.addEntriesFromFile(file, categoryServices,
					Origin.FIXED_DOMAIN);
		}

		final Lexicon<LogicalExpression> semiFactored = new Lexicon<LogicalExpression>();
		for (final LexicalEntry<LogicalExpression> entry : readLexicon
				.toCollection()) {
			for (final FactoredLexicalEntry factoredEntry : FactoringServices
					.factor(entry, true, true, 2)) {
				semiFactored.add(FactoringServices.factor(factoredEntry));
			}
		}

		return semiFactored;
	}

	private Job createJob(Parameters params) throws FileNotFoundException {
		final String type = params.get("type");
		if (type.equals("train")) {
			return createTrainJob(params);
		} else if (type.equals("test")) {
			return createTestJob(params);
		} else if (type.equals("save")) {
			return createSaveJob(params);
		} else if (type.equals("log")) {
			return createModelLoggingJob(params);
		} else if ("init".equals(type)) {
			return createModelInitJob(params);
		} else {
			throw new RuntimeException("Unsupported job type: " + type);
		}
	}

	private Job createModelInitJob(Parameters params)
			throws FileNotFoundException {
		final Model<Sentence, LogicalExpression> model = get(params
				.get("model"));
		final List<IModelInit<Sentence, LogicalExpression>> modelInits = ListUtils
				.map(params.getSplit("init"),
						new ListUtils.Mapper<String, IModelInit<Sentence, LogicalExpression>>() {

							@Override
							public IModelInit<Sentence, LogicalExpression> process(
									String obj) {
								return get(obj);
							}
						});

		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {

			@Override
			protected void doJob() {
				for (final IModelInit<Sentence, LogicalExpression> modelInit : modelInits) {
					modelInit.init(model);
				}
			}
		};
	}

	private Job createModelLoggingJob(Parameters params)
			throws FileNotFoundException {
		final IModelImmutable<?, ?> model = get(params.get("model"));
		final ModelLogger modelLogger = get(params.get("logger"));
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {

			@Override
			protected void doJob() {
				modelLogger.log(model, getOutputStream());
			}
		};
	}

	private Job createSaveJob(final Parameters params)
			throws FileNotFoundException {
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {

			@SuppressWarnings("unchecked")
			@Override
			protected void doJob() {
				// Save the model to file.
				try {
					LOG.info("Saving model (id=%s) to: %s",
							params.get("model"), params.getAsFile("file")
									.getAbsolutePath());
					Model.write((Model<Sentence, LogicalExpression>) get(params
							.get("model")), params.getAsFile("file"));
				} catch (final IOException e) {
					LOG.error("Failed to save model to: %s", params.get("file"));
					throw new RuntimeException(e);
				}

			}
		};
	}

	private Job createTestJob(Parameters params) throws FileNotFoundException {

		final IValidator<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>, LogicalExpression> validator =
				get("validator");

		// Make the stats
		final ExactMatchSituatedTestingStatistics<SituatedSentence<CLEVRScene>, LogicalExpression, CLEVRAnswer, LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>> stats =
				new ExactMatchSituatedTestingStatistics<>(validator);

		// Get the tester
		final SituatedTester<CLEVRScene, LogicalExpression, CLEVRAnswer> tester = get(params
				.get("tester"));

		// The model to use
		final Model<SituatedSentence<CLEVRScene>, LogicalExpression> model = get(params
				.get("model"));

		// Create and return the job
		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {

			@Override
			protected void doJob() {

				// Record start time
				final long startTime = System.currentTimeMillis();

				// Job started
				LOG.info("============ (Job %s started)", getId());

				tester.test(model, stats);
				LOG.info("%s", stats);

				// Output total run time
				LOG.info("Total run time %.4f seconds",
						(System.currentTimeMillis() - startTime) / 1000.0);

				// Output machine readable stats
				getOutputStream().println(stats.toTabDelimitedString());

				// Job completed
				LOG.info("============ (Job %s completed)", getId());
			}
		};
	}

	@SuppressWarnings("unchecked")
	private Job createTrainJob(Parameters params) throws FileNotFoundException {
		// The model to use
		final Model<Sentence, LogicalExpression> model = (Model<Sentence, LogicalExpression>) get(params
				.get("model"));

		// The learning
		final ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>> learner = (ILearner<Sentence, SingleSentence, Model<Sentence, LogicalExpression>>) get(params
				.get("learner"));

		return new Job(params.get("id"), new HashSet<String>(
				params.getSplit("dep")), this,
				createJobOutputFile(params.get("id")),
				createJobLogFile(params.get("id"))) {

			@Override
			protected void doJob() {
				final long startTime = System.currentTimeMillis();

				// Start job
				LOG.info("============ (Job %s started)", getId());

				LOG.info("Enabling Bayesian scorer...");
				bayesianScorer.enable();

				// Do the learning
				learner.train(model);

				// Log the final model
				LOG.info("Final model:\n%s", model);

				// Output total run time
				LOG.info("Total run time %.4f seconds",
						(System.currentTimeMillis() - startTime) / 1000.0);

				// Job completed
				LOG.info("============ (Job %s completed)", getId());

			}
		};
	}

}
