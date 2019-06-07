package rmkl.tiles;

import collision.AABB;
import collision.Hit;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import rmkl.Bitmask;
import rmkl.Camera3D;
import rmkl.Pj3D;

public class Door extends Tile {

    public static final char code = 'D';
    static Model model;
    private AnimationController animationController;
    private AABB collider;

    private boolean locked = true;
    private boolean updateFlag;

    public int x, y;

    /* Checks whether the tile's bitmask is valid and returns an Error tile otherwise */
    public static Tile build(int x, int y, char[][] map) {

        int bitmask = Bitmask.check4bit(x, y, map, Fence.code, Wall.code, TWall.code, Door.code);
        if(bitmask != 6 && bitmask != 9) {
            System.out.println("ERROR: Door(" + x + ", " + y + ") -> bitmask=" + bitmask);
            return new Error().init(x, y);

        } else {
            if(model == null) return ServerDoor(x, y, bitmask);
            return new Door(x, y, bitmask);
        }
    }

    /* for the server */
    private Door() {}
    private static Door ServerDoor(int x, int y, int bitmask) {

        Door door = new Door();
        door.x = x;
        door.y = y;

        float colliderX = x+0.5f, colliderY = y+0.5f;
        if(bitmask == 6) door.collider = new AABB(colliderX, colliderY, 0.5f, 0.09f);
        else if(bitmask == 9) door.collider = new AABB(colliderX, colliderY, 0.09f, 0.5f);

        return door;
    }

    /* for the client */
    private Door(int x, int y, int bitmask) {

        modelInstance = new ModelInstance(model);
        animationController = new AnimationController(modelInstance);
        float colliderX = x+0.5f, colliderY = y+0.5f;

        if(bitmask == 6) {
            collider = new AABB(colliderX, colliderY, 0.5f, 0.09f);

        } else if(bitmask == 9) {
            modelInstance.transform.setToRotation(Vector3.Y, 90);
            collider = new AABB(colliderX, colliderY, 0.09f, 0.5f);
        }

        modelInstance.transform.setTranslation(x+0.5f, 0, y+0.5f);
    }

    @Override
    public void collisionResponse(Pj3D pj) {

        if(locked) {

            Hit hit = pj.collider.intersectAABB(collider);
            if(hit != null) {

                //open(); // TODO check if pj has keys

                pj.collider.pos.add(hit.delta);
            }
        }
    }

    public void setLocked(boolean locked) {
        this.locked = locked;

        if(model != null) {
            if(!locked) open();
            else close();
        }
    }

    private void open() {
        startAnimation("open", false);
    }

    private void close() {
        startAnimation("close", true);
    }

    private void startAnimation(String animationID, final boolean lock) {

        float offset = animationController.current == null ? 0 :
                animationController.current.duration-animationController.current.time;
        int duration = -1; // to play complete
        int loopCount = 1; // to play only once
        float speed = 1f;

        locked = lock;
        updateFlag = true;
        animationController.setAnimation(animationID, offset, duration, loopCount, speed, new AnimationController.AnimationListener() {
            @Override
            public void onEnd(AnimationController.AnimationDesc animationDesc) {
                updateFlag = false;
            }

            @Override
            public void onLoop(AnimationController.AnimationDesc animationDesc) {}
        });
    }

    @Override
    public void update(float dt) {
        if(updateFlag) {
            animationController.update(dt);
        }
    }

    @Override
    public void cameraCollision(Pj3D pj, Camera3D cam) {
        if(locked) {
            Hit hit = collider.intersectSegment(pj.pos, cam.pos);
            if(hit != null) cam.pos.set(hit.pos);
        }
    }

    @Override
    public char type() {
        return code;
    }
}
