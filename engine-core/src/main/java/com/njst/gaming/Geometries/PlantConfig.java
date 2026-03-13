package com.njst.gaming.Geometries;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all parameters that define how a procedural plant is generated.
 * Created by {@link PlantSeed} deterministically from a long seed.
 */
public class PlantConfig {

    /** The starting symbol string for the L-System. */
    public final String axiom;

    /** Production rules: each character maps to a replacement string. */
    public final Map<Character, String> rules;

    /** Number of times the L-System is expanded (recursion depth). */
    public final int iterations;

    /** Branching angle in radians. */
    public final float angle;

    /** Length of each forward (F) segment. */
    public final float stepLength;

    /** Radius of the trunk at the base. */
    public final float trunkRadius;

    /** Factor by which branch radius shrinks at each push '['. Range 0-1. */
    public final float branchTaper;

    /** Half-size of a leaf quad. */
    public final float leafSize;

    public PlantConfig(String axiom,
                       Map<Character, String> rules,
                       int iterations,
                       float angle,
                       float stepLength,
                       float trunkRadius,
                       float branchTaper,
                       float leafSize) {
        this.axiom       = axiom;
        this.rules       = rules;
        this.iterations  = iterations;
        this.angle       = angle;
        this.stepLength  = stepLength;
        this.trunkRadius = trunkRadius;
        this.branchTaper = branchTaper;
        this.leafSize    = leafSize;
    }
}
