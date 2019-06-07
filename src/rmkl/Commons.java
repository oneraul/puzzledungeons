package rmkl;

import collision.AABB;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import rmkl.tiles.Tile;

class Commons {

    private static Vector3 tmpPosition = new Vector3(), tmpDimension = new Vector3(), tmpScale = new Vector3(), tmpCenter = new Vector3();
    private static BoundingBox boundingBox = new BoundingBox();
    private static AABB tmpAABB = new AABB(0,0,0,0);

    static void checkCollisions(Pj3D pj, Tile[][] grid) {

        AABB c = pj.collider;
        c.pos.set(pj.pos);

        int left = (int)(c.pos.x-c.half.x);
        int right = (int)(c.pos.x+c.half.x);
        int bottom = (int)(c.pos.y-c.half.y);
        int top = (int)(c.pos.y+c.half.y);

        left = MathUtils.clamp(left, 0, grid.length-1);
        right = MathUtils.clamp(right, 0, grid.length-1);
        bottom = MathUtils.clamp(bottom, 0, grid[0].length-1);
        top = MathUtils.clamp(top, 0, grid[0].length-1);

        for(int x = left; x <= right; x++) {
            for(int y = bottom; y <= top; y++) {
                Tile tile = grid[x][y];
                if(tile != null) {
                    tile.collisionResponse(pj);
                }
            }
        }

        pj.pos.set(c.pos);
        pj.modelInstance.transform.setTranslation(pj.pos.x, pj.y, pj.pos.y);
    }

    public static AABB modelInstanceCollider(ModelInstance instance) {
        instance.calculateBoundingBox(boundingBox);

        boundingBox.getCenter(tmpCenter);
        boundingBox.getDimensions(tmpDimension);
        instance.transform.getTranslation(tmpPosition);
        instance.transform.getScale(tmpScale);

        tmpDimension.scl(tmpScale);
        tmpCenter.scl(tmpScale);
        tmpPosition.add(tmpCenter);

        return tmpAABB.set(tmpPosition.x, tmpPosition.z, tmpDimension.x * 0.5f, tmpDimension.z * 0.5f);
    }

    static boolean isVisible(PerspectiveCamera cam, ModelInstance modelInstance) {
        modelInstance.transform.getTranslation(tmpPosition);
        tmpDimension.set(1, 1, 1);
        return cam.frustum.boundsInFrustum(tmpPosition, tmpDimension);
    }

    static Tile tileFromCoords(Vector2 coords, Tile[][] grid) {
        int x = (int)coords.x;
        int y = (int)coords.y;

        if(x < 0 || x >= grid.length || y < 0 || y >= grid[x].length) return null; // out of bounds

        return grid[x][y];
    }
}
