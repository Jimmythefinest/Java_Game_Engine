package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TetraLoader implements SceneLoader {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final float CELL_SIZE = 1.0f;
    private static final float BLOCK_SCALE = 1.0f;
    private static final float DROP_INTERVAL_MS = 650f;
    private static final float CAMERA_HEIGHT = 10.5f;
    private static final float CAMERA_DISTANCE = 16.0f;
    private static final float BOARD_Z = 0f;

    private static final PieceTemplate[] PIECES = new PieceTemplate[] {
            new PieceTemplate(0, "I", new int[][] { { -2, 0 }, { -1, 0 }, { 0, 0 }, { 1, 0 } },
                    new byte[] { 80, (byte) 220, (byte) 255, (byte) 255 }),
            new PieceTemplate(1, "O", new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } },
                    new byte[] { (byte) 255, (byte) 210, 80, (byte) 255 }),
            new PieceTemplate(2, "T", new int[][] { { -1, 0 }, { 0, 0 }, { 1, 0 }, { 0, 1 } },
                    new byte[] { (byte) 180, 110, (byte) 255, (byte) 255 }),
            new PieceTemplate(3, "L", new int[][] { { -1, 0 }, { 0, 0 }, { 1, 0 }, { 1, 1 } },
                    new byte[] { (byte) 255, (byte) 140, 70, (byte) 255 }),
            new PieceTemplate(4, "J", new int[][] { { -1, 1 }, { -1, 0 }, { 0, 0 }, { 1, 0 } },
                    new byte[] { 90, (byte) 140, (byte) 255, (byte) 255 }),
            new PieceTemplate(5, "S", new int[][] { { -1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } },
                    new byte[] { 110, (byte) 235, 110, (byte) 255 }),
            new PieceTemplate(6, "Z", new int[][] { { -1, 1 }, { 0, 1 }, { 0, 0 }, { 1, 0 } },
                    new byte[] { (byte) 255, 95, 120, (byte) 255 })
    };

    private final Random random = new Random(7L);
    private final CubeGeometry cubeGeometry = new CubeGeometry();
    private final boolean[][] occupied = new boolean[BOARD_HEIGHT][BOARD_WIDTH];
    private final GameObject[][] settledBlocks = new GameObject[BOARD_HEIGHT][BOARD_WIDTH];
    private final Vector3 boardOrigin = new Vector3(-((BOARD_WIDTH - 1) * CELL_SIZE) * 0.5f, 0f, BOARD_Z);

    private Scene scene;
    private GraphicsDevice graphicsDevice;
    private int boardTexture;
    private int skyTexture;
    private int[] pieceTextures;
    private ActivePiece activePiece;
    private long lastDropAt;

    @Override
    public void load(Scene scene) {
        this.scene = scene;
        this.graphicsDevice = scene.renderer.getGraphicsDevice();

        boardTexture = createSolidTexture(new byte[] { 52, 56, 64, (byte) 255 });
        skyTexture = createSolidTexture(new byte[] { (byte) 185, (byte) 215, (byte) 255, (byte) 255 });
        pieceTextures = new int[PIECES.length];
        for (int i = 0; i < PIECES.length; i++) {
            pieceTextures[i] = createSolidTexture(PIECES[i].rgba);
        }

        buildEnvironment();
        configureCamera();
        spawnPiece();
        lastDropAt = System.currentTimeMillis();

        scene.animations.add(new Animation() {
            @Override
            public void animate() {
                updateFallingPiece();
            }
        });
    }

    public void moveActiveLeft() {
        moveActiveHorizontally(-1);
    }

    public void moveActiveRight() {
        moveActiveHorizontally(1);
    }

    public void rotateActivePieceClockwise() {
        rotateActivePiece(true);
    }

    public void rotateActivePieceCounterClockwise() {
        rotateActivePiece(false);
    }

    public void softDropActivePiece() {
        if (activePiece == null) {
            return;
        }
        lastDropAt = System.currentTimeMillis();
        if (canPlace(activePiece.cells, activePiece.gridX, activePiece.gridY - 1)) {
            activePiece.gridY -= 1;
            applyPieceTransform(activePiece);
            return;
        }
        lockActivePiece();
        clearFilledRows();
        spawnPiece();
    }

    private void rotateActivePiece(boolean clockwise) {
        if (activePiece == null) {
            return;
        }
        int[][] rotated = clockwise ? rotateCellsClockwise(activePiece.cells) : rotateCellsCounterClockwise(activePiece.cells);
        if (!canPlace(rotated, activePiece.gridX, activePiece.gridY)) {
            return;
        }
        activePiece.cells = rotated;
        applyPieceTransform(activePiece);
    }

    private void buildEnvironment() {
        GameObject skybox = new GameObject(new SphereGeometry(1f, 18, 18), skyTexture);
        skybox.ambientlight_multiplier = 4.5f;
        skybox.shininess = 1f;
        skybox.setScale(160f, 160f, 160f);
        skybox.setPosition(0f, 20f, 0f);
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        GameObject backboard = new GameObject(cubeGeometry, boardTexture);
        backboard.ambientlight_multiplier = 1.6f;
        backboard.shininess = 4f;
        backboard.setScale(BOARD_WIDTH * CELL_SIZE * 0.57f, BOARD_HEIGHT * CELL_SIZE * 0.52f, 0.12f);
        backboard.setPosition(0f, (BOARD_HEIGHT - 1) * CELL_SIZE * 0.5f, BOARD_Z - 0.62f);
        scene.addGameObject(backboard);

        GameObject floor = new GameObject(cubeGeometry, boardTexture);
        floor.ambientlight_multiplier = 1.25f;
        floor.shininess = 3f;
        floor.setScale(BOARD_WIDTH * CELL_SIZE * 0.57f, 0.18f, 1.0f);
        floor.setPosition(0f, -0.8f, BOARD_Z);
        scene.addGameObject(floor);
    }

    private void configureCamera() {
        Vector3 focus = new Vector3(0f, BOARD_HEIGHT * CELL_SIZE * 0.42f, BOARD_Z);
        Vector3 eye = new Vector3(0f, CAMERA_HEIGHT, -CAMERA_DISTANCE);
        scene.renderer.camera.lookAt(eye, focus, new Vector3(0f, 1f, 0f));
    }

    private void updateFallingPiece() {
        if (activePiece == null) {
            spawnPiece();
            return;
        }

        long now = System.currentTimeMillis();
        if ((now - lastDropAt) < DROP_INTERVAL_MS) {
            return;
        }
        lastDropAt = now;

        if (canPlace(activePiece.cells, activePiece.gridX, activePiece.gridY - 1)) {
            activePiece.gridY -= 1;
            applyPieceTransform(activePiece);
            return;
        }

        lockActivePiece();
        clearFilledRows();
        spawnPiece();
    }

    private void moveActiveHorizontally(int deltaX) {
        if (activePiece == null) {
            return;
        }
        int nextX = activePiece.gridX + deltaX;
        if (!canPlace(activePiece.cells, nextX, activePiece.gridY)) {
            return;
        }
        activePiece.gridX = nextX;
        applyPieceTransform(activePiece);
    }

    private void spawnPiece() {
        for (int attempt = 0; attempt < 24; attempt++) {
            PieceTemplate template = PIECES[random.nextInt(PIECES.length)];
            int spawnX = BOARD_WIDTH / 2;
            int spawnY = BOARD_HEIGHT - 2;
            if (canPlace(template.cells, spawnX, spawnY)) {
                activePiece = createActivePiece(template, spawnX, spawnY);
                return;
            }
        }

        resetBoard();
        spawnPiece();
    }

    private ActivePiece createActivePiece(PieceTemplate template, int gridX, int gridY) {
        int texture = pieceTextures[template.index];
        List<GameObject> blocks = new ArrayList<>();
        for (int i = 0; i < template.cells.length; i++) {
            GameObject block = new GameObject(cubeGeometry, texture);
            block.ambientlight_multiplier = 1.2f;
            block.shininess = 8f;
            block.setScale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
            scene.addGameObject(block);
            blocks.add(block);
        }
        ActivePiece piece = new ActivePiece(template.cells, blocks, gridX, gridY);
        applyPieceTransform(piece);
        return piece;
    }

    private void applyPieceTransform(ActivePiece piece) {
        for (int i = 0; i < piece.cells.length; i++) {
            int[] cell = piece.cells[i];
            Vector3 world = gridToWorld(piece.gridX + cell[0], piece.gridY + cell[1]);
            GameObject block = piece.blocks.get(i);
            block.setPosition(world.x, world.y, world.z);
            block.setRotation(0f, 0f, 0f);
        }
    }

    private void lockActivePiece() {
        for (int i = 0; i < activePiece.cells.length; i++) {
            int[] cell = activePiece.cells[i];
            int x = activePiece.gridX + cell[0];
            int y = activePiece.gridY + cell[1];
            occupied[y][x] = true;
            GameObject block = activePiece.blocks.get(i);
            Vector3 world = gridToWorld(x, y);
            block.setPosition(world.x, world.y, world.z);
            settledBlocks[y][x] = block;
        }
        activePiece = null;
    }

    private void clearFilledRows() {
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            if (!isRowFull(y)) {
                continue;
            }
            clearRow(y);
            collapseAbove(y);
            y -= 1;
        }
    }

    private boolean isRowFull(int y) {
        for (int x = 0; x < BOARD_WIDTH; x++) {
            if (!occupied[y][x]) {
                return false;
            }
        }
        return true;
    }

    private void clearRow(int y) {
        for (int x = 0; x < BOARD_WIDTH; x++) {
            occupied[y][x] = false;
            GameObject block = settledBlocks[y][x];
            if (block == null) {
                continue;
            }
            scene.removeGameObject(block);
            block.cleanup();
            settledBlocks[y][x] = null;
        }
    }

    private void collapseAbove(int clearedY) {
        for (int y = clearedY; y < BOARD_HEIGHT - 1; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                occupied[y][x] = occupied[y + 1][x];
                settledBlocks[y][x] = settledBlocks[y + 1][x];
                if (settledBlocks[y][x] != null) {
                    Vector3 world = gridToWorld(x, y);
                    settledBlocks[y][x].setPosition(world.x, world.y, world.z);
                }
            }
        }

        int top = BOARD_HEIGHT - 1;
        for (int x = 0; x < BOARD_WIDTH; x++) {
            occupied[top][x] = false;
            settledBlocks[top][x] = null;
        }
    }

    private void resetBoard() {
        if (activePiece != null) {
            for (GameObject block : activePiece.blocks) {
                scene.removeGameObject(block);
                block.cleanup();
            }
            activePiece = null;
        }

        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                occupied[y][x] = false;
                GameObject block = settledBlocks[y][x];
                if (block == null) {
                    continue;
                }
                scene.removeGameObject(block);
                block.cleanup();
                settledBlocks[y][x] = null;
            }
        }
    }

    private boolean canPlace(int[][] cells, int anchorX, int anchorY) {
        for (int[] cell : cells) {
            int x = anchorX + cell[0];
            int y = anchorY + cell[1];
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return false;
            }
            if (occupied[y][x]) {
                return false;
            }
        }
        return true;
    }

    private Vector3 gridToWorld(int x, int y) {
        return new Vector3(boardOrigin.x + (x * CELL_SIZE), y * CELL_SIZE, boardOrigin.z);
    }

    private int createSolidTexture(byte[] rgba) {
        return graphicsDevice.createTextureRGBA(1, 1, rgba);
    }

    private static final class PieceTemplate {
        private final int index;
        private final String name;
        private final int[][] cells;
        private final byte[] rgba;

        private PieceTemplate(int index, String name, int[][] cells, byte[] rgba) {
            this.index = index;
            this.name = name;
            this.cells = copyCells(cells);
            this.rgba = rgba;
        }
    }

    private static final class ActivePiece {
        private int[][] cells;
        private final List<GameObject> blocks;
        private int gridX;
        private int gridY;

        private ActivePiece(int[][] cells, List<GameObject> blocks, int gridX, int gridY) {
            this.cells = copyCells(cells);
            this.blocks = blocks;
            this.gridX = gridX;
            this.gridY = gridY;
        }
    }

        private static int[][] rotateCellsClockwise(int[][] source) {
        int[][] rotated = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            rotated[i] = new int[] { source[i][1], -source[i][0] };
        }
        return rotated;
    }

    private static int[][] rotateCellsCounterClockwise(int[][] source) {
        int[][] rotated = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            rotated[i] = new int[] { -source[i][1], source[i][0] };
        }
        return rotated;
    }

    private static int[][] copyCells(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = new int[] { source[i][0], source[i][1] };
        }
        return copy;
    }
}
