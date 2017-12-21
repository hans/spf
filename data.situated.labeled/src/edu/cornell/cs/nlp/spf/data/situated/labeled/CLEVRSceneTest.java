package edu.cornell.cs.nlp.spf.data.situated.labeled;

import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CLEVRSceneTest {

    private CLEVRObject smallObject;
    private CLEVRObject largeObject;
    private Set<CLEVRObject> objects;

    private CLEVRScene scene;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        final File typesFile = new File("../clevr_basic/resources/geo.types");

        List<File> ontFiles = new ArrayList<>();
        ontFiles.add(new File("../clevr_basic/resources/geo.preds.ont"));
        ontFiles.add(new File("../clevr_basic/resources/geo.consts.ont"));

        try {
            LogicLanguageServices.setInstance(
                    new LogicLanguageServices.Builder(
                            new TypeRepository(typesFile),
                            new FlexibleTypeComparator())
                            .setNumeralTypeName("i").setUseOntology(true)
                            .addConstantsToOntology(ontFiles).closeOntology(true)
                            .build()
            );
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        smallObject = new CLEVRObject("black", "small", "cylinder", "metal",
                0, 0, 0, 0);
        largeObject = new CLEVRObject("green", "large", "cylinder", "rubber",
                0, 0, 0, 0);

        objects = new HashSet<>();
        objects.add(smallObject);
        objects.add(largeObject);

        scene = new CLEVRScene(0, objects, null);
    }

    @AfterEach
    void tearDown() {
        LogicLanguageServices.setInstance(null);
    }

    private <T> Set<T> setOf(T... args) {
        Set<T> ret = new HashSet<>();
        Collections.addAll(ret, args);
        return ret;
    }

    @Test
    void testEvaluateFilter() {
        assertEquals(
                new CLEVRAnswer(setOf(largeObject)),
                scene.evaluate(
                        "(filter_material:<<e,t>,<pm,<e,t>>> scene:<e,t> rubber:pm)")
        );
    }

    @Test
    void testEvaluateUnique() {
        assertEquals(
                new CLEVRAnswer(largeObject),
                scene.evaluate(
                        "(unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi))"
                )
        );
    }

    @Test
    void testEvaluatePropertyQuery() {
        assertEquals(
                new CLEVRAnswer("rubber"),
                scene.evaluate(
                        "(query_material:<e,pm> (unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi)))"
                )
        );
    }

    @Test
    void testEvaluateSame() {
        assertEquals(
                new CLEVRAnswer(true),
                scene.evaluate(
                        "(same_shape:<e,<e,t>> " +
                                "(unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi))" +
                                "(unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> small:psi)))"
                )
        );
    }

    @Test
    void testEvaluateUnion() {
        assertEquals(
                new CLEVRAnswer(objects),
                scene.evaluate(
                        "(union:<<e,t>,<<e,t>,<e,t>>> " +
                                "(filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi)" +
                                "(filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> small:psi))"
                )
        );
    }
}