package edu.mit.bcs.clevros.situated;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private static final String SCRIPT_PATH = "./run_wppl";
    private static final String SCORER_TEMPLATE_PATH = "syntaxGuidedScorer.wppl.template";
    private static final String SCORER_PATH = "syntaxGuidedScorer.wppl";
    private File scorerFile;

    private static final String QUERY_CODE =
                    "var qAttr = sample(attr);\n" +
                    "var qAttrVal = sample(attrVal(qAttr));\n" +
                    "observe(term(qAttrVal), \"%s\");\n" +
                    "observe(syntax(qAttr), \"%s\");\n" +
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
    private static final Set<Syntax> SUPPORTED_SYNTAXES =
            new HashSet<>(Arrays.asList(Syntax.read("N"), Syntax.read("N/N")));
    private boolean alwaysDefault = true;

    /**
     * Caches posterior predictive distributions computed for a particular model state.
     */
    private transient HashMap<Pair<String, Syntax>, Counter<List<String>>> cache = new HashMap<>();

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

    public void enable() {
        alwaysDefault = false;
    }

    public void disable() {
        alwaysDefault = true;
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
    private Map<String, Counter<Syntax>> buildSyntaxPriors(LexicalEntry<?> queryEntry) {
        Map<String, Counter<Syntax>> ret = new HashMap<>();

        // Collect LexicalEntry instances associated with each attribute type.
        Map<String, Set<LexicalEntry<LogicalExpression>>> entries = new HashMap<>();
        getFilterLexicalEntries().forEach(entry -> {
            Pair<String, String> filterArguments = GetFilterArguments.of(entry.getCategory().getSemantics());
            entries.computeIfAbsent(filterArguments.first(), k -> new HashSet<>());
            entries.get(filterArguments.first()).add(entry);
        });

        // Make sure each attribute->term distribution is present, and that each distribution has the query term in
        // its support.
        Syntax querySyntax = queryEntry.getCategory().getSyntax();
        for (String attribute : ATTRIBUTES) {
            Counter<Syntax> attrCounter = ret.computeIfAbsent(attribute, k -> new Counter<>(1.0));
            attrCounter.get(querySyntax);
        }

        // Now aggregate attribute type -> syntax weights.
        for (Map.Entry<String, Set<LexicalEntry<LogicalExpression>>> entry : entries.entrySet()) {
            Counter<Syntax> attrCounter = new Counter<>(1.0);
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
    private Map<String, Counter<String>> buildTermPriors(LexicalEntry<?> queryEntry) {
        Map<String, Counter<String>> ret = new HashMap<>();

        // Collect LexicalEntry instances associated with each attribute value.
        Map<String, Set<LexicalEntry<LogicalExpression>>> entries = new HashMap<>();
        getFilterLexicalEntries().forEach(entry -> {
            Pair<String, String> filterArguments = GetFilterArguments.of(entry.getCategory().getSemantics());
            entries.computeIfAbsent(filterArguments.second(), k -> new HashSet<>());
            entries.get(filterArguments.second()).add(entry);
        });

        // Make sure each attribute->term distribution is present, and that each distribution has the query term in
        // its support.
        String queryTerm = queryEntry.getTokens().toString();
        List<String> allAttributeValues = ATTRIBUTE_VALUES.values().stream()
                .flatMap(List::stream).collect(Collectors.toList());
        for (String attribute : allAttributeValues) {
            Counter<String> attrCounter = ret.computeIfAbsent(attribute, k -> new Counter<>(1.0));
            attrCounter.get(queryTerm);
        }

        // Now aggregate attribute value -> term weights.
        for (Map.Entry<String, Set<LexicalEntry<LogicalExpression>>> entry : entries.entrySet()) {
            Counter<String> attrCounter = ret.get(entry.getKey());
            for (LexicalEntry<LogicalExpression> lexEntry : entry.getValue()) {
                // Make sure this call isn't circular by forcing the score call to use the default score init function
                // if necessary.
                alwaysDefault = true;
                double score = getModel().score(lexEntry);
                alwaysDefault = false;

                attrCounter.addTo(lexEntry.getTokens().toString(), score);
            }
        }

        for (Counter<String> attrCounter : ret.values())
            attrCounter.normalize();

        return ret;
    }

    private JSONArray runScorer() {
        try {
            ProcessBuilder pb = new ProcessBuilder(SCRIPT_PATH, SCORER_PATH);
            pb.redirectError(new File("err.out"));
            Process proc = pb.start();

            BufferedReader outReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            return (JSONArray) new JSONParser().parse(outReader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    private Counter<List<String>> getScores() {
        JSONArray scores = runScorer();
        Counter<List<String>> ret = new Counter<>();

        for (Object scoreEl : scores) {
            JSONArray scoreTuple = (JSONArray) scoreEl;
            JSONObject supportEl = (JSONObject) scoreTuple.get(0);
            double score = scoreTuple.get(1) instanceof Long
                    ? (double) (long) scoreTuple.get(1) : (double) scoreTuple.get(1);

            if (!supportEl.keySet().equals(QUERY_VARS_SET))
                throw new RuntimeException("did not receive expected query data");

            List<String> values = QUERY_VARS.stream().map((var) -> (String) supportEl.get(var))
                    .collect(Collectors.toList());
            ret.put(values, score);
        }

        return ret;
    }

    /**
     * Execute the prepared Bayesian model and return marginal distributions for each of {@link #QUERY_VARS}.
     */
    private Map<String, Counter<String>> getMarginalizedScores() {
        Counter<List<String>> fullTable = getScores();
        Map<String, Counter<String>> ret = new HashMap<>();

        for (int i = 0; i < QUERY_VARS.size(); i++) {
            String var = QUERY_VARS.get(i);

            Counter<String> marginalized = new Counter<>();
            for (Map.Entry<List<String>, Double> entry : fullTable.entrySet()) {
                marginalized.addTo(entry.getKey().get(i), entry.getValue());
            }

            marginalized.normalize();
            ret.put(var, marginalized);
        }

        return ret;
    }

    /**
     * Prepare a webppl-friendly string describing prior distributions.
     *
     * @param priorDistribution
     * @param support1 Support of the conditional distribution (e.g. attribute type, attribute value).
     * @param support2 Support of the query distribution (e.g. syntax, term).
     * @return
     */
    private <T> String buildPriorString(Map<String, Counter<T>> priorDistribution,
                                        List<String> support1, List<T> support2) {
        StringBuilder ret = new StringBuilder();
        ret.append("{\n");

        support1.forEach(key -> {
            Counter<T> thisPrior = priorDistribution.computeIfAbsent(key, k -> new Counter<>(1.0));
            String priorValueStr = support2.stream().map(val -> String.valueOf(thisPrior.get(val)))
                    .collect(Collectors.joining(","));
            ret.append(String.format("\t\"%s\": Dirichlet({alpha: Vector([%s])}),\n", key, priorValueStr));
        });

        ret.append("}");
        return ret.toString();
    }

    private void buildScript(LexicalEntry<LogicalExpression> entry) throws IOException {
        Map<String, Counter<Syntax>> syntaxPriors = buildSyntaxPriors(entry);
        Map<String, Counter<String>> termPriors = buildTermPriors(entry);

        List<String> allAttributes = new ArrayList<>(ATTRIBUTE_VALUES.keySet());
        List<String> allAttributeValues = ATTRIBUTE_VALUES.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());

        List<String> allTerms = termPriors.values().stream()
                .flatMap(counter -> counter.keySet().stream())
                .distinct().collect(Collectors.toList());
        List<Syntax> allSyntaxes = syntaxPriors.values().stream()
                .flatMap(counter -> counter.keySet().stream())
                .distinct().collect(Collectors.toList());
        List<String> allSyntaxStrings = allSyntaxes.stream()
                .map(Syntax::toString).collect(Collectors.toList());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Collect template variables.
        HashMap<String, String> tData = new HashMap<>();
        tData.put("properties", gson.toJson(ATTRIBUTE_VALUES));
        tData.put("terms", gson.toJson(allTerms));
        tData.put("syntaxes", gson.toJson(allSyntaxStrings));
        tData.put("termPriors", buildPriorString(termPriors, allAttributeValues, allTerms));
        tData.put("syntaxPriors", buildPriorString(syntaxPriors, allAttributes, allSyntaxes));
        tData.put("queryCode", String.format(QUERY_CODE, entry.getTokens(), entry.getCategory().getSyntax()));

        BufferedReader templateReader = new BufferedReader(new FileReader(SCORER_TEMPLATE_PATH));
        Template tmpl = Mustache.compiler().escapeHTML(false).compile(templateReader);
        String scoreCode = tmpl.execute(tData);

        Files.write(Paths.get(SCORER_PATH), Arrays.asList(scoreCode.split("\n")));
    }

    /**
     * Get the posterior predictive joint distribution over (attribute type, attribute value)
     * for a given LexicalEntry.
     */
    private Counter<List<String>> getPosterior(LexicalEntry<LogicalExpression> entry) {
        try {
            buildScript(entry);
            Counter<List<String>> scores = getScores();
            System.out.printf("%s %s\n", entry.getTokens(), getMarginalizedScores().get("attr"));
            return scores;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public double score(LexicalEntry<LogicalExpression> entry) {
        if (alwaysDefault)
            return defaultScorer.score(entry);

        // DEV: We only deal with a restricted syntax for now!
        if (!SUPPORTED_SYNTAXES.contains(entry.getCategory().getSyntax()))
            return defaultScorer.score(entry);

        // DEV: Only deal with particular semantics for now!
        if (GetFilterArguments.of(entry.getCategory().getSemantics()) == null)
            return defaultScorer.score(entry);

        Pair<String, Syntax> cacheKey = Pair.of(entry.getTokens().toString(), entry.getCategory().getSyntax());
        Counter<List<String>> distribution = cache.computeIfAbsent(cacheKey, k -> getPosterior(entry));
        // TODO renormalize, etc.

        Pair<String, String> filterArguments = GetFilterArguments.of(entry.getCategory().getSemantics());
        List<String> distKey = Arrays.asList(filterArguments.first(), filterArguments.second());

        //System.out.printf("%s %s %s %f\n", entry.getTokens(), entry.getCategory().getSyntax(), filterArguments, distribution.get(distKey));

        return distribution.get(distKey);
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
                    : new UniformScorer<>(1.0);

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
