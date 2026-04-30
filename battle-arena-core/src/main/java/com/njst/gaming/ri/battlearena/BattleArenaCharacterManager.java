package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.collision.CollisionEventType;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterBrain;
import com.njst.gaming.ri.battlearena.controls.BattleArenaCharacterControlState;
import com.njst.gaming.ri.battlearena.gameobjects.BattleArenaHealthBarGameObject;
import com.njst.gaming.ri.battlearena.gameobjects.BattleArenaHitboxDebugGameObject;
import com.njst.gaming.ri.battlearena.networking.BattleArenaTcpControlClient;
import com.njst.gaming.ri.battlearena.networking.BattleArenaTcpRemoteController;
import com.njst.gaming.ri.battlearena.skills.BattleArenaFireballSkill;
import com.njst.gaming.ri.battlearena.skills.BattleArenaMudWallSkill;
import com.njst.gaming.ri.battlearena.skills.BattleArenaSkillContext;
import com.njst.gaming.ri.battlearena.skills.BattleArenaSkillSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BattleArenaCharacterManager {
    private static final String CHARACTER_DEFINITION_FILE = "battle_arena/defeated.character.json";
    private static final float PLAYER_SCALE = 1f;
    private static final float SECOND_CHARACTER_START_X = 2.5f;
    private static final float HEALTH_BAR_WIDTH = 1.35f;
    private static final float HEALTH_BAR_HEIGHT = 0.18f;
    private static final float HEALTH_BAR_VERTICAL_OFFSET = 2.35f;

    private final List<GameObject> playerMeshes = new ArrayList<>();
    private final List<BattleArenaHitboxDebugGameObject> debugHitboxes = new ArrayList<>();
    private final BattleArenaCharacterAssembler characterAssembler = new BattleArenaCharacterAssembler();
    private final BattleArenaCharacterDefinitionLoader characterDefinitionLoader = new BattleArenaCharacterDefinitionLoader();
    private final BattleArenaSkillSystem skillSystem = new BattleArenaSkillSystem();
    private final List<BattleArenaControlledCharacter> arenaCharacters = new ArrayList<>();
    private final List<BattleArenaControlledCharacter> npcCharacters = new ArrayList<>();
    private final Map<String, BattleArenaControlledCharacter> charactersByPlayer =
            new LinkedHashMap<String, BattleArenaControlledCharacter>();
    private final ArrayList<KeyframeAnimation> activeAnimations = new ArrayList<>();

    private BattleArenaControlledCharacter playerCharacter;
    private BattleArenaControlledCharacter activeCharacter;
    private WeightedGeometry characterGeometry;
    private BattleArenaCharacterDefinition characterDefinition;
    private BattleArenaHitboxTracks hitboxTracks;
    private int characterTexture;
    private boolean debugHitboxesVisible;
    private BattleArenaSkillContext.TerrainSampler terrainSampler;

    void reset() {
        playerMeshes.clear();
        debugHitboxes.clear();
        arenaCharacters.clear();
        npcCharacters.clear();
        charactersByPlayer.clear();
        activeAnimations.clear();
        playerCharacter = null;
        activeCharacter = null;
        characterGeometry = null;
        characterDefinition = null;
        hitboxTracks = null;
        characterTexture = 0;
        debugHitboxesVisible = false;
        terrainSampler = null;
    }

    void setTerrainSampler(BattleArenaSkillContext.TerrainSampler terrainSampler) {
        this.terrainSampler = terrainSampler;
    }

    void loadCharacterAssets(Scene scene, GraphicsDevice graphicsDevice) {
        characterDefinition = characterDefinitionLoader.load(graphicsDevice, CHARACTER_DEFINITION_FILE);
        characterGeometry = characterAssembler.loadWeightedGeometry(graphicsDevice, characterDefinition.model.mesh);
        activeAnimations.clear();
        hitboxTracks = BattleArenaHitboxTracks.load(graphicsDevice, resolveHitboxTrackPath(characterDefinition));
        characterTexture = loadCharacterTexture(graphicsDevice, characterDefinition);
    }

    void syncTcpCharacters(Scene scene,
                           GraphicsDevice graphicsDevice,
                           BattleArenaTcpControlClient tcpControlClient) {
        if (tcpControlClient == null || characterGeometry == null || characterDefinition == null) {
            return;
        }
        Set<String> activePlayers = tcpControlClient.getActivePlayersSnapshot();
        String assignedPlayer = tcpControlClient.getAssignedPlayer();
        if (assignedPlayer != null && !assignedPlayer.trim().isEmpty()) {
            activePlayers.add(assignedPlayer);
        }
        for (String player : activePlayers) {
            if (player == null || player.trim().isEmpty()) {
                continue;
            }
            BattleArenaControlledCharacter existing = charactersByPlayer.get(player);
            boolean shouldBeLocal = player.equals(assignedPlayer);
            if (existing != null && existing.playerControlled != shouldBeLocal) {
                despawnCharacter(scene, player);
                existing = null;
            }
            if (existing == null) {
                spawnTcpCharacter(scene, graphicsDevice, tcpControlClient, player, shouldBeLocal);
            }
        }

        HashSet<String> activePlayerCopy = new HashSet<String>(activePlayers);
        Iterator<String> playerIterator = charactersByPlayer.keySet().iterator();
        while (playerIterator.hasNext()) {
            String player = playerIterator.next();
            if (activePlayerCopy.contains(player)) {
                continue;
            }
            BattleArenaControlledCharacter character = charactersByPlayer.get(player);
            removeCharacterFromScene(scene, character);
            playerIterator.remove();
        }
        if (activeCharacter == null && !arenaCharacters.isEmpty()) {
            activeCharacter = arenaCharacters.get(0);
        }
    }

    void updateCharacters(Scene scene,
                          BattleArenaTcpControlClient tcpControlClient,
                          BattleArenaCharacterControlState playerControls,
                          BattleArenaSkillContext.TerrainSampler terrainSampler,
                          float deltaSeconds) {
        BattleArenaSkillContext skillContext = createSkillContext(scene, terrainSampler);
        if (playerCharacter != null) {
            playerCharacter.applyPlayerControls(playerControls);
            sendLocalControls(tcpControlClient, playerCharacter);
            playerCharacter.updateController(deltaSeconds);
            updateControlTriggeredSkills(playerCharacter, skillContext);
        }
        for (BattleArenaControlledCharacter npc : npcCharacters) {
            if (npc.brain instanceof BattleArenaSimpleChaseAi) {
                ((BattleArenaSimpleChaseAi) npc.brain)
                        .setIncomingFireballThreat(skillSystem.hasIncomingFireballThreat(
                                npc.runtime,
                                playerCharacter != null ? playerCharacter.runtime : null));
            }
            npc.captureControls(null, null, playerCharacter != null ? playerCharacter.runtime : null, deltaSeconds);
            sendLocalControls(tcpControlClient, npc);
            npc.updateController(deltaSeconds);
            updateControlTriggeredSkills(npc, skillContext);
        }
        updateFireballCasting(skillContext);
        skillSystem.update(skillContext, deltaSeconds);
        if (playerCharacter != null) {
            for (BattleArenaControlledCharacter npc : npcCharacters) {
                updateSideStepFacing(playerCharacter.runtime, npc.runtime);
                updateSideStepFacing(npc.runtime, playerCharacter.runtime);
            }
        }
    }

    ArrayList<ArrayList<KeyframeAnimation>> collectActiveSkeletonAnimations() {
        ArrayList<ArrayList<KeyframeAnimation>> skeletonAnimations = new ArrayList<>();
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            if (character == null || character.runtime == null) {
                continue;
            }
            ArrayList<KeyframeAnimation> activeSkeletonAnimations = collectActiveAnimations(character.runtime);
            if (!activeSkeletonAnimations.isEmpty()) {
                skeletonAnimations.add(activeSkeletonAnimations);
            }
        }
        return skeletonAnimations;
    }

    void syncRigs() {
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            character.runtime.syncRig();
        }
    }

    void handleHitboxCollision(Scene scene,
                               BattleArenaSkillContext.TerrainSampler terrainSampler,
                               CollisionEvent event) {
        if (event == null) {
            return;
        }
        if (event.getFirst() instanceof BattleArenaMudWallCollider
                || event.getSecond() instanceof BattleArenaMudWallCollider) {
            skillSystem.onCollision(createSkillContext(scene, terrainSampler), event);
            return;
        }
        if (event.getType() != CollisionEventType.ENTER) {
            return;
        }
        if (!(event.getFirst() instanceof BattleArenaHitboxCollider)
                || !(event.getSecond() instanceof BattleArenaHitboxCollider)) {
            return;
        }

        BattleArenaHitboxCollider first = (BattleArenaHitboxCollider) event.getFirst();
        BattleArenaHitboxCollider second = (BattleArenaHitboxCollider) event.getSecond();

        if (first.getType() == BattleArenaHitboxCollider.Type.HITBOX
                && second.getType() == BattleArenaHitboxCollider.Type.HURTBOX) {
            logHit(first, second, event);
            second.getCharacter().onHitTaken(first.getCharacter(), second.getName(), second.getOnHitAnimation());
            return;
        }
        if (second.getType() == BattleArenaHitboxCollider.Type.HITBOX
                && first.getType() == BattleArenaHitboxCollider.Type.HURTBOX) {
            logHit(second, first, event);
            first.getCharacter().onHitTaken(second.getCharacter(), first.getName(), first.getOnHitAnimation());
        }
    }

    void toggleActiveCharacter() {
        if (arenaCharacters.isEmpty()) {
            return;
        }
        int currentIndex = arenaCharacters.indexOf(activeCharacter);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % arenaCharacters.size();
        activeCharacter = arenaCharacters.get(nextIndex);
        Vector3 activePosition = activeCharacter.runtime.getPosition();
        log("camera target switched to " + activeCharacter.runtime.meshObject.name
                + " x=" + activePosition.x + " z=" + activePosition.z);
    }

    void toggleHitboxDebug() {
        debugHitboxesVisible = !debugHitboxesVisible;
        for (BattleArenaHitboxDebugGameObject debugHitbox : debugHitboxes) {
            debugHitbox.setEnabled(debugHitboxesVisible);
        }
        skillSystem.setDebugVisible(debugHitboxesVisible);
        log("hitbox debug visible=" + debugHitboxesVisible);
    }

    BattleArenaControlledCharacter getActiveCharacter() {
        return activeCharacter;
    }

    private int loadCharacterTexture(GraphicsDevice graphicsDevice, BattleArenaCharacterDefinition definition) {
        String texturePath = BattleArenaEnvironmentLoader.resolveResourcePath(definition.model.texture);
        int texture = graphicsDevice.loadTexture(texturePath);
        log("model texture path=" + texturePath + " textureId=" + texture);
        return texture;
    }

    private String resolveHitboxTrackPath(BattleArenaCharacterDefinition definition) {
        if (definition != null && definition.hitboxTracks != null && !definition.hitboxTracks.trim().isEmpty()) {
            return definition.hitboxTracks;
        }
        return "battle_arena/defeated.hitbox_tracks.json";
    }

    private BattleArenaControlledCharacter spawnCharacter(Scene scene,
                                                          GraphicsDevice graphicsDevice,
                                                          WeightedGeometry weightedGeometry,
                                                          BattleArenaCharacterDefinition definition,
                                                          String meshName,
                                                          String healthBarName,
                                                          int texture,
                                                          float startX,
                                                          String playerId,
                                                          BattleArenaCharacterBrain brain,
                                                          boolean playerControlled) {
        BattleArenaCharacterController controller = new BattleArenaCharacterController();
        controller.setTerrainHeightSampler(this::sampleTerrainHeight);
        BattleArenaCharacterAssembly assembly = characterAssembler.assembleCharacter(
                scene,
                graphicsDevice,
                weightedGeometry,
                definition,
                meshName,
                texture,
                PLAYER_SCALE,
                activeAnimations);
        BattleArenaCharacterRuntime runtime = new BattleArenaCharacterRuntime(
                controller,
                assembly,
                definition,
                hitboxTracks,
                activeAnimations);
        BattleArenaControlledCharacter character = new BattleArenaControlledCharacter(
                runtime,
                controller,
                skillSystem.skills(),
                playerId,
                brain,
                playerControlled);
        if (startX != 0f) {
            controller.setPlayerPosition(startX, 0f, 0f);
        }
        runtime.syncRig();
        log("spawned character runtime mesh=" + runtime.meshObject.name
                + " playerId=" + playerId
                + " playerControlled=" + playerControlled
                + " texture=" + runtime.meshObject.texture
                + " startPosition=" + runtime.getPosition().x + ","
                + runtime.getPosition().y + ","
                + runtime.getPosition().z
                + " bones=" + runtime.bones.size()
                + " hitboxes=" + runtime.getHitboxColliders().size());
        playerMeshes.add(runtime.meshObject);
        BattleArenaHealthBarGameObject healthBar = new BattleArenaHealthBarGameObject(
                character,
                scene.renderer.camera,
                healthBarName,
                HEALTH_BAR_WIDTH,
                HEALTH_BAR_HEIGHT,
                HEALTH_BAR_VERTICAL_OFFSET);
        character.healthBarObject = healthBar;
        scene.addGameObject(healthBar);
        arenaCharacters.add(character);
        return character;
    }

    private void spawnTcpCharacter(Scene scene,
                                   GraphicsDevice graphicsDevice,
                                   BattleArenaTcpControlClient tcpControlClient,
                                   String player,
                                   boolean playerControlled) {
        int spawnSlot = resolveSpawnSlot(player);
        float startX = spawnSlot == 0 ? 0f : SECOND_CHARACTER_START_X * spawnSlot;
        String safeName = player.replaceAll("[^A-Za-z0-9_]", "_");
        BattleArenaCharacterBrain brain = playerControlled
                ? null
                : new BattleArenaTcpRemoteController(tcpControlClient, player);
        BattleArenaControlledCharacter character = spawnCharacter(
                scene,
                graphicsDevice,
                characterGeometry,
                characterDefinition,
                "BattleArena_" + safeName,
                "BattleArena_" + safeName + "_HealthBar",
                characterTexture,
                startX,
                player,
                brain,
                playerControlled);
        registerCharacterHitboxes(scene, character.runtime);
        charactersByPlayer.put(player, character);
        if (playerControlled) {
            playerCharacter = character;
            activeCharacter = character;
        } else {
            npcCharacters.add(character);
        }
        log("spawned tcp character player=" + player
                + " local=" + playerControlled
                + " x=" + startX
                + " bones=" + character.runtime.bones.size());
        logAnimationSummary(character);
    }

    private int resolveSpawnSlot(String player) {
        int numericPlayerId = parseTrailingPlayerNumber(player);
        if (numericPlayerId > 0) {
            return numericPlayerId - 1;
        }
        return charactersByPlayer.size();
    }

    private int parseTrailingPlayerNumber(String player) {
        if (player == null) {
            return -1;
        }
        int dashIndex = player.lastIndexOf('-');
        if (dashIndex < 0 || dashIndex >= player.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(player.substring(dashIndex + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void despawnCharacter(Scene scene, String player) {
        BattleArenaControlledCharacter character = charactersByPlayer.remove(player);
        removeCharacterFromScene(scene, character);
    }

    private void removeCharacterFromScene(Scene scene, BattleArenaControlledCharacter character) {
        if (scene == null || character == null) {
            return;
        }
        for (Collider collider : character.runtime.getHitboxColliders()) {
            scene.getCollisionWorld().removeCollider(collider);
        }
        Iterator<BattleArenaHitboxDebugGameObject> debugIterator = debugHitboxes.iterator();
        while (debugIterator.hasNext()) {
            BattleArenaHitboxDebugGameObject debugObject = debugIterator.next();
            if (debugObject.getCollider().getCharacter() == character.runtime) {
                scene.removeGameObject(debugObject);
                debugIterator.remove();
            }
        }
        scene.removeGameObject(character.runtime.meshObject);
        if (character.healthBarObject != null) {
            scene.removeGameObject(character.healthBarObject);
        }
        playerMeshes.remove(character.runtime.meshObject);
        arenaCharacters.remove(character);
        npcCharacters.remove(character);
        if (playerCharacter == character) {
            playerCharacter = null;
        }
        if (activeCharacter == character) {
            activeCharacter = playerCharacter != null
                    ? playerCharacter
                    : (!arenaCharacters.isEmpty() ? arenaCharacters.get(0) : null);
        }
        log("despawned tcp character player=" + character.playerId);
    }

    private void logAnimationSummary(BattleArenaControlledCharacter character) {
        if (character == null) {
            return;
        }
        log("wired animation count total=" + activeAnimations.size()
                + " idle=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_IDLE).size()
                + " walk=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_WALK).size()
                + " walkBackward=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_WALK_BACKWARD).size()
                + " run=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_RUN).size()
                + " jump=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_JUMP).size()
                + " punch=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_PUNCH).size()
                + " hit=" + character.runtime.animationSet(BattleArenaCharacterController.ANIM_TAKE_HIT).size());
    }

    private void updateFireballCasting(BattleArenaSkillContext skillContext) {
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            character.castLatched = updateFireballCasting(
                    skillContext,
                    character,
                    resolveOpponent(character),
                    character.castLatched);
        }
    }

    private void updateControlTriggeredSkills(BattleArenaControlledCharacter character,
                                              BattleArenaSkillContext skillContext) {
        if (character == null) {
            return;
        }
        if (character.controls.castMudWallPressed) {
            character.runSkill(BattleArenaMudWallSkill.ID, skillContext, resolveOpponent(character));
        }
    }

    private void sendLocalControls(BattleArenaTcpControlClient tcpControlClient, BattleArenaControlledCharacter character) {
        if (tcpControlClient == null || character == null || !character.playerControlled) {
            return;
        }
        tcpControlClient.sendControls(character.playerId, character.controls);
    }

    private boolean updateFireballCasting(BattleArenaSkillContext skillContext,
                                          BattleArenaControlledCharacter caster,
                                          BattleArenaCharacterRuntime target,
                                          boolean castLatched) {
        if (caster == null) {
            return false;
        }
        boolean casting = caster.runtime.isCasting();
        if (casting && !castLatched) {
            caster.runSkill(BattleArenaFireballSkill.ID, skillContext, target);
        }
        return casting;
    }

    private void registerCharacterHitboxes(Scene scene, BattleArenaCharacterRuntime character) {
        for (Collider collider : character.getHitboxColliders()) {
            scene.getCollisionWorld().addCollider(collider);
            if (collider instanceof BattleArenaHitboxCollider) {
                BattleArenaHitboxDebugGameObject debugObject =
                        new BattleArenaHitboxDebugGameObject((BattleArenaHitboxCollider) collider, character.meshObject.texture);
                debugObject.setEnabled(debugHitboxesVisible);
                debugHitboxes.add(debugObject);
                scene.addGameObject(debugObject);
            }
        }
        log("registered hitboxes for " + character.meshObject.name + " count=" + character.getHitboxColliders().size());
    }

    private void updateSideStepFacing(BattleArenaCharacterRuntime self, BattleArenaCharacterRuntime opponent) {
        if (self == null || opponent == null || (!self.isSideSteppingLeft() && !self.isSideSteppingRight())) {
            return;
        }
        self.faceTowards(opponent);
    }

    private void logHit(BattleArenaHitboxCollider attacker,
                        BattleArenaHitboxCollider defender,
                        CollisionEvent event) {
        log("HIT "
                + attacker.getCharacter().meshObject.name
                + " -> "
                + defender.getCharacter().meshObject.name
                + " hitbox=" + defender.getName()
                + " contact=" + event.getManifold().getContactPoint());
    }

    private ArrayList<KeyframeAnimation> collectActiveAnimations(BattleArenaCharacterRuntime runtime) {
        ArrayList<KeyframeAnimation> activeSkeletonAnimations = new ArrayList<>();
        for (ArrayList<KeyframeAnimation> animationSet : runtime.animationSets.values()) {
            for (KeyframeAnimation animation : animationSet) {
                if (animation != null && animation.active && !activeSkeletonAnimations.contains(animation)) {
                    activeSkeletonAnimations.add(animation);
                }
            }
        }
        return activeSkeletonAnimations;
    }

    private BattleArenaCharacterRuntime resolveOpponent(BattleArenaControlledCharacter self) {
        if (self == null) {
            return null;
        }
        if (self != playerCharacter && playerCharacter != null) {
            return playerCharacter.runtime;
        }
        if (!npcCharacters.isEmpty()) {
            return npcCharacters.get(0).runtime;
        }
        for (BattleArenaControlledCharacter character : arenaCharacters) {
            if (character != self) {
                return character.runtime;
            }
        }
        return null;
    }

    private BattleArenaCharacterRuntime primaryOpponentRuntime() {
        return resolveOpponent(playerCharacter);
    }

    private BattleArenaSkillContext createSkillContext(Scene scene, BattleArenaSkillContext.TerrainSampler terrainSampler) {
        return new BattleArenaSkillContext(
                scene,
                terrainSampler,
                debugHitboxesVisible);
    }

    private float sampleTerrainHeight(float worldX, float worldZ) {
        return terrainSampler != null ? terrainSampler.sample(worldX, worldZ) : 0f;
    }

    private static void log(String message) {
        BattleArenaDemoLoader.log(message);
    }
}
