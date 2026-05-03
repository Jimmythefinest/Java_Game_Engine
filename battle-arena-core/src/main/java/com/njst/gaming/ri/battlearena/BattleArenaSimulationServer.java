package com.njst.gaming.ri.battlearena;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaSimulationServer implements BattleArenaPlayerStateProvider {
    private final BattleArenaLocalPlayerStateServer stateServer;
    private final BattleArenaHeadlessCombatSystem combatSystem = new BattleArenaHeadlessCombatSystem();
    private final Map<String, BattleArenaNpcController> npcControllers =
            new LinkedHashMap<String, BattleArenaNpcController>();

    public BattleArenaSimulationServer(float[][] spawnPositions,
                                       Map<String, BattleArenaAnimationTiming> animationTimings) {
        this.stateServer = new BattleArenaLocalPlayerStateServer(spawnPositions, animationTimings);
    }

    public void setNpcController(String playerId, BattleArenaNpcController controller) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return;
        }
        if (controller == null) {
            npcControllers.remove(playerId);
            return;
        }
        npcControllers.put(playerId, controller);
    }

    @Override
    public List<BattleArenaPlayerState> initialStates() {
        return combatSystem.attachHealth(stateServer.initialStates());
    }

    @Override
    public int currentTick() {
        return stateServer.currentTick();
    }

    @Override
    public float tickSeconds() {
        return stateServer.tickSeconds();
    }

    @Override
    public void submitInput(String playerId, int tick, BattleArenaPlayerInput input) {
        stateServer.submitInput(playerId, tick, input);
    }

    @Override
    public void tick() {
        submitNpcInputs();
        stateServer.tick();
        combatSystem.update(stateServer.currentTick(), stateServer.snapshotStates(), stateServer);
    }

    @Override
    public List<BattleArenaPlayerState> snapshotStates() {
        return combatSystem.attachHealth(stateServer.snapshotStates());
    }

    public BattleArenaSimulationSnapshot snapshot() {
        return new BattleArenaSimulationSnapshot(
                currentTick(),
                tickSeconds(),
                snapshotStates());
    }

    @Override
    public BattleArenaPlayerState stateForPlayer(String playerId) {
        return stateServer.stateForPlayer(playerId);
    }

    private void submitNpcInputs() {
        if (npcControllers.isEmpty()) {
            return;
        }
        List<BattleArenaPlayerState> snapshot = stateServer.snapshotStates();
        int targetTick = stateServer.currentTick() + 1;
        for (Map.Entry<String, BattleArenaNpcController> entry : npcControllers.entrySet()) {
            BattleArenaPlayerState npcState = stateServer.stateForPlayer(entry.getKey());
            BattleArenaNpcController controller = entry.getValue();
            if (npcState == null || controller == null) {
                continue;
            }
            BattleArenaPlayerInput input = controller.decide(
                    npcState,
                    snapshot,
                    stateServer.currentTick());
            stateServer.submitInput(entry.getKey(), targetTick, input);
        }
    }
}
