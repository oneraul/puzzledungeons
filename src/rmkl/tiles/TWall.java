package rmkl.tiles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import rmkl.Bitmask;
import rmkl.Camera3D;
import rmkl.Pj3D;

public class TWall extends Tile implements AutoTilable {

    public static final char code = 'm';
    static Model model;

    public static Tile build(int x, int y, char[][] map) {
        int bitmask = Bitmask.check8bit(x, y, map, TWall.code, Wall.code);
        int tile = Bitmask.rawTile8bit(bitmask);

        if(tile == 0) {
            System.out.println("ERROR: Wall(" + x + ", " + y + ") -> tile=0");
            return new Error().init(x, y);

        } else return new TWall(x, y, tile);
    }

    private TWall(int x, int z, int tile) {
        if(model != null) {
            int baseTile = Bitmask.baseTile8bit(tile);
            modelInstance = new ModelInstance(model, "Floor", "" + baseTile);
            modelInstance.transform.setToRotation(Vector3.Y, Bitmask.tileRotation8bit(tile));
            modelInstance.transform.setTranslation(x + 0.5f, 0, z + 0.5f);
        }
    }

    @Override
    public void collisionResponse(Pj3D pj) {
    }

    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {
    }

    @Override
    public char type() {
        return code;
    }
}
