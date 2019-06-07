package rmkl;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import rmkl.tiles.CacheableTile;
import rmkl.tiles.Tile;

class MapChunk {
    final static int size = 24;

    Tile[][] grid;
    int x, y; //chunk coordinates, not world space!
    ModelCache floorCache;
    ModelCache wallsCache;
    ModelCache fencesCache;

    static ModelCache cacheModelInstances(MapChunk chunk, char tileType, int left, int right, int top, int bottom) {
        ModelCache cache = new ModelCache();
        boolean empty = true;

        cache.begin();
        for (int i = left; i < right; i++) {
            for (int j = top; j < bottom; j++) {
                Tile tile = chunk.grid[i - left][j - top];
                if (tile != null && tile.type() == tileType) {
                    ((CacheableTile)tile).setCached();
                    cache.add(tile.getModelInstance());
                    empty = false;
                }
            }
        }
        cache.end();

        if(empty) return null;
        else return cache;
    }

    void render(ModelBatch modelBatch, PerspectiveCamera cam, Environment environment) {
        if (floorCache  != null) modelBatch.render(floorCache,  environment);
        if (wallsCache  != null) modelBatch.render(wallsCache,  environment);
        if (fencesCache != null) modelBatch.render(fencesCache, environment);

        for (Tile[] t : grid) {
            for (Tile tile : t) {
                if (tile != null && Commons.isVisible(cam, tile.getModelInstance())) {
                    tile.render(modelBatch, environment);
                }
            }
        }
    }
}