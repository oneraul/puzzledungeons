package rmkl;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import rmkl.tiles.*;

import java.io.IOException;

class World {

    final char[][] map;
    final Tile[][] grid;
    Network.Snapshot spawnSnapshot;
    final Environment environment;
    final ObjectMap<GridPoint2, Tile> tiles = new ObjectMap<>();
    private final Pool<MapChunk> chunksPool = new Pool<MapChunk>() {
        @Override
        protected MapChunk newObject() {
            return new MapChunk();
        }
    };
    private final TileBuilder tileBuilder;
    private MapChunk[][] chunks = new MapChunk[3][3];

    // server
    World(String path, String fileName, TileBuilder tileBuilder) throws IOException {
        environment = new Environment();
        this.tileBuilder = tileBuilder;
        map = GameMapLoader.loadMap(path + fileName + ".txt");
        grid = GameMapLoader.parseMap(map, tileBuilder);
        spawnSnapshot = new Network.Snapshot();
        GameMapLoader.loadCommands(path + fileName + "_commands.txt", this);
    }

    // editor#load
    World(String path, String fileName) throws IOException {
        environment = new Environment();
        tileBuilder = new TileBuilder();
        map = GameMapLoader.loadMap(path + fileName + ".txt");
        grid = GameMapLoader.parseMap(map, tileBuilder);
        spawnSnapshot = new Network.Snapshot();
        GameMapLoader.loadCommands(path + fileName + "_commands.txt", this);
    }

    // client
    World(char[][] map) {
        environment = new Environment();
        this.map = map;
        tileBuilder = new TileBuilder();
        grid = GameMapLoader.parseMap(map, tileBuilder);
    }

    // editor#new
    World(int width, int height) {
        environment = new Environment();
        tileBuilder = new TileBuilder();
        map = new char[width][height];
        grid = new Tile[map.length][map[0].length];
    }

    void updateTiles(float dt) {
        for(Tile[] t : grid) {
            for(Tile tile : t) {
                if(tile != null) tile.update(dt);
            }
        }
    }

    void draw(ModelBatch modelBatch, PerspectiveCamera cam) {
        for(MapChunk[] c : chunks) {
            for(MapChunk chunk : c) {
                if(chunk != null) {
                    chunk.render(modelBatch, cam, environment);
                }
            }
        }
    }

    //TODO refactor to use chunkWithinBounds for clarity
    void initChunksAround(float x, float y) {

        int centerChunkX = (int)(x / MapChunk.size);
        int centerChunkY = (int)(y / MapChunk.size);

        // these are the index of last item of the array, so size-1
        int totalChunksX = map.length / MapChunk.size;
        int totalChunksY = map[0].length / MapChunk.size;

        //center column
        chunks[1][1] = BaseMapLoader.parseChunk(map, grid, x, y, chunksPool, tileBuilder);
        chunks[1][0] = centerChunkY > 0 ? BaseMapLoader.parseChunk(map, grid, x, y-MapChunk.size, chunksPool, tileBuilder) : null;
        chunks[1][2] = centerChunkY < totalChunksY ? BaseMapLoader.parseChunk(map, grid, x, y+MapChunk.size, chunksPool, tileBuilder) : null;
        //left column
        if(centerChunkX > 0) {
            chunks[0][1] = BaseMapLoader.parseChunk(map, grid, x-MapChunk.size, y, chunksPool, tileBuilder);
            chunks[0][0] = centerChunkY > 0 ? BaseMapLoader.parseChunk(map, grid, x-MapChunk.size, y-MapChunk.size, chunksPool, tileBuilder) : null;
            chunks[0][2] = centerChunkY < totalChunksY ? BaseMapLoader.parseChunk(map, grid, x-MapChunk.size, y+MapChunk.size, chunksPool, tileBuilder) : null;
        }
        //right column
        if(centerChunkX < totalChunksX) {
            chunks[2][1] = BaseMapLoader.parseChunk(map, grid, x+MapChunk.size, y, chunksPool, tileBuilder);
            chunks[2][0] = centerChunkY > 0 ? BaseMapLoader.parseChunk(map, grid, x+MapChunk.size, y-MapChunk.size, chunksPool, tileBuilder) : null;
            chunks[2][2] = centerChunkY < totalChunksY ? BaseMapLoader.parseChunk(map, grid, x+MapChunk.size, y+MapChunk.size, chunksPool, tileBuilder) : null;
        }
    }

    void swapChunks(float pjPosX, float pjPosY) {

        // check the movement direction
        int deltaX = 0, deltaY = 0;
        if(pjPosX > MapChunk.size*(chunks[1][1].x+1))    deltaX =  1;
        else if(pjPosX < MapChunk.size*(chunks[1][1].x)) deltaX = -1;
        if(pjPosY > MapChunk.size*(chunks[1][1].y+1))    deltaY =  1;
        else if(pjPosY < MapChunk.size*(chunks[1][1].y)) deltaY = -1;

        // early exit: no swap needed
        if(deltaX == 0 && deltaY == 0) return;

        int newCenterChunkX = chunks[1][1].x + deltaX;
        int newCenterChunkY = chunks[1][1].y + deltaY;

        // early exit: moved out of the map's bounds
        if(!chunkWithinBounds(newCenterChunkX, newCenterChunkY)) return;

        // actual swap
        final MapChunk[][] newChunks = new MapChunk[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {

                // iterate the chunks array and map the new indices
                int m = i+deltaX, n = j+deltaY;

                // if possible, swap the chunks to their new locations
                if(m >= 0 && m < 3 && n >= 0 && n < 3) {
                    newChunks[i][j] = chunks[m][n];

                // else load the necessary chunks
                } else {
                    int chunkX = newCenterChunkX + (i-1);
                    int chunkY = newCenterChunkY + (j-1);
                    if(chunkWithinBounds(chunkX, chunkY)) {
                        newChunks[i][j] = GameMapLoader.parseChunk(map, grid, chunkX*MapChunk.size, chunkY*MapChunk.size, chunksPool, tileBuilder);
                    }
                }

            }
        }
        chunks = newChunks;
    }

    /** params in chunk coordinates, not world space! */
    private boolean chunkWithinBounds(int chunkX, int chunkY) {
        return chunkX < map.length/MapChunk.size && chunkX >= 0 && chunkY < map[0].length/MapChunk.size && chunkY >= 0;
    }
}
