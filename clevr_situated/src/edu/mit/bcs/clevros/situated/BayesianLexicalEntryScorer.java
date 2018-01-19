package edu.mit.bcs.clevros.situated;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.AbstractScaledScorerCreator;
import edu.cornell.cs.nlp.spf.parser.ccg.features.basic.scorer.UniformScorer;
import edu.cornell.cs.nlp.spf.parser.ccg.model.IModelImmutable;
import edu.cornell.cs.nlp.spf.parser.ccg.model.Model;
import edu.cornell.cs.nlp.utils.collections.IScorer;
import edu.cornell.cs.nlp.utils.collections.ISerializableScorer;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.mit.bcs.clevros.GetFilterArguments;
import edu.mit.bcs.clevros.data.CLEVRTypes;
import edu.mit.bcs.clevros.util.Counter;
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

    private static final List<String> ATTRIBUTES = Arrays.asList("shape", "color");
    private static final Map<String, List<String>> ATTRIBUTE_VALUES = new HashMap<>();
    static {
        ATTRIBUTE_VALUES.put("shape", Arrays.asList(CLEVRTypes.SHAPES));
        ATTRIBUTE_VALUES.put("color", Arrays.asList(CLEVRTypes.COLORS));
    }

    private ILexicon<LogicalExpression> lexicon;
    private IModelImmutable<?, LogicalExpression> model;
    private String lexiconId;
    private String modelId;

    private final IResourceRepository repo;

    private final IScorer<LexicalEntry<LogicalExpression>> defaultScorer;
    private boolean alwaysDefault = false;

    public BayesianLexicalEntryScorer(ILexicon<LogicalExpression> lexicon, Model model,
                                      IScorer<LexicalEntry<LogicalExpression>> defaultScorer) {
        this.repo = null;
        this.lexicon = lexicon;
        this.model = model;
        this.defaultScorer = defaultScorer;

        checkScorer();
    }

    /**
     * HACK: when loading from experiment files, this class may be instantiated before lexicon, model are ready for
     * access. Store their IDs instead and lazy-load them.
     */
    public BayesianLexicalEntryScorer(IResourceRepository repo, String lexiconId, String modelId,
                                      IScorer<LexicalEntry<LogicalExpression>> defaultScorer) {
        this.repo = repo;
        this.lexiconId = lexiconId;
        this.modelId = modelId;
        this.defaultScorer = defaultScorer;

        checkScorer();
    }

    private ILexicon<LogicalExpression> getLexicon() {
        if (lexicon == null)
            lexicon = repo.get(lexiconId);
        return lexicon;
    }

    private IModelImmutable<?, LogicalExpression> getModel() {
        if (model == null)
            model = repo.get(modelId);
        return model;
    }

    private void checkScorer() {
        scorerFile = new File(SCORER_PATH);
        if (!scorerFile.exists())
            throw new RuntimeException("cannot find scorer file at " + SCORER_PATH);
    }

    private Set<LexicalEntry<LogicalExpression>> getFilterLexicalEntries() {
        return getLexicon().toCollection().stream()
                .filter(entry -> GetFilterArguments.of(entry.getCategory().getSemantics()) != null)
                .collect(Collectors.toSet());
    }

    /**
     * Use the lexicon to build prior distributions over syntaxes for each attribute type.
     */
    private Map<String, Counter<Syntax>> buildSyntaxPriors() {
        Map<String, Counter<Syntax>> ret = new HashMap<>();

        // Collect LexicalEntry instances associated with each attribute type.
        Map<String, Set<LexicalEntry<LogicalExpression>>> entries = new HashMap<>();

        getFilterLexicalEntries().forEach(entry -> {
            Pair<String, String> filterArguments = GetFilterArguments.of(entry.getCategory().getSemantics());
            entries.computeIfAbsent(filterArguments.first(), k -> new HashSet<>());
            entries.get(filterArguments.first()).add(entry);
        });

        // Now aggregate attribute type -> syntax weights.
        for (Map.Entry<String, Set<LexicalEntry<LogicalExpression>>> entry : entries.entrySet()) {
            Counter<Syntax> attrCounter = new Counter<>();
            for (LexicalEntry<LogicalExpression> lexEntry : entry.getValue()) {
                Syntax entrySyntax = lexEntry.getCategory().getSyntax();

                // Make sure this call isn't circular by forcing the score call to use the default score init function
                // if necessary.
                alwaysDefault = true;
                double score = getModel().score(lexEntry);
                alwaysDefault = false;

                attrCounter.addTo(entrySyntax, score);
            }

            attrCounter.normalize();
            ret.put(entry.getKey(), attrCounter);
        }

        return ret;
    }

    /**
     * Use the lexicon to build prior distributions over terms for each attribute value.
     */
    private Map<String, Counter<String>> buildTermPriors() {
        Map<String, Counter<String>> ret = new HashMap<>();

        // Collect LexicalEntry instances associated with each attribute value.
        Map<String, Set<LexicalEntry<LogicalExpression>>> entries = new HashMap<>();

        getFilterLexicalEntries().forEach(entry -> {
            Pair<String, String> filterArguments = GetFilterArguments.of(entry.getCategory().getSemantics());
            entries.computeIfAbsent(filterArguments.second(), k -> new HashSet<>());
            entries.get(filterArguments.second()).add(entry);
        });

        // Now aggregate attribute value -> term weights.
        for (Map.Entry<String, Set<LexicalEntry<LogicalExpression>>> entry : entries.entrySet()) {
            Counter<String> attrCounter = new Counter<>();
            for (LexicalEntry<LogicalExpression> lexEntry : entry.getValue()) {
                // Make sure this call isn't circular by forcing the score call to use the default score init function
                // if necessary.
                alwaysDefault = true;
                double score = getModel().score(lexEntry);
                alwaysDefault = false;

                attrCounter.addTo(lexEntry.getTokens().toString(), score);
            }

            attrCounter.normalize();
            ret.put(entry.getKey(), attrCounter);
        }

        return ret;
    }

    private JSONArray runScorer() {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{SCRIPT_PATH, SCORER_PATH});
            BufferedReader outReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            return (JSONArray) new JSONParser().parse(outReader);
        } catch (IOException | ParseException e) {
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

    /**
     * Execute the prepared Bayesian model and return marginal distributions for each of {@link #QUERY_VARS}.
     */
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
        if (alwaysDefault)
            return defaultScorer.score(entry);

        Map<String, Counter<Syntax>> syntaxPriors = buildSyntaxPriors();
        Map<String, Counter<String>> termPriors = buildTermPriors();
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
