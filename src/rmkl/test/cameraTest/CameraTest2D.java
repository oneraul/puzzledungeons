package rmkl.test.cameraTest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import collision.AABB;
import collision.Hit;

public class CameraTest2D extends ApplicationAdapter {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Camera2D Test");
        cfg.setWindowedMode(800, 600);
        new Lwjgl3Application(new CameraTest2D(), cfg);
    }

    private ShapeRenderer shaper;
    private AABB[][] grid;
    private final int size = 40;
    private CameraController2D controller;
    private Camera2D cam;
    private Pj2D pj;

    @Override
    public void create() {
        shaper = new ShapeRenderer();

        cam = new Camera2D();
        pj = new Pj2D();
        controller = new CameraController2D(cam, pj);


        grid = new AABB[10][10];
        for(int i = 0; i < grid.length; i++) {
            grid[i][2] = new AABB((i + 0.5f) * size, (2 + 0.5f) * size, size * 0.5f, size * 0.5f);
        }
        grid[4][2] = null;
        for(int i = 3; i < grid[0].length; i++) {
            grid[grid[0].length-1][i] = new AABB((grid[0].length-1 + 0.5f) * size, (i + 0.5f) * size, size * 0.5f, size * 0.5f);
        }
    }

    @Override
    public void render() {

        controller.update(grid, size);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shaper.begin(ShapeRenderer.ShapeType.Line);

        /* grid */
        shaper.setColor(Color.DARK_GRAY);
        for(int i = 0; i <= grid.length; i++) {
            shaper.line(0, i*size, grid.length*size, i*size);
            shaper.line(i*size, 0, i*size, grid[0].length*size);
        }

        /* pj & cam */
        pj.draw(shaper, size);
        cam.draw(shaper, size);

        /* objects */
        for(int x = 0; x < grid.length; x++) {
            for(int y = 0; y < grid[x].length; y++) {
                if(grid[x][y] != null) grid[x][y].debug(shaper);
            }
        }

        shaper.end();
    }

    @Override
    public void dispose() {
        shaper.dispose();
    }
}

class Camera2D {
    Vector2 pos = new Vector2();
    Vector2 dir = new Vector2(1, 0);
    float angle;
    float distance = 2;
    boolean independent;

    private static final Vector2 tmp = new Vector2();
    void draw(ShapeRenderer shaper, float scl) {
        final float visionAngle = 60;

        shaper.setColor(Color.ORANGE);
        shaper.circle(pos.x, pos.y, 2);

        tmp.x = MathUtils.cosDeg(angle + visionAngle/2);
        tmp.y = MathUtils.sinDeg(angle + visionAngle/2);
        shaper.line(pos, tmp.scl(scl).add(pos));

        tmp.x = MathUtils.cosDeg(angle - visionAngle/2);
        tmp.y = MathUtils.sinDeg(angle - visionAngle/2);
        shaper.line(pos, tmp.scl(scl).add(pos));
    }
}

class Pj2D {
    Vector2 pos = new Vector2(50, 50);
    Vector2 dir = new Vector2(1, 0);
    float angle;

    private static final Vector2 tmp = new Vector2();
    void draw(ShapeRenderer shaper, float scl) {
        shaper.setColor(Color.WHITE);
        shaper.circle(pos.x, pos.y, 2);

        tmp.x = MathUtils.cosDeg(angle);
        tmp.y = MathUtils.sinDeg(angle);
        shaper.line(pos, tmp.scl(scl/2).add(pos));
    }
}

class CameraController2D {
    private final Camera2D cam;
    private final Pj2D pj;

    CameraController2D(Camera2D cam, Pj2D pj) {
        this.cam = cam;
        this.pj = pj;
    }

    void update(AABB[][] grid, float tileSize) {

        /* input */
        float v = 3;
        // pj movement & rotation
        if(Gdx.input.isKeyPressed(Input.Keys.A)) rotatePj(false);
        if(Gdx.input.isKeyPressed(Input.Keys.D)) rotatePj(true);
        if(Gdx.input.isKeyPressed(Input.Keys.W)) pj.pos.add(new Vector2(pj.dir).scl(v));
        if(Gdx.input.isKeyPressed(Input.Keys.S)) pj.pos.add(new Vector2(pj.dir).scl(-v));
        if(Gdx.input.isKeyPressed(Input.Keys.Q)) pj.pos.add(new Vector2(-pj.dir.y, pj.dir.x).scl(v));
        if(Gdx.input.isKeyPressed(Input.Keys.E)) pj.pos.add(new Vector2(pj.dir.y, -pj.dir.x).scl(v));
        // cam rotation (pj-independent)
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            cam.independent = true;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) rotateCam(false);
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) rotateCam(true);
        } else cam.independent = false;
        // cam distance
        if(Gdx.input.isKeyPressed(Input.Keys.Z)) cam.distance += 0.05f;
        if(Gdx.input.isKeyPressed(Input.Keys.X)) cam.distance -= 0.05f;
        cam.distance = MathUtils.clamp(cam.distance, 0, 4);


        /* set cam */
        Vector2 tmp = new Vector2(cam.dir).scl(30*cam.distance);
        cam.pos.set(pj.pos).sub(tmp);

        /* cam collision with the walls */
        int centerX = (int)((cam.pos.x)/tileSize);
        int centerY = (int)((cam.pos.y)/tileSize);

        int left = MathUtils.clamp(centerX-1, 0, grid.length-1);
        int right = MathUtils.clamp(centerX+1, 0, grid.length-1);
        int bottom = MathUtils.clamp(centerY-1, 0, grid[0].length-1);
        int top = MathUtils.clamp(centerY+1, 0, grid[0].length-1);

        for(int x = left; x <= right; x++) {
            for(int y = bottom; y <= top; y++) {
                if(grid[x][y] != null) {
                    Hit hit = grid[x][y].intersectSegment(pj.pos, cam.pos);
                    if(hit != null) cam.pos.set(hit.pos);
                }
            }
        }
    }

    private void rotatePj(boolean rightDirection) {
        float rotation_v = 3;
        pj.angle += rightDirection ? -rotation_v : rotation_v;
        pj.dir.x = MathUtils.cosDeg(pj.angle);
        pj.dir.y = MathUtils.sinDeg(pj.angle);

        if(!cam.independent) {
            cam.angle = pj.angle;
            cam.dir.set(pj.dir);
        }
    }

    private void rotateCam(boolean rightDirection) {
        float rotation_v = 3;
        cam.angle += rightDirection ? -rotation_v : rotation_v;
        cam.dir.x = MathUtils.cosDeg(cam.angle);
        cam.dir.y = MathUtils.sinDeg(cam.angle);
    }
}
