package com.njst.gaming.ri.battlearena;

public final class BattleArenaGuWormAction {
    public static final String CREATE_WATER = "create_water";
    public static final String CREATE_ICE = "create_ice";
    public static final String CREATE_EARTH = "create_earth";
    public static final String CREATE_FLAME = "create_flame";
    public static final String SHAPE_SPEAR = "shape_spear";
    public static final String SHAPE_WALL = "shape_wall";
    public static final String COOL = "cool";
    public static final String HEAT = "heat";
    public static final String MOVE_FORWARD = "move_forward";

    private BattleArenaGuWormAction() {
    }

    public static boolean isShapeAction(String action) {
        return SHAPE_SPEAR.equals(action) || SHAPE_WALL.equals(action);
    }
}
