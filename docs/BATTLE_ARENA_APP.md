# Battle Arena App Documentation

This document covers the Battle Arena desktop entry point and its core scene loader:

- `battle-arena-desktop/src/main/java/com/njst/gaming/ri/battlearena/BattleArenaApp.java`
- `battle-arena-core/src/main/java/com/njst/gaming/ri/battlearena/BattleArenaDemoLoader.java`

`BattleArenaApp` owns desktop startup and input binding. `BattleArenaDemoLoader` builds the playable scene, loads assets, manages TCP-controlled characters, updates combat systems, and positions the camera.

## BattleArenaApp

### Class Role

`BattleArenaApp` extends `Engine` and acts as the LWJGL desktop launcher for the battle arena demo. It creates the scene loader, maps keyboard and mouse input to `BattleArenaActions`, installs the loader during engine initialization, and starts the engine from `main`.

### Variables

| Variable | Type | Purpose |
| --- | --- | --- |
| `loader` | `BattleArenaDemoLoader` | Scene loader used by the engine to create and update the Battle Arena scene. |

### Constructors

| Function | Purpose |
| --- | --- |
| `BattleArenaApp()` | Reads TCP host and port from system properties, then delegates to the two-argument constructor. |
| `BattleArenaApp(String tcpControlHost, int tcpControlPort)` | Creates the `BattleArenaDemoLoader` and sets the window title to `Battle Arena`. |

### Functions

| Function | Purpose |
| --- | --- |
| `configureInputBindings(InputBindings bindings)` | Maps desktop keys and mouse input to gameplay actions such as movement, jump, attacks, fireball, mud wall, debug hitboxes, camera look, and target switching. |
| `onInit()` | Assigns `scene.loader = loader` so the engine loads the battle arena scene. |
| `onKey(int key, int action)` | Empty event hook reserved for future key-specific behavior. |
| `main(String[] args)` | Reads optional TCP host and port from JVM properties or command-line arguments, then starts the app. |
| `readPortProperty(String name, int fallback)` | Reads a JVM property and parses it as a TCP port. |
| `parsePort(String value, int fallback)` | Converts a string to an integer port, returning the fallback for null, blank, or invalid input. |

### Input Bindings

| Input | Action |
| --- | --- |
| `W` | `FORWARD` |
| `S` | `BACKWARD` |
| `A` | `ROTATE` |
| `D` | `TURN_LEFT` |
| `Left Shift`, `Right Shift` | `RUN` |
| `Space` | `JUMP` |
| `E` | `PUNCH` |
| `Q` | `KICK` |
| `/` | `FIREBALL` |
| `G` | `MUD_WALL` |
| `X` | `BURST` |
| `Left Arrow` | `STEP_LEFT` |
| `Right Arrow` | `STEP_RIGHT` |
| `9` | `SNAP` |
| `0` | `TOGGLE_HITBOXES` |
| `Left Mouse Button` | `LOOK` |
| Mouse pointer | `LOOK_POINTER` |

## BattleArenaDemoLoader

### Class Role

`BattleArenaDemoLoader` implements `Scene.SceneLoader`. It builds the arena scene, loads terrain, skybox, character mesh data, animation data, texture assets, debug hitboxes, health bars, TCP-controlled characters, audio smoke-test playback, and the per-frame simulation update.

### Constants

| Constant | Purpose |
| --- | --- |
| `LOCAL_PLAYER_ANDROID`, `LOCAL_PLAYER_DESKTOP` | Supported local player identity modes. |
| `DEFAULT_TCP_CONTROL_PORT` | Default remote-control TCP port, currently `7777`. |
| `SKYBOX_FILE`, `GROUND_FILE` | Texture filenames for the skybox and ground. |
| `CHARACTER_DEFINITION_FILE` | Character JSON definition used for mesh, texture, skeleton, animations, and hitboxes. |
| `AUDIO_TEST_FILE` | Audio file used for the current attack/smoke-test sound. |
| `GROUND_SIZE` | Width and depth of the generated flat terrain grid. |
| `CAMERA_DISTANCE`, `CAMERA_HEIGHT`, `PLAYER_FOCUS_HEIGHT` | Third-person camera positioning values. |
| `LOOK_SENSITIVITY`, `MIN_PITCH`, `MAX_PITCH` | Mouse-look tuning and vertical camera clamps. |
| `PLAYER_SCALE` | Character mesh scale applied during assembly. |
| `SECOND_CHARACTER_START_X` | Horizontal spacing used when spawning additional TCP players. |
| `HEALTH_BAR_WIDTH`, `HEALTH_BAR_HEIGHT`, `HEALTH_BAR_VERTICAL_OFFSET` | Health bar display dimensions and placement. |
| `DISABLE_ACTIVE_ANIMATIONS_FOR_PROFILING` | Profiling switch for skipping active skeleton animation updates. |
| `LOG_PREFIX` | Prefix used for Battle Arena console logs. |

### State Variables

