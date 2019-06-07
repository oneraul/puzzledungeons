package rmkl.tiles;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.*;
import com.esotericsoftware.kryonet.Server;
import rmkl.Pj3D;
import rmkl.Camera3D;
import rmkl.PjConnection;

public abstract class Tile {

    public static final char empty = ' ', spawnCode = 'S';
    protected ModelInstance modelInstance;

    public static void load(AssetManager assets) {
        Error.model = assets.get("assets/tiles/error.g3dj", Model.class);
        Floor.model = assets.get("assets/tiles/floor.g3dj", Model.class);
        Wall.model = assets.get("assets/tiles/wall.g3dj", Model.class);
        Fence.model = assets.get("assets/tiles/fence.g3dj", Model.class);
        Teleporter.model = assets.get("assets/tiles/teleporter.g3dj", Model.class);
        Door.model = assets.get("assets/tiles/door.g3dj", Model.class);
        Button.model = assets.get("assets/tiles/button.g3dj", Model.class);
        TWall.model = assets.get("assets/tiles/twall.g3dj", Model.class);

        Fence.load();
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(modelInstance, environment);
    }

    public void serverCollisionResponse(Server server, PjConnection c) {}

    public abstract void collisionResponse(Pj3D pj);
    public abstract void cameraCollision(Pj3D pj, Camera3D cam);
    public abstract char type();
    public void update(float dt) {}
}