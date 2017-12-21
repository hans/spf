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
    private CLEVRObject thirdObject;
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
        largeObject = new CLEVRObject("blue", "large", "cylinder", "rubber",
                0, 5, 5, 0);
        thirdObject = new CLEVRObject("green", "large", "cube", "metal",
                0, 5, 3.5, 0);

        objects = new HashSet<>();
        objects.add(smallObject);
        objects.add(largeObject);
        objects.add(thirdObject);

        // small object is in front of large object
        // third object is between and to the right of small+large object
        Map<CLEVRRelation, Map<CLEVRObject, Set<CLEVRObject>>> relations = new HashMap<>();

        Map<CLEVRObject, Set<CLEVRObject>> behindMap = new HashMap<>();
        Set<CLEVRObject> behindLarge = new HashSet<>();
        behindLarge.add(smallObject);
        behindLarge.add(thirdObject);
        behindMap.put(largeObject, behindLarge);
        behindMap.put(thirdObject, Collections.singleton(smallObject));
        relations.put(CLEVRRelation.BEHIND, behindMap);

        Map<CLEVRObject, Set<CLEVRObject>> frontMap = new HashMap<>();
        Set<CLEVRObject> frontSmall = new HashSet<>();
        frontSmall.add(largeObject);
        frontSmall.add(thirdObject);
        frontMap.put(smallObject, frontSmall);
        frontMap.put(thirdObject, Collections.singleton(largeObject));
        relations.put(CLEVRRelation.FRONT, frontMap);

        Map<CLEVRObject, Set<CLEVRObject>> rightMap = new HashMap<>();
        Set<CLEVRObject> rightThird = new HashSet<>();
        rightThird.add(smallObject);
        rightThird.add(largeObject);
        rightMap.put(thirdObject, rightThird);
        relations.put(CLEVRRelation.RIGHT, rightMap);

        Map<CLEVRObject, Set<CLEVRObject>> leftMap = new HashMap<>();
        leftMap.put(smallObject, Collections.singleton(thirdObject));
        leftMap.put(largeObject, Collections.singleton(thirdObject));
        relations.put(CLEVRRelation.LEFT, leftMap);

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

        // Nested filters
        assertEquals(
                new CLEVRAnswer(Collections.singleton(largeObject)),
                scene.evaluate(
                        "(filter_size:<<e,t>,<psi,<e,t>>> " +
                                "(filter_material:<<e,t>,<pm,<e,t>>> scene:<e,t> rubber:pm) large:psi)")
        );
    }

    @Test
    void testEvaluateUnique() {
        assertEquals(
                new CLEVRAnswer(thirdObject),
                scene.evaluate(
                        "(unique:<<e,t>,e> (filter_shape:<<e,t>,<psh,<e,t>>> scene:<e,t> cube:psh))"
                )
        );
    }

    @Test
    void testEvaluatePropertyQuery() {
        assertEquals(
                new CLEVRAnswer("rubber"),
                scene.evaluate(
                        "(query_material:<e,pm> (unique:<<e,t>,e> " +
                                "(filter_shape:<<e,t>,<psh,<e,t>>> (filter_size:<<e,t>,<psi,<e,t>>> scene:<e,t> large:psi) cylinder:psh))"
                )
        );
    }

    @Test
    void testEvaluateSame() {
        assertEquals(
                new CLEVRAnswer(true),
                scene.evaluate(
                        "(same_shape:<e,<e,t>> " +
                                "(unique:<<e,t>,e> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> red:pc))" +
                                "(unique:<<e,t>,e> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> blue:pc)))"
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
                        "(exists:<<e,t>,t> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> brown:pc)))"
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
                                "(count:<<e,t>,i> (filter_shape:<<e,t>,<psh,<e,t>>> scene:<e,t> cube:psh))))"
                )
        );
    }

    @Test
    void testEvaluateRelate() {
        Set<CLEVRObject> expected = new HashSet<>();
        expected.add(smallObject);
        expected.add(thirdObject);

        assertEquals(
                new CLEVRAnswer(expected),
                scene.evaluate(
                        "(relate:<e,<s,<e,t>>> " +
                                "(unique:<<e,t>,e> " +
                                "(filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> blue:pc))" +
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
                                "(query_shape:<e,psh> (unique:<<e,t>,e> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> red:pc)))" +
                                "(query_shape:<e,psh> (unique:<<e,t>,e> (filter_color:<<e,t>,<pc,<e,t>>> scene:<e,t> blue:pc))))"
                )
        );
    }
}