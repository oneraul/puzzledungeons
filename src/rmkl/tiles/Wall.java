package rmkl.tiles;

import collision.AABB;
import collision.Hit;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Pool;
import rmkl.Camera3D;
import rmkl.Pj3D;

public class Wall extends CacheableTile implements Pool.Poolable {

    public static final char code = 'x';
    static Model model;
    private final AABB collider;

    Wall() {
        collider = new AABB(0, 0, 0.5f, 0.5f);
        if(model != null) modelInstance = new ModelInstance(model);
    }

    Wall init(int x, int y) {
        if(modelInstance != null) modelInstance.transform.setTranslation(x+0.5f, 0, y+0.5f);
        collider.pos.set(x+0.5f, y+0.5f);
        return this;
    }

    @Override
    public void collisionResponse(Pj3D pj) {
        Hit hit = pj.collider.intersectAABB(collider);
        if(hit != null) {
            pj.collider.pos.add(hit.delta);
        }
    }

    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {
        Hit hit = collider.intersectSegment(pj.pos, cam.pos);
        if(hit != null) cam.pos.set(hit.pos);
    }

    @Override
    public char type() {
        return code;
    }

    @Override
    public void reset() {}
}