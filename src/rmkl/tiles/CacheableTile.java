package rmkl.tiles;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public abstract class CacheableTile extends Tile {

    private boolean cached;

    public final void setCached() {
        cached = true;
    }

    @Override
    public final void render(ModelBatch modelBatch, Environment environment) {
        // if the modelInstance is cached the render is handled by the World instance
        if(!cached) modelBatch.render(modelInstance, environment);
    }
}
