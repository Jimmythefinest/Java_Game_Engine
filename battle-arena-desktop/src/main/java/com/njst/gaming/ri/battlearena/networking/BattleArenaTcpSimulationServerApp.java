package com.njst.gaming.ri.battlearena.networking;

public final class BattleArenaTcpSimulationServerApp {
    private BattleArenaTcpSimulationServerApp() {
    }

    public static void main(String[] args) throws Exception {
        int port = readPort(args);
        BattleArenaTcpSimulationServer server = new BattleArenaTcpSimulationServer();
        server.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.close();
            }
        }, "battle-arena-simulation-server-shutdown"));
        server.runLoop();
    }

    private static int readPort(String[] args) {
        if (args != null && args.length > 0) {
            return parsePort(args[0], BattleArenaTcpSimulationServer.DEFAULT_PORT);
        }
        return parsePort(
                System.getProperty("battleArena.simulationPort"),
                BattleArenaTcpSimulationServer.DEFAULT_PORT);
    }

    private static int parsePort(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
