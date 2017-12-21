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
import static edu.cornell.cs.nlp.spf.data.situated.labeled.CLEVRTypes.CLEVRRelation;

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

        smallObject = new CLEVRObject("red", "small", "cylinder", "metal",
                0, 0, 0, 0);
        largeObject = new CLEVRObject("green", "large", "cylinder", "rubber",
                0, 0, 0, 0);

        objects = new HashSet<>();
        objects.add(smallObject);
        objects.add(largeObject);

        // small object is in front of large object
        Map<CLEVRRelation, Map<CLEVRObject, Set<CLEVRObject>>> relations = new HashMap<>();

        Map<CLEVRObject, Set<CLEVRObject>> behindMap = new HashMap<>();
        behindMap.put(largeObject, Collections.singleton(smallObject));
        behindMap.put(smallObject, Collections.emptySet());
        relations.put(CLEVRRelation.BEHIND, behindMap);

        Map<CLEVRObject, Set<CLEVRObject>> frontMap = new HashMap<>();
        frontMap.put(smallObject, Collections.singleton(largeObject));
        frontMap.put(largeObject, Collections.emptySet());
        relations.put(CLEVRRelation.FRONT, frontMap);

        relations.put(CLEVRRelation.RIGHT, Collections.emptyMap());
        relations.put(CLEVRRelation.LEFT, Collections.emptyMap());

        scene = new CLEVRScene(0, objects, relations);
    }

    @AfterEach
    void tearDown() {
        LogicLanguageServices.setInstance(null);
    }

    @Test
    void testEvaluateFilter() {
        assertEquals(
                new CLEVRAnswer(Collections.singleton(largeObject)),
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

    @Test
    void testEvaluateExist() {
        assertEquals(
                new CLEVRAnswer(true),
                scene.evaluate(
                        "(exists:<<e,t>,t> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> red:pc)))"
                )
        );

        assertEquals(
                new CLEVRAnswer(false),
                scene.evaluate(
                        "(exists:<<e,t>,t> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> blue:pc)))"
                )
        );
    }

    @Test
    void testEvaluateCount() {
        assertEquals(
                new CLEVRAnswer(2),
                scene.evaluate(
                        "(count:<<e,t>,i> (filter_shape:<<e,t>,<psh,<e,t>>> scene:<e,t> cylinder:psh)))"
                )
        );
    }

    @Test
    void testEvaluateGreaterThan() {
        assertEquals(
                new CLEVRAnswer(true),
                scene.evaluate(
                        "(greater_than:<i,<i,t>> " +
                                "(count:<<e,t>,i> (filter_shape:<<e,t>,<psh,<e,t>>> scene:<e,t> cylinder:psh)))" +
                                "(count:<<e,t>,i> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi))))"
                )
        );
    }

    @Test
    void testEvaluateRelate() {
        assertEquals(
                new CLEVRAnswer(Collections.singleton(smallObject)),
                scene.evaluate(
                        "(relate:<e,<s,<e,t>>> " +
                                "(unique:<<e,t>,e> " +
                                "(filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi))" +
                                "front:s)"
                )
        );
    }

    @Test
    void testEvaluateEqual() {
        assertEquals(
                new CLEVRAnswer(true),
                scene.evaluate(
                        "(equal_shape:<psh,<psh,t>> " +
                                "(query_shape:<e,psh> (unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> small:psi)))" +
                                "(query_shape:<e,psh> (unique:<<e,t>,e> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi))))"
                )
        );
    }
}