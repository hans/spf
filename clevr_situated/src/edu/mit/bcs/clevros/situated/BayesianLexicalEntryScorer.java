package edu.mit.bcs.clevros.situated;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import edu.cornell.cs.nlp.spf.base.token.TokenSeq;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.lexicon.ILexicon;
import edu.cornell.cs.nlp.spf.ccg.lexicon.LexicalEntry;
import edu.cornell.cs.nlp.spf.data.IDataItem;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.learn.ILearnerListener;
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
import java.util.function.Function;
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
public class BayesianLexicalEntryScorer implements ISerializableScorer<LexicalEntry<LogicalExpression>>, ILearnerListener {

    private static final String SCRIPT_PATH = "webppl";
    private static final String SCORER_TEMPLATE_PATH = "syntaxGuidedScorer.wppl.template";
    private static final String SCORER_PATH = "syntaxGuidedScorer.%d.wppl";

    private static final List<String> QUERY_VARS = Arrays.asList("attr", "attrVal");
    private static final Set<String> QUERY_VARS_SET = new HashSet<>(QUERY_VARS);

    private static final List<String> ATTRIBUTES = Arrays.asList("shape", "color");
    private static final Map<String, List<String>> ATTRIBUTE_VALUES = new HashMap<>();
    static {
        ATTRIBUTE_VALUES.put("shape", Arrays.asList(CLEVRTypes.SHAPES));
        ATTRIBUTE_VALUES.put("color", Arrays.asList(CLEVRTypes.COLORS));
    }

    private static final List<Syntax> SYNTAXES = Arrays.asList(Syntax.read("N"), Syntax.read("N/N"));

    private static final Set<TokenSeq> IGNORE_LEX_ENTRIES = new HashSet<>();
    static {
        // HACK: don't let seed lexicon entries influence our priors over syntax <-> semantics mappings
        IGNORE_LEX_ENTRIES.add(TokenSeq.of("x"));
    }

    private ILexicon<LogicalExpression> lexicon;
    private IModelImmutable<?, LogicalExpression> model;
    private String lexiconId;
    private String modelId;

    private final IResourceRepository repo;

    private final IScorer<LexicalEntry<LogicalExpression>> defaultScorer;
    private boolean alwaysDefault = true;

