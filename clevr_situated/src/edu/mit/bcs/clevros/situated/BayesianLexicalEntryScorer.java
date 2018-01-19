package edu.mit.bcs.clevros.situated;

import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.ccg.lexicon.factored.lambda.Lexeme;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.factoredlex.features.scorers.LexicalEntryLexemeBasedScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.AbstractScaledScorerCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.UniformScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A scorer which uses a Bayesian model to compute prior weights
 * (initial weights) for lexical induction results. It is deeply
 * coupled with its associated model and lexicon.
 *
 * When not associated with a model/lexicon or when associated with
 * a model/lexicon which is not prepared, defaults to another Scorer
 * instance.
 */
public class BayesianLexicalEntryScorer implements ISerializableScorer<LexicalEntry<LogicalExpression>> {

    private static final String SCRIPT_PATH = "run_wppl";
    private static final String SCORER_PATH = "syntaxGuidedScorer.wppl";
    private File scorerFile;

    private static final String QUERY_CODE =
                    "var qAttr = sample(attr);" +
                    "var qAttrVal = sample(attrVal);" +
                    "observe(term(qAttrVal), %s);" +
                    "observe(syntax(qAttr), %s);" +
                    "return {attr: qAttr, attrVal: qAttrVal}";
    private static final List<String> QUERY_VARS = Arrays.asList("attr", "attrVal");
    private static final Set<String> QUERY_VARS_SET = new HashSet<>(QUERY_VARS);

    private final ILexicon<LogicalExpression> lexicon;
    private final Model model;

    private final IScorer<LexicalEntry<LogicalExpression>> defaultScorer;

    public BayesianLexicalEntryScorer(ILexicon<LogicalExpression> lexicon, Model model,
                                      IScorer<LexicalEntry<LogicalExpression>> defaultScorer) {
        this.lexicon = lexicon;
        this.model = model;
        this.defaultScorer = defaultScorer;

        scorerFile = new File(SCORER_PATH);
        if (!scorerFile.exists())
            throw new RuntimeException("cannot find scorer file at " + SCORER_PATH);
    }

    private JSONArray runScorer() {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{SCRIPT_PATH, SCORER_PATH});
            BufferedReader outReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            JSONArray scores = (JSONArray) new JSONParser().parse(outReader);
            return scores;
        } catch (IOException e) {
            // TODO
            return null;
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    private Map<List<String>, Double> getScores() {
        JSONArray scores = runScorer();
        Map<List<String>, Double> ret = new HashMap<>();

        for (Object scoreEl : scores) {
            JSONArray scoreTuple = (JSONArray) scoreEl;
            JSONObject supportEl = (JSONObject) scoreTuple.get(0);
            double score = (double) scoreTuple.get(1);

            if (!supportEl.keySet().equals(QUERY_VARS_SET))
                throw new RuntimeException("did not receive expected query data");

            List<String> values = QUERY_VARS.stream().map((var) -> (String) supportEl.get(var))
                    .collect(Collectors.toList());
            ret.put(Collections.unmodifiableList(values), score);
        }

        return ret;
    }

    private Map<String, Counter<String>> getMarginalizedScores() {
        Map<List<String>, Double> fullTable = getScores();
        Map<String, Counter<String>> ret = new HashMap<>();

        for (int i = 0; i < QUERY_VARS.size(); i++) {
            String var = QUERY_VARS.get(i);
            final int idx = i;
            Set<String> support = fullTable.keySet().stream().map((tuple) -> tuple.get(idx))
                    .collect(Collectors.toSet());

            Counter<String> marginalized = new Counter<>();
            for (Map.Entry<List<String>, Double> entry : fullTable.entrySet()) {
                marginalized.addTo(entry.getKey().get(i), entry.getValue());
            }

            marginalized.normalize();
            ret.put(var, marginalized);
        }

        return ret;
    }

    @Override
    public double score(LexicalEntry<LogicalExpression> entry) {
        // TODO
        return defaultScorer.score(entry);
    }

    public static void main(String[] args) {
        BayesianLexicalEntryScorer s = new BayesianLexicalEntryScorer(null, null, new UniformScorer<>(0.0));
        s.getMarginalizedScores();
    }

    public static class Creator
            extends
            AbstractScaledScorerCreator<LexicalEntry<LogicalExpression>, BayesianLexicalEntryScorer> {

        @SuppressWarnings("unchecked")
        @Override
        public BayesianLexicalEntryScorer createScorer(
                ParameterizedExperiment.Parameters parameters, IResourceRepository resourceRepo) {
            IScorer<LexicalEntry<LogicalExpression>> defaultScorer = parameters.contains("defaultScorer")
                    ? resourceRepo.get(parameters.get("defaultScorer"))
                    : new UniformScorer<>(0.0);

            return new BayesianLexicalEntryScorer(resourceRepo, parameters.get("lexicon"),
                    parameters.get("model"), defaultScorer);
        }

        @Override
        public String type() {
            return "scorer.bayesian";
        }

        @Override
        public ResourceUsage usage() {
            return new ResourceUsage.Builder(type(),
                    BayesianLexicalEntryScorer.class)
                    .addParam("scale", "double", "Scaling factor")
                    .addParam("defaultScorer", "id", "Default scorer")
                    .addParam("lexicon", "id", "")
                    .addParam("model", "id", "")
                    .build();
        }

    }

}
