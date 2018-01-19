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

import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.FactoredLexicon;
import edu.cornell.cs.nlp.spf.data.collection.CompositeDataCollection;
import edu.cornell.cs.nlp.spf.data.sentence.Sentence;
import edu.cornell.cs.nlp.spf.data.sentence.SentenceLengthFilter;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentence;
import edu.cornell.cs.nlp.spf.data.singlesentence.SingleSentenceCollection;
import edu.cornell.cs.nlp.spf.data.situated.labeled.LabeledSituatedSentence;
import edu.cornell.cs.nlp.spf.data.situated.sentence.SituatedSentence;
import edu.cornell.cs.nlp.spf.explat.resources.ResourceCreatorRepository;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.UnificationGenlex;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.UnificationModelInit;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.Splitter;
import edu.cornell.cs.nlp.spf.learn.validation.perceptron.ValidationPerceptron;
import edu.cornell.cs.nlp.spf.learn.validation.stocgrad.ValidationStocGrad;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.SimpleFullParseFilter;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.multi.MultiCKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.cky.single.CKYParser;
import edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.FactoredLexicalFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.scorers.LexemeCooccurrenceScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.DynamicWordSkippingFeatures;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.LexicalFeaturesInit;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.RuleUsageFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.ExpLengthLexicalEntryScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.SkippingSensitiveLexicalEntryScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.UniformScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.features.lambda.LogicalExpressionCoordinationFeatureSet;
import edu.cornell.cs.nlp.spf.parser.ccg.lambda.pruning.SupervisedFilterFactory;
import edu.cornell.cs.nlp.spf.parser.ccg.model.LexiconModelInit;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.spf.parser.ccg.model.ModelLogger;
import edu.cornell.cs.nlp.spf.parser.ccg.model.WeightInit;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.eisner.EisnerNormalFormCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.unaryconstraint.UnaryConstraint;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.PluralExistentialTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.ThatlessRelative;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeraising.ForwardTypeRaisedComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.typeshifting.PrepositionTypeShifting;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ApplicationCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.CompositionCreator;
import edu.mit.bcs.clevros.data.CLEVRAnswer;
import edu.mit.bcs.clevros.situated.data.CurriculumDataCollection;
import edu.mit.bcs.clevros.situated.data.SituatedCLEVRCollection;
import edu.mit.bcs.clevros.data.CLEVRScene;
import edu.mit.bcs.clevros.situated.data.CLEVRValidator;
import edu.mit.bcs.clevros.genlex.ccg.template.coarse.SituatedTemplateCoarseGenlex;
import edu.mit.bcs.clevros.situated.test.SituatedTester;

public class CLEVRResourceRepo extends ResourceCreatorRepository {
	public CLEVRResourceRepo() {
		// Parser creators
		registerResourceCreator(new ApplicationCreator<LogicalExpression>());
		registerResourceCreator(new CompositionCreator<LogicalExpression>());
		registerResourceCreator(new PrepositionTypeShifting.Creator());
		registerResourceCreator(new ForwardTypeRaisedComposition.Creator());
		registerResourceCreator(new ThatlessRelative.Creator());
		registerResourceCreator(new PluralExistentialTypeShifting.Creator());
		registerResourceCreator(
				new MultiCKYParser.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new CKYParser.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new SimpleFullParseFilter.Creator());
		registerResourceCreator(
				new ExpLengthLexicalEntryScorer.Creator<LogicalExpression>());
		registerResourceCreator(
				new LexicalFeaturesInit.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(
				new CompositeDataCollection.Creator<SingleSentence>());
		registerResourceCreator(
				new Model.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(new ModelLogger.Creator());
		registerResourceCreator(new UniformScorer.Creator<LogicalExpression>());
		registerResourceCreator(
				new FactoredLexicalFeatureSet.Creator<Sentence>());
		registerResourceCreator(
				new SkippingSensitiveLexicalEntryScorer.Creator<LogicalExpression>());
		registerResourceCreator(new BayesianLexicalEntryScorer.Creator());
		registerResourceCreator(
				new LogicalExpressionCoordinationFeatureSet.Creator<Sentence>());
		registerResourceCreator(new FactoredLexicon.Creator());
		registerResourceCreator(new SituatedTemplateCoarseGenlex.Creator<SituatedSentence<CLEVRScene>, LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>>());
		registerResourceCreator(new SingleSentenceCollection.Creator());
		registerResourceCreator(new SituatedCLEVRCollection.Creator());
		registerResourceCreator(new CurriculumDataCollection.Creator<LabeledSituatedSentence<CLEVRScene, CLEVRAnswer>>());
		registerResourceCreator(
				new ValidationPerceptron.Creator<Sentence, SingleSentence, LogicalExpression>());
		registerResourceCreator(
				new ValidationStocGrad.Creator<Sentence, SingleSentence, LogicalExpression>());
		registerResourceCreator(
				new CLEVRValidator.Creator<>());
		registerResourceCreator(
				new SituatedTester.Creator<SituatedSentence<CLEVRScene>, LogicalExpression, CLEVRAnswer>());
		registerResourceCreator(
				new LexiconModelInit.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(
				new UnificationGenlex.Creator<Sentence, SingleSentence>());
		registerResourceCreator(new Splitter.Creator());
		registerResourceCreator(new UnificationModelInit.Creator());
		registerResourceCreator(new LexemeCooccurrenceScorer.Creator());
		registerResourceCreator(
				new SentenceLengthFilter.Creator<SingleSentence>());
		registerResourceCreator(new EisnerNormalFormCreator());
		registerResourceCreator(new UnaryConstraint.Creator());
		registerResourceCreator(
				new RuleUsageFeatureSet.Creator<Sentence, LogicalExpression>());
		registerResourceCreator(
				new SupervisedFilterFactory.Creator<SingleSentence>());
		registerResourceCreator(new DynamicWordSkippingFeatures.Creator<>());
		registerResourceCreator(new WeightInit.Creator<>());
	}
}