| Variable | Type | Purpose |
| --- | --- | --- |
| `terrainGeometry` | `TerrainGeometry` | Generated terrain used for rendering and height sampling. |
| `terrainOrigin` | `Vector3` | World-space origin of the terrain grid. |
| `loadedScene` | `Scene` | Last scene passed into `load`, used when creating skill contexts. |
| `playerMeshes` | `List<GameObject>` | Mesh objects for spawned characters. |
| `debugHitboxes` | `List<BattleArenaHitboxDebugGameObject>` | Toggleable debug render objects for character hitboxes. |
| `characterAssembler` | `BattleArenaCharacterAssembler` | Builds runtime character objects from definitions and geometry. |
| `characterDefinitionLoader` | `BattleArenaCharacterDefinitionLoader` | Loads the character JSON definition. |
| `skillSystem` | `BattleArenaSkillSystem` | Owns skill instances, skill updates, and skill collision handling. |
| `arenaCharacters` | `List<BattleArenaControlledCharacter>` | All active battle arena characters. |
| `npcCharacters` | `List<BattleArenaControlledCharacter>` | Remote or AI-controlled non-local characters. |
| `charactersByPlayer` | `Map<String, BattleArenaControlledCharacter>` | Player ID to character mapping for TCP session synchronization. |
| `playerCharacter` | `BattleArenaControlledCharacter` | Local player-controlled character, when assigned by TCP. |
| `activeCharacter` | `BattleArenaControlledCharacter` | Current camera target. |
| `tcpControlClient` | `BattleArenaTcpControlClient` | Client used to receive player assignments and remote control state. |
| `loadedGraphicsDevice` | `GraphicsDevice` | Graphics device cached for later character spawns. |
| `characterGeometry` | `WeightedGeometry` | Loaded skinned mesh geometry shared by spawned characters. |
| `characterDefinition` | `BattleArenaCharacterDefinition` | Parsed character definition. |
| `characterTexture` | `int` | Texture ID for the character model. |
| `nextSpawnSlot` | `int` | Counter used to place newly spawned TCP characters. |
| `cameraYaw`, `cameraPitch` | `float` | Orbit camera orientation controlled by mouse look. |
| `debugHitboxesVisible` | `boolean` | Whether hitbox and skill debug objects are visible. |
| `activeAnimations` | `ArrayList<KeyframeAnimation>` | Shared list populated as character animations are assembled. |
| `audioTestBuffer` | `AudioBufferHandle` | Loaded smoke-test audio buffer. |
| `audioTestSource` | `AudioSourceHandle` | Source used to play the smoke-test audio. |
| `audioAvailable` | `boolean` | Tracks whether audio loaded and can still be played. |
| `localPlayer` | `String` | Sanitized local player mode from constructor input. |
| `tcpControlHost` | `String` | Host used by the TCP control client. |
| `tcpControlPort` | `int` | Port used by the TCP control client. |

### Constructors

| Function | Purpose |
| --- | --- |
| `BattleArenaDemoLoader()` | Reads local player, TCP host, and TCP port from system properties. |
| `BattleArenaDemoLoader(String localPlayer)` | Uses the provided local player mode with host and port from system properties. |
| `BattleArenaDemoLoader(String localPlayer, String tcpControlHost, int tcpControlPort)` | Normalizes local player mode, host, and port before storing them. |

### Main Load Flow

| Function | Purpose |
| --- | --- |
| `load(Scene scene)` | Resets loader state, creates the TCP client, loads skybox/ground/audio/lights/character assets, registers collision listeners and pointer input, installs the per-frame animation update, and initializes the camera. |
| anonymous `Animation.animate(float deltaSeconds)` | Per-frame update loop: updates TCP state, spawns/despawns characters, handles toggles and attack sound, captures local and remote controls, sends local controls, updates skills, animates skeletons, syncs rigs, and updates the camera. |

### Asset, Audio, and Lighting Functions

| Function | Purpose |
| --- | --- |
| `initAudioSmokeTest(Scene scene)` | Loads `AUDIO_TEST_FILE` and creates a source, disabling audio if loading fails. |
| `playAudioSmokeTest(Scene scene, float gain)` | Plays the smoke-test sound at the requested gain, disabling audio after playback errors. |
| `setupDemoLights(Scene scene)` | Clears existing lights and adds two colored point lights. |
| `loadCharacterAssets(Scene scene, GraphicsDevice graphicsDevice)` | Loads the character definition, weighted geometry, logs mesh statistics, clears animation state, and loads the character texture. |
| `loadCharacterTexture(GraphicsDevice graphicsDevice, BattleArenaCharacterDefinition definition)` | Resolves and loads the texture referenced by the character definition. |
| `resolveTexturePath(String fileName)` | Uses the desktop resource root when the file exists; otherwise returns the asset name for Android-style loading. |

### Character and TCP Functions

