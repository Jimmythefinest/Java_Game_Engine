package com.njst.gaming.Geometries;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Derives a {@link PlantConfig} deterministically from an integer seed.
 *
 * <p>Usage:</p>
 * <pre>
 *   PlantConfig config = new PlantSeed(42).generateConfig();
 *   PlantGeometry geo  = new PlantGeometry(config);
 * </pre>
 *
 * Different seeds produce visually distinct plants (different angles, branch
 * patterns, leaf densities, etc.), just like changing the terrain seed
 * produces a different heightmap.
 */
public class PlantSeed {

    private final long seed;

    // -------------------------------------------------------------------------
    // Built-in L-System grammar templates
    // -------------------------------------------------------------------------

    /** Simple bush — lots of branching in all directions */
    private static final String[][] GRAMMAR_BUSH = {
        {"F", "FF-[-F+F+F]+[^+F^F^F]&[&F-F-F]"}
    };

    /** Classic tree — yaw + pitch splits */
    private static final String[][] GRAMMAR_TREE = {
        {"F", "F[^+F]F[&-F][/+F][\\-F]F"}
    };

    /** Fern-like plant — uses X placeholder for recursion */
    private static final String[][] GRAMMAR_FERN = {
        {"F", "FF"},
        {"X", "F+[^X]-F[&-FX]+X"}
    };

    /** Spreading canopy — fans in both yaw and pitch */
    private static final String[][] GRAMMAR_CANOPY = {
        {"F", "F[^+F][&-F][/+F][\\-F]F"}
    };

    /** 3D spiral weed — each branch rolls AND pitches */
    private static final String[][] GRAMMAR_SPIRAL = {
        {"F", "F[^/+F][&\\-F][+^F][-&F]F"}
    };

    private static final String[][] [] ALL_GRAMMARS = {
        GRAMMAR_BUSH, GRAMMAR_TREE, GRAMMAR_FERN, GRAMMAR_CANOPY, GRAMMAR_SPIRAL
    };

    private static final String[] AXIOMS = {
        "F",       // bush, tree, canopy, spiral
        "FFFFY",   // fern-like — 'Y' ignored, 'X' used inside rule
        "X",       // fern
        "F",
        "F"
    };

    // -------------------------------------------------------------------------

    public PlantSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Generates a fully-populated {@link PlantConfig} from the stored seed.
     *
     * @return deterministic plant configuration
     */
    public PlantConfig generateConfig() {
        Random rng = new Random(seed);

        // Pick grammar template
        int grammarIndex = rng.nextInt(ALL_GRAMMARS.length);
        String[][] grammarTemplate = ALL_GRAMMARS[grammarIndex];
        String axiom = AXIOMS[grammarIndex];

        // Build rules map
        Map<Character, String> rules = new HashMap<>();
        for (String[] entry : grammarTemplate) {
            char symbol = entry[0].charAt(0);
            String replacement = entry[1];
            rules.put(symbol, replacement);
        }

        // Derive numeric parameters
        // Capping iterations to prevent massive vertex counts (OOM)
        int iterations = 3 + rng.nextInt(2);                       // 3–4 (was 3-5, but 5 is too much for complex rules)
        
        float angle       = (float) Math.toRadians(18 + rng.nextInt(28)); // 18°–45°
        float stepLength  = 0.25f + rng.nextFloat() * 0.5f;        // 0.25–0.75
        float trunkRadius = 0.06f + rng.nextFloat() * 0.09f;       // 0.06–0.15
        float branchTaper = 0.60f + rng.nextFloat() * 0.20f;       // 0.60–0.80 (slightly more taper reduces detail)
        float leafSize    = 0.08f + rng.nextFloat() * 0.15f;       // 0.08–0.23 (smaller leaves)

        return new PlantConfig(axiom, rules, iterations, angle,
                               stepLength, trunkRadius, branchTaper, leafSize);
    }
}
