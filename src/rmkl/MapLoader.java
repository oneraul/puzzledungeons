package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.GridPoint2;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import rmkl.tiles.*;
import rmkl.tiles.descriptors.*;
import rmkl.tiles.descriptors.buttonActionsDescriptors.*;

abstract class BaseMapLoader {

    static char[][] loadMap(final String path) throws IOException {
        /* extract lines from map file */
        ArrayList<String> lines = new ArrayList<>();
        lines.addAll(Files.readAllLines(Paths.get(path)));

        /* search map size */
        int number_of_lines = lines.size();
        int longest_line = 0;
        for(String line : lines) {
            if(line.length() > longest_line) longest_line = line.length();
        }

        /* ArrayList<String> to char[][] */
        final char[][] map = new char[longest_line][number_of_lines];
        for(int row = 0; row < number_of_lines; row++) {
            char[] line = lines.get(row).toCharArray();

            for(int column = 0; column < line.length; column++) {
                map[column][row] = line[column];
            }
        }

        return map;
    }

    static Tile[][] parseMap(final char[][] map, final TileBuilder tileBuilder) {

        final Tile[][] grid = new Tile[map.length][map[0].length];

        for(int x = 0; x < map.length; x++) {
            for(int y = 0; y < map[x].length; y++) {
                if(map[x][y] == Tile.empty) continue;

                char type = map[x][y];
                grid[x][y] = tileBuilder.build(type, x, y, map);
            }
        }

        return grid;
    }

    /** @param x, y are in world space */
    static MapChunk parseChunk(final char[][] map, final Tile[][] grid, float x, float y, Pool<MapChunk> chunksPool, final TileBuilder tileBuilder) {

        // set the chunk coordinates and size
        // if the chunk is the last, set the size accordingly (might not be full size)

        MapChunk chunk = chunksPool.obtain();
        chunk.x = (int)(x / MapChunk.size);
        chunk.y = (int)(y / MapChunk.size);

        int fullChunksX = map.length / MapChunk.size;
        int width = chunk.x < fullChunksX ? MapChunk.size : map.length%MapChunk.size;
        int fullChunksY = map[0].length / MapChunk.size;
        int height = chunk.y < fullChunksY ? MapChunk.size : map[0].length%MapChunk.size;

        // load the tiles in the chunk
        chunk.grid = new Tile[width][height];

        int left = chunk.x * MapChunk.size;
        int right = left + width;
        int top = chunk.y * MapChunk.size;
        int bottom = top + height;

        for(int i = left; i < right; i++) {
            for(int j = top; j < bottom; j++) {
                int m = i-left, n = j-top; // translate from world space to chunk space

                if(grid[i][j] != null) chunk.grid[m][n] = grid[i][j];
                else {
                    char type = map[i][j];
                    if(type != Tile.empty) {
                        chunk.grid[m][n] = tileBuilder.build(type, i, j, map);
                    }
                }
            }
        }

        // set ModelCaches
        chunk.floorCache = null;
        chunk.wallsCache = null;
        chunk.fencesCache = null;
        Gdx.app.postRunnable(() -> {

            chunk.floorCache  = MapChunk.cacheModelInstances(chunk, Floor.code, left, right, top, bottom);
            chunk.wallsCache  = MapChunk.cacheModelInstances(chunk, Wall.code,  left, right, top, bottom);
            chunk.fencesCache = MapChunk.cacheModelInstances(chunk, Fence.code, left, right, top, bottom);

        });

        return chunk;
    }

    protected static void deserializeEnvironment(JsonValue value, Environment environment) {

        float ambientIntensity = value.getFloat("ambient");
        float dirX = value.getFloat("dirX");
        float dirY = value.getFloat("dirY");
        float dirZ = value.getFloat("dirZ");

        environment.clear();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, ambientIntensity));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, dirX, dirY, dirZ));
    }

}

class GameMapLoader extends BaseMapLoader {

    static void loadCommands(final String commandsPath, World world) throws IOException {

        JsonReader reader = new JsonReader();
        JsonValue mapCommands = reader.parse(new FileInputStream(commandsPath));

        deserializeEnvironment(mapCommands.get("environment"), world.environment);

        for(int i = 0; i < mapCommands.size; i++) {
            JsonValue value = mapCommands.get(i);

            char type = value.getChar("type", '\0');
            if(type == '\0') continue;

            switch(type) {
                case Button.code: deserializeButton(value, world.grid); break;
                case Teleporter.code: deserializeTeleporter(value, world.grid); break;
                case Tile.spawnCode: deserializeSpawn(value, world.spawnSnapshot); break;
                case Door.code: deserializeDoor(value, world.grid); break;
            }
        }
    }

    private static void deserializeButton(JsonValue value, Tile[][] grid) {
        int x = value.getInt("x");
        int y = value.getInt("y");
        Button button = (Button)grid[x][y];


        if(!value.has("modes")) return;
        JsonValue modes = value.get("modes");

        for(JsonValue modeValue : modes.iterator()) {
            if(!modeValue.has("mode")) continue;

            String modeStr = modeValue.getString("mode");
            Button.ButtonAction mode = null;

            switch(modeStr) {
                case "SwitchDoor":
                case "CloseDoor":
                case "OpenDoor": {
                    int doorX = modeValue.getInt("doorX");
                    int doorY = modeValue.getInt("doorY");

                    if(doorX < 0 || doorX >= grid.length || doorY < 0 || doorY >= grid[x].length) continue; // out of bounds
                    Door door = (Door) grid[doorX][doorY];

                    switch (modeStr) {
                        case "SwitchDoor": mode = button.new SwitchDoor(door); break;
                        case "CloseDoor": mode = button.new CloseDoor(door); break;
                        case "OpenDoor": mode = button.new OpenDoor(door); break;
                    }
                    break;
                }
            }
            if(mode != null) button.addMode(mode);
        }
    }