| Function | Purpose |
| --- | --- |
| `spawnCharacter(...)` | Assembles a character runtime, creates its controller and health bar, syncs the rig, registers mesh state, and adds it to the arena list. |
| `syncTcpCharacters(Scene scene, GraphicsDevice graphicsDevice)` | Reconciles active TCP players with local runtime characters, spawning new players and removing inactive ones. |
| `spawnTcpCharacter(Scene scene, GraphicsDevice graphicsDevice, String player, boolean playerControlled)` | Creates a player-controlled or remote-controlled TCP character and registers hitboxes. |
| `despawnCharacter(Scene scene, String player)` | Removes a character for a specific TCP player ID. |
| `removeCharacterFromScene(Scene scene, BattleArenaControlledCharacter character)` | Removes colliders, debug hitboxes, mesh, health bar, and list references for a character. |
| `sendLocalControls(BattleArenaControlledCharacter character)` | Sends local player control state through the TCP client. |
| `resolveOpponent(BattleArenaControlledCharacter self)` | Picks the best target runtime for skills and AI. |
| `primaryOpponentRuntime()` | Resolves the local player's primary opponent. |

### Animation and Rig Functions

| Function | Purpose |
| --- | --- |
| `collectActiveSkeletonAnimations()` | Gathers active animation groups for all runtime characters. |
| `collectActiveAnimations(BattleArenaCharacterRuntime runtime)` | Finds unique active animations inside a single character runtime. |
| `logAnimationSummary(BattleArenaControlledCharacter character)` | Logs animation counts for important controller animation sets. |

### Combat, Skills, and Collision Functions

| Function | Purpose |
| --- | --- |
| `updateFireballCasting(BattleArenaSkillContext skillContext)` | Updates fireball cast latches for all arena characters. |
| `updateFireballCasting(BattleArenaSkillContext skillContext, BattleArenaControlledCharacter caster, BattleArenaCharacterRuntime target, boolean castLatched)` | Runs the fireball skill when a character starts casting. |
| `updateControlTriggeredSkills(BattleArenaControlledCharacter character, BattleArenaSkillContext skillContext)` | Runs immediate control-triggered skills such as mud wall. |
| `registerCharacterHitboxes(Scene scene, BattleArenaCharacterRuntime character)` | Adds hitbox colliders to the collision world and creates matching debug objects. |
| `updateSideStepFacing(BattleArenaCharacterRuntime self, BattleArenaCharacterRuntime opponent)` | Keeps side-stepping characters facing their opponent. |
| `handleHitboxCollision(CollisionEvent event)` | Routes mud wall collisions into the skill system and applies hit reactions for hitbox-to-hurtbox contacts. |
| `logHit(BattleArenaHitboxCollider attacker, BattleArenaHitboxCollider defender, CollisionEvent event)` | Logs hit details including attacker, defender, hurtbox name, and contact point. |
| `createSkillContext()` | Creates the shared context object passed to skills and collision handlers. |

### Camera, Terrain, and Utility Functions

| Function | Purpose |
| --- | --- |
| `toggleActiveCharacter()` | Cycles the camera target through active arena characters. |
| `toggleHitboxDebug()` | Toggles hitbox and skill debug visualization. |
| `sampleTerrainHeight(float worldX, float worldZ)` | Bilinearly samples terrain height at a world position. |
| `handlePointerLook(ActionInput actions, PointerState pointer)` | Updates camera yaw and pitch while the look action is held. |
| `updateCamera(Camera camera)` | Positions the camera behind and above the active character and points it at the character focus point. |
| `clampIndex(int value, int max)` | Clamps an index between `0` and `max`. |
| `clamp(float value)` | Clamps a value between `-1` and `1`. |
| `clamp(float value, float min, float max)` | Clamps a value between a custom minimum and maximum. |
| `lerp(float a, float b, float t)` | Linearly interpolates from `a` to `b`. |
| `log(String message)` | Writes a Battle Arena-prefixed message to standard output. |
| `readPortProperty(String name, int fallback)` | Reads and parses a TCP port from a JVM system property. |

## Proposed Improvements

1. Respect `tcpControlHost` in `BattleArenaApp(String tcpControlHost, int tcpControlPort)`. The constructor currently accepts a host but passes `"localhost"` to `BattleArenaDemoLoader`, so command-line and property host selection is effectively ignored.
2. Clarify or remove `localPlayer` in `BattleArenaDemoLoader`. It is stored and normalized, but the current loader flow appears to rely on TCP assignment instead of this field.
3. Split `BattleArenaDemoLoader.load` into smaller setup methods. The method currently resets state, loads assets, builds scene objects, wires input, and installs frame logic in one long flow.
4. Replace the audio smoke test with named gameplay audio events. Attacks currently share `battle_arena_test.wav`, which is useful for validation but not expressive for final gameplay.
5. Move hard-coded tuning values into a config object or character/arena definition. Camera distance, health bar size, spawn spacing, and terrain size are currently compile-time constants.
6. Review the input action names for consistency. For example, `A` is bound to `ROTATE` while `D` is bound to `TURN_LEFT`, which may be intentional but reads asymmetrically.
7. Add lifecycle cleanup for TCP and audio resources if the engine supports scene unloading. The loader creates a TCP client and audio handles but does not show explicit teardown.
8. Add automated smoke tests for parsing TCP port configuration and for terrain height sampling. These are small, stable areas that can be tested without a full renderer.
