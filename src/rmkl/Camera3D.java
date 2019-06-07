package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import rmkl.tiles.Tile;

// TODO change most Vector2 to Vector3

public class Camera3D {
    public Vector2 pos;
    Vector2 dir;
    float angle;
    float distance;
    boolean independent;
    PerspectiveCamera perspectiveCamera;

    Camera3D() {
        pos = new Vector2();
        dir = new Vector2();
        distance = 2;
        perspectiveCamera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        perspectiveCamera.far = 24;
        perspectiveCamera.near = 0.1f;
    }
}

class CameraController3D extends InputAdapter {

    private final Camera3D cam;
    private final Pj3D pj;

    private boolean movingForward, movingBackwards, movingLeft, movingRight, turningLeft, turningRight;
    private boolean camLerping;
    private final float max_cam_distance = 4;
    private final Vector2 tmp = new Vector2();

    CameraController3D(Camera3D cam, Pj3D pj) {
        this.cam = cam;
        this.pj = pj;

        //init
        pj.dir.x = MathUtils.cosDeg(pj.angle);
        pj.dir.y = MathUtils.sinDeg(pj.angle);
        cam.angle = pj.angle;
        cam.dir.set(pj.dir);
    }

    @Override
    public boolean keyDown(int keycode) {
        switch(keycode) {
            case Keys.W: movingForward   = true; break;
            case Keys.A: turningLeft     = true; break;
            case Keys.S: movingBackwards = true; break;
            case Keys.D: turningRight    = true; break;
            case Keys.Q: movingLeft      = true; break;
            case Keys.E: movingRight     = true; break;
            case Keys.SPACE:
                Clientesito.CLIENT.sendUDP(new Network.Jump());
                pj.jump();
                break;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch(keycode) {
            case Keys.W: movingForward   = false; break;
            case Keys.A: turningLeft     = false; break;
            case Keys.S: movingBackwards = false; break;
            case Keys.D: turningRight    = false; break;
            case Keys.Q: movingLeft      = false; break;
            case Keys.E: movingRight     = false; break;
        }

        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        cam.distance = MathUtils.clamp(cam.distance + amount*0.1f, 0, max_cam_distance);
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        cam.independent = true;
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        cam.independent = false;
        if(pj.angle > cam.angle+0.03f || pj.angle < cam.angle-0.03f)
            camLerping = true;
        return false;
    }

    void update(Tile[][] grid, float dt) {

        /* set/correct and update cam.dir */
        if(cam.independent) {
            cam.angle += Gdx.input.getDeltaX();
        } else if(camLerping) {
            cam.angle = cam.angle*0.8f + pj.angle*0.2f;
            if(pj.angle < cam.angle+0.03f && pj.angle > cam.angle-0.03f)
                camLerping = false;
        }
        cam.dir.x = MathUtils.cosDeg(cam.angle);
        cam.dir.y = MathUtils.sinDeg(cam.angle);

        /* make local copies of the input booleans so they can be safely modified */
        boolean localMovingForward = movingForward, localMovingBackwards = movingBackwards, localMovingLeft = movingLeft,
                localMovingRight = movingRight, localTurningLeft = turningLeft, localTurningRight = turningRight;

        /* cancel if both directions at the same time */
        if(localMovingForward && localMovingBackwards) localMovingForward = localMovingBackwards = false;
        if(localMovingLeft && localMovingRight) localMovingLeft = localMovingRight = false;
        if(localTurningLeft && localTurningRight) localTurningLeft = localTurningRight = false;

        /* pj movement */
        final float v = 2f * dt;
        final float strafe_v = 0.8f;
        final float backwards_v = 0.5f;
        if(localMovingForward)   pj.pos.add(new Vector2(pj.dir).scl(v));
        if(localMovingBackwards) pj.pos.add(new Vector2(pj.dir).scl(-v*backwards_v));
        if(localMovingLeft)      pj.pos.add(new Vector2(pj.dir.y, -pj.dir.x).scl(v*strafe_v));
        if(localMovingRight)     pj.pos.add(new Vector2(-pj.dir.y, pj.dir.x).scl(v*strafe_v));
        if(localTurningLeft)     rotatePj(false, dt);
        if(localTurningRight)    rotatePj(true, dt);

        /* pj animation */ // TODO only queue on change
        if(localMovingForward)   pj.animationController.animate("run", -1, 1, null, 0.1f);
        if(localMovingBackwards) pj.animationController.animate("walk", -1, -1, null, 0.1f);
        if(localMovingLeft)      pj.animationController.animate("left_strafe", -1, strafe_v, null, 0.1f);
        if(localMovingRight)     pj.animationController.animate("right_strafe", -1, strafe_v, null, 0.1f);
        if(!localMovingForward && !localMovingBackwards && !localMovingLeft && !localMovingRight)
            pj.animationController.animate("idle", -1, 1, null, 0.1f);

        /* cam2D position*/
        tmp.set(cam.dir).scl(cam.distance); // FIXME la cam traspasa los muros cuando la distancia es bastante grande
        cam.pos.set(pj.pos).sub(tmp);

        /* cam2D collision with the walls */
        int centerX = (int)cam.pos.x;
        int centerY = (int)cam.pos.y;

        int left = MathUtils.clamp(centerX-1, 0, grid.length-1);
        int right = MathUtils.clamp(centerX+1, 0, grid.length-1);
        int bottom = MathUtils.clamp(centerY-1, 0, grid[0].length-1);
        int top = MathUtils.clamp(centerY+1, 0, grid[0].length-1);

        for(int x = left; x <= right; x++) {
            for(int y = bottom; y <= top; y++) {
                if(grid[x][y] != null) {
                    grid[x][y].cameraCollision(pj, cam);
                }
            }
        }

        /* update perspectiveCamera */
        float camY = cam.distance/max_cam_distance+0.5f; //y relative to cam.distance
        cam.perspectiveCamera.position.set(cam.pos.x, camY, cam.pos.y);
        cam.perspectiveCamera.direction.set(cam.dir.x, 0, cam.dir.y);
        cam.perspectiveCamera.update();
    }

    private void rotatePj(boolean rightDirection, float dt) {
        final float rotation_v = 100f * dt;
        pj.angle += rightDirection ? rotation_v : -rotation_v;
        pj.dir.x = MathUtils.cosDeg(pj.angle);
        pj.dir.y = MathUtils.sinDeg(pj.angle);

        if(!cam.independent) {
            cam.angle = pj.angle;
            cam.dir.set(pj.dir);
        }
    }

    /* puts in sync the camera angle and pj angles and dir to the current pj.angle */
    void syncAngle() {
        rotatePj(true, 0);
    }
}