    private static void deserializeTeleporter(JsonValue value, Tile[][] grid) {

        int x = value.getInt("x");
        int y = value.getInt("y");
        if(x < 0 || x >= grid.length || y < 0 || y >= grid[x].length) return; // out of bounds

        int targetX = value.getInt("targetX", -1);
        int targetY = value.getInt("targetY", -1);
        if(targetX < 0 || targetX >= grid.length || targetY < 0 || targetY >= grid[x].length) return; // target out of bounds

        Teleporter teleporter = (Teleporter)grid[x][y];

        teleporter.angle = value.getInt("angle", 0);
        teleporter.addToTeleportersList(new GridPoint2(x, y), new GridPoint2(targetX, targetY));
    }

    private static void deserializeSpawn(JsonValue value, Network.Snapshot spawn) {
        int x = value.getInt("x");
        int y = value.getInt("y");
        spawn.pos = new Vector2(x, y).add(0.5f, 0.5f);
        spawn.angle = value.getInt("angle", 0);
    }

    private static void deserializeDoor(JsonValue value, Tile[][] grid) {
        int x = value.getInt("x");
        int y = value.getInt("y");
        boolean locked = value.getBoolean("locked");

        Door door = (Door)grid[x][y];
        door.setLocked(locked);
    }
}

class EditorMapLoader extends BaseMapLoader {

    static void loadCommands(final String path, ObjectMap<GridPoint2, TileDescriptor> digits, Environment environment, SpawnDescriptor spawnDescriptor) throws IOException {

        JsonReader reader = new JsonReader();
        JsonValue mapCommands = reader.parse(new FileInputStream(path));

        deserializeEnvironment(mapCommands.get("environment"), environment);

        for(int i = 0; i < mapCommands.size; i++) {
            JsonValue value = mapCommands.get(i);

            char type = value.getChar("type", '\0');
            if(type == '\0') continue;

            TileDescriptor descriptor = null;
            switch(type) {
                case Button.code: descriptor = deserializeButton(value); break;
                case Teleporter.code: descriptor = deserializeTeleporter(value); break;
                case Door.code: descriptor = deserializeDoor(value); break;
                case Tile.spawnCode:
                    SpawnDescriptor d = deserializeSpawn(value);
                    spawnDescriptor.x = d.x;
                    spawnDescriptor.z = d.z;
                    spawnDescriptor.angle = d.angle;
                    spawnDescriptor.setModelInstance();
                    continue; // continue instead of break so it won't add the descriptor to digits
            }

            if(descriptor != null) {
                digits.put(new GridPoint2(descriptor.x, descriptor.z), descriptor);
            }
        }
    }

    static private TileDescriptor deserializeButton(JsonValue value) {

        ButtonDescriptor descriptor = new ButtonDescriptor();
        descriptor.x = value.getInt("x");
        descriptor.z = value.getInt("y");

        if(value.has("modes")) {
            JsonValue modes = value.get("modes");

            for(JsonValue modeValue : modes.iterator()) {
                if(!modeValue.has("mode")) continue;

                String modeStr = modeValue.getString("mode");
                ButtonActionDescriptor mode = null;

                switch(modeStr) {
                    case "SwitchDoor":
                    case "CloseDoor":
                    case "OpenDoor": {
                        switch(modeStr) {
                            case "SwitchDoor": mode = new SwitchDoorDescriptor(descriptor); break;
                            case "CloseDoor": mode = new CloseDoorDescriptor(descriptor); break;
                            case "OpenDoor": mode = new OpenDoorDescriptor(descriptor); break;
                        }

                        ((DoorButtonDescriptor)mode).doorX = modeValue.getInt("doorX", -1);
                        ((DoorButtonDescriptor)mode).doorY = modeValue.getInt("doorY", -1);
                        break;
                    }
                }
                if(mode != null) descriptor.modes.add(mode);
            }
        }

        descriptor.setGUIModes();
        return descriptor;
    }

    static private TileDescriptor deserializeTeleporter(JsonValue value) {
        TeleporterDescriptor descriptor = new TeleporterDescriptor();
        descriptor.x = value.getInt("x");
        descriptor.z = value.getInt("y");
        descriptor.targetX = value.getInt("targetX", -1);
        descriptor.targetY = value.getInt("targetY", -1);
        descriptor.angle = value.getInt("angle", 0);
        descriptor.setModelInstance();
        return descriptor;
    }

    static private SpawnDescriptor deserializeSpawn(JsonValue value) {
        SpawnDescriptor descriptor = new SpawnDescriptor();
        descriptor.x = value.getInt("x");
        descriptor.z = value.getInt("y");
        descriptor.angle = value.getInt("angle");
        return descriptor;
    }

    static private TileDescriptor deserializeDoor(JsonValue value) {
        DoorDescriptor descriptor = new DoorDescriptor();
        descriptor.x = value.getInt("x");
        descriptor.z = value.getInt("y");
        descriptor.locked = value.getBoolean("locked");
        return descriptor;
    }
}
