package com.njst.gaming.Geometries;

import java.util.Map;

/**
 * A simple parametric L-System that expands an axiom string by applying
 * production rules a specified number of times.
 *
 * <p>Supported turtle symbols interpreted by {@link PlantGeometry}:</p>
 * <pre>
 *  F  - draw branch segment forward
 *  +  - yaw left by angle
 *  -  - yaw right by angle
 *  ^  - pitch up by angle
 *  &amp;  - pitch down by angle
 *  \  - roll left by angle
 *  /  - roll right by angle
 *  [  - push turtle state (start sub-branch)
 *  ]  - pop turtle state (end sub-branch)
 *  L  - place a leaf quad
 * </pre>
 */
public class LSystem {

    private final Map<Character, String> rules;
    private final int iterations;

    public LSystem(Map<Character, String> rules, int iterations) {
        this.rules      = rules;
        this.iterations = iterations;
    }

    /**
     * Expands {@code axiom} by applying rules {@code iterations} times.
     *
     * @param axiom starting symbol string
     * @return final expanded string
     */
    public String expand(String axiom) {
        String current = axiom;
        for (int i = 0; i < iterations; i++) {
            StringBuilder next = new StringBuilder(current.length() * 3);
            for (char c : current.toCharArray()) {
                String replacement = rules.get(c);
                next.append(replacement != null ? replacement : c);
            }
            current = next.toString();
        }
        return current;
    }
}
