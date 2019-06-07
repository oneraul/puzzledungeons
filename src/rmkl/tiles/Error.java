package rmkl.tiles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Pool;
import rmkl.Camera3D;
import rmkl.Pj3D;

class Error extends Tile implements Pool.Poolable{

    public static final char code = 'e';
    static Model model;

    Error() {
        if(model != null) {
            modelInstance = new ModelInstance(model);
        }
    }

    Error init(int x, int y) {
        if(modelInstance != null) modelInstance.transform.setTranslation(x+0.5f, 0, y+0.5f);
        return this;
    }

    @Override
    public void collisionResponse(Pj3D pj) {}
    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {}
    @Override
    public char type() {
        return code;
    }

    @Override
    public void reset() {}
}