    private final Random random = new Random();

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
        if (!new File(SCORER_TEMPLATE_PATH).exists())
            throw new RuntimeException("cannot find scorer template file at " + SCORER_TEMPLATE_PATH);
    }

    public void clearCache() {
        cache.clear();
    }

    @Override
    public void beganDataItem(IDataItem<?> dataItem) {
        clearCache();
    }

    /**
     * Thin wrapper on {@link LexicalEntry} which implements a different hashCode.
     */
    private static class WrappedLexicalEntry {

        private final LexicalEntry<LogicalExpression> entry;

        public WrappedLexicalEntry(LexicalEntry<LogicalExpression> entry) {
            this.entry = entry;
        }

        public LexicalEntry<LogicalExpression> getEntry() {
            return entry;
        }

        public Syntax getSyntax() {
            return entry.getCategory().getSyntax();
        }

        public LogicalExpression getSemantics() {
            return entry.getCategory().getSemantics();
        }

        public String getTerm() {
            return entry.getTokens().toString();
        }

        @Override
        public String toString() {
            return entry.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrappedLexicalEntry that = (WrappedLexicalEntry) o;
            return Objects.equals(entry.toString(), that.entry.toString());
        }

        @Override
        public int hashCode() {
            return Objects.hash(entry.toString());
        }
    }

    private Set<WrappedLexicalEntry> getFilterLexicalEntries() {
        return getLexicon().toCollection().stream()
                .filter(entry -> GetFilterArguments.of(entry.getCategory().getSemantics()) != null)
                .filter(entry -> !IGNORE_LEX_ENTRIES.contains(entry.getTokens()))
                .map(WrappedLexicalEntry::new)
                .collect(Collectors.toSet());
    }

    private Set<String> getFilterTerms() {
        return getFilterLexicalEntries().stream()
                .map(WrappedLexicalEntry::getTerm)
                .collect(Collectors.toSet());
    }

    /**
     * Build a set of conditional distributions p(x|y) for discrete events x, y.
     * This method is used to build distributions of the form p(syntax | attribute type) and p(term | attribute value).
     *
     * @param conditionalSupport Support of the conditional distribution.
     * @param conditionalFunction Function mapping a lexicon entry to its assignment in the conditional distribution.
     * @param supportFunction Function mapping a lexicon entry to its assignment in the posterior query distribution.
     * @param <T> Type of the posterior support.
     * @return
     */
    private <T> Map<String, Counter<T>> buildPriorDistribution(List<String> conditionalSupport,
                                                               List<T> support,
                                                               Function<WrappedLexicalEntry, String> conditionalFunction,
                                                               Function<WrappedLexicalEntry, T> supportFunction) {
        Map<String, Counter<T>> ret = new HashMap<>();

        // Collect LexicalEntry instances associated with each attribute type.
        Map<String, Set<WrappedLexicalEntry>> entries = new HashMap<>();
        getFilterLexicalEntries().forEach(entry -> {
            String condKey = conditionalFunction.apply(entry);
            entries.computeIfAbsent(condKey, k -> new HashSet<>()).add(entry);
        });

        Function<String, Counter<T>> defaultCounter = k -> new Counter<>(1.0, support);
        for (String value : conditionalSupport)
            ret.put(value, defaultCounter.apply(null));

        // Now aggregate weights for each conditional distribution.
        for (Map.Entry<String, Set<WrappedLexicalEntry>> entry : entries.entrySet()) {
            Counter<T> attrCounter = ret.get(entry.getKey());
            for (WrappedLexicalEntry lexEntry : entry.getValue()) {
                T entryKey = supportFunction.apply(lexEntry);

                // Make sure this call isn't circular by forcing the score call to use the default score init function
                // if necessary.
                alwaysDefault = true;
                double score = getModel().score(lexEntry.getEntry());
                alwaysDefault = false;

                attrCounter.addTo(entryKey, score);
            }
        }

        // The lexicon yields weights over the posterior support. Adjust these with a temperature parameter
        // and normalize.
        double temperature = 5;
        for (String value : conditionalSupport) {
            Counter<T> counter = ret.get(value);
            counter.div(temperature);
            counter.exp();
            counter.normalize(3);
        }

        return ret;
    }

    /**
     * Use the lexicon to build prior distributions over syntaxes for each attribute type.
     */
    private Map<String, Counter<Syntax>> buildSyntaxPriors(List<Syntax> support) {
        return buildPriorDistribution(
                ATTRIBUTES,
                support,
                (entry) -> GetFilterArguments.of(entry.getSemantics()).first(),
                (entry) -> entry.getSyntax());
    }

    /**
     * Use the lexicon to build prior distributions over terms for each attribute value.
     */
    private Map<String, Counter<String>> buildTermPriors(List<String> support) {
        return buildPriorDistribution(
                ATTRIBUTE_VALUES.values().stream().flatMap(List::stream).collect(Collectors.toList()),
                support,
                entry -> GetFilterArguments.of(entry.getSemantics()).second(),
                entry -> entry.getTerm());
    }

    private JSONArray runScorer(String scorerPath) {
        for (int tries = 0; tries < 5; tries++) {
            try {
                ProcessBuilder pb = new ProcessBuilder(SCRIPT_PATH, scorerPath);
                pb.redirectError(new File("err.out"));
                Process proc = pb.start();

                BufferedReader outReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                JSONArray ret = (JSONArray) new JSONParser().parse(outReader);
                return ret;
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        System.exit(1);
        return null;
    }

    private Counter<List<String>> getScores(String scorerPath) {
        JSONArray scores = runScorer(scorerPath);
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
    private Map<String, Counter<String>> getMarginalizedScores(String scorerPath) {
        Counter<List<String>> fullTable = getScores(scorerPath);
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
            Counter<T> thisPrior = priorDistribution.get(key);
            String priorValueStr = support2.stream().map(val -> String.valueOf(thisPrior.get(val)))
                    .collect(Collectors.joining(","));
            ret.append(String.format("\t\"%s\": Dirichlet({alpha: Vector([%s])}),\n", key, priorValueStr));
        });

        ret.append("}");
        return ret.toString();
    }

    private String buildScript(LexicalEntry<LogicalExpression> entry) throws IOException {
        // Make sure that queried entry appears in the support.
        Set<String> allTermsSet = getFilterTerms();
        allTermsSet.add(entry.getTokens().toString());
        List<String> allTerms = new ArrayList<>(allTermsSet);

        // Calculate prior distributions from lexicon weights.
        Map<String, Counter<Syntax>> syntaxPriors = buildSyntaxPriors(SYNTAXES);
        Map<String, Counter<String>> termPriors = buildTermPriors(allTerms);

        List<String> allAttributes = new ArrayList<>(ATTRIBUTE_VALUES.keySet());
        List<String> allAttributeValues = ATTRIBUTE_VALUES.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Collect template variables.
        HashMap<String, String> tData = new HashMap<>();
        tData.put("properties", gson.toJson(ATTRIBUTE_VALUES));
        tData.put("terms", gson.toJson(allTerms));
        tData.put("syntaxes", gson.toJson(SYNTAXES.stream().map(Syntax::toString).collect(Collectors.toList())));
        tData.put("termPriors", buildPriorString(termPriors, allAttributeValues, allTerms));
        tData.put("syntaxPriors", buildPriorString(syntaxPriors, allAttributes, SYNTAXES));
        tData.put("queryTerm", entry.getTokens().toString());
        tData.put("querySyntax", entry.getCategory().getSyntax().toString());

        BufferedReader templateReader = new BufferedReader(new FileReader(SCORER_TEMPLATE_PATH));
        Template tmpl = Mustache.compiler().escapeHTML(false).compile(templateReader);
        String scoreCode = tmpl.execute(tData);

        String scorerPath = String.format(SCORER_PATH, random.nextInt());
        Files.write(Paths.get(scorerPath), Arrays.asList(scoreCode.split("\n")));
        return scorerPath;
    }

    /**
     * Get the posterior predictive joint distribution over (attribute type, attribute value)
     * for a given LexicalEntry.
     */
    private Counter<List<String>> getPosterior(LexicalEntry<LogicalExpression> entry) {
        try {
            String scorerPath = buildScript(entry);
            Counter<List<String>> scores = getScores(scorerPath);
            Files.delete(Paths.get(scorerPath));

            System.out.printf("%30s\t%s\t%s\n", entry.getTokens(), entry.getCategory().getSyntax(),
                    scores);
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
        if (!SYNTAXES.contains(entry.getCategory().getSyntax()))
            return defaultScorer.score(entry);

        // DEV: Only deal with particular semantics for now!
        if (GetFilterArguments.of(entry.getCategory().getSemantics()) == null)
            return defaultScorer.score(entry);

        Pair<String, Syntax> cacheKey = Pair.of(entry.getTokens().toString(), entry.getCategory().getSyntax());
        Counter<List<String>> distribution = cache.computeIfAbsent(cacheKey, k -> getPosterior(entry));

        Pair<String, String> filterArguments = GetFilterArguments.of(entry.getCategory().getSemantics());
        List<String> distKey = Arrays.asList(filterArguments.first(), filterArguments.second());

        //System.out.printf("%s %s %s %f\n", entry.getTokens(), entry.getCategory().getSyntax(), filterArguments, distribution.get(distKey));

        return distribution.get(distKey);
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
