package rmkl.test;

import collision.AABB;
import collision.Hit;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class NamesTest extends ApplicationAdapter {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Camera3D Test");
        cfg.setWindowedMode(800, 600);
        new Lwjgl3Application(new NamesTest(), cfg);
    }

    private ShapeRenderer shaper;
    private final int size = 1;
    private CameraController3D controller;
    private Camera3D cam3D;
    private Pj3D pj;

    private ModelBatch modelBatch;
    private Model pjModel, wallModel;
    private AABB[][] grid;
    private ModelInstance[][] gridModels;

    /////////
    SpriteBatch batch;
    BitmapFont font;

    @Override
    public void create() {

        grid = new AABB[10][10];
        for(int i = 0; i < grid.length; i++) {
            grid[i][2] = new AABB((i + 0.5f) * size, (2 + 0.5f) * size, size * 0.5f, size * 0.5f);
        }
        grid[4][2] = null;
        for(int i = 3; i < grid[0].length; i++) {
            grid[grid[0].length-1][i] = new AABB((grid[0].length-1 + 0.5f) * size, (i + 0.5f) * size, size * 0.5f, size * 0.5f);
        }

        modelBatch = new ModelBatch();
        shaper = new ShapeRenderer();

        ModelBuilder modelBuilder = new ModelBuilder();
        pjModel = modelBuilder.createCapsule(0.2f, 1, 8,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked);
        wallModel = modelBuilder.createBox(size, size, size,
                new Material(ColorAttribute.createDiffuse(Color.NAVY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked);

        // -------------

        cam3D = new Camera3D();
        pj = new Pj3D();
        controller = new CameraController3D(cam3D, pj);

        cam3D.perspectiveCamera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        pj.modelInstance = new ModelInstance(pjModel);

        gridModels = new ModelInstance[10][10];
        for(int x = 0; x < grid.length; x++) {
            for(int y = 0; y < grid[x].length; y++) {
                if(grid[x][y] != null) {
                    gridModels[x][y] = new ModelInstance(wallModel, (x+0.5f)*size, 0.5f*size, (y+0.5f)*size);
                }
            }
        }

        ////////////////

        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render() {

        controller.update(grid, size);


        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        shaper.setProjectionMatrix(cam3D.perspectiveCamera.combined);
        shaper.begin(ShapeRenderer.ShapeType.Line);
        /* grid */
        shaper.setColor(Color.DARK_GRAY);
        for(int i = 0; i < grid.length; i++) {
            shaper.line(i*size, 0, 0,     i*size, 0, grid.length*size);
            shaper.line(0, 0, i*size,     grid.length*size, 0, i*size);
        }
        /* axis */
        shaper.setColor(Color.RED);     shaper.line(Vector3.Zero, Vector3.X);
        shaper.setColor(Color.GREEN);   shaper.line(Vector3.Zero, Vector3.Y);
        shaper.setColor(Color.BLUE);    shaper.line(Vector3.Zero, Vector3.Z);
        /* pj */
        pj.draw(shaper);
        shaper.end();

        modelBatch.begin(cam3D.perspectiveCamera);
        modelBatch.render(pj.modelInstance);
        for(int x = 0; x < gridModels.length; x++) {
            for(int y = 0; y < gridModels[x].length; y++) {
                if(gridModels[x][y] != null) modelBatch.render(gridModels[x][y]);
            }
        }
        modelBatch.end();

        //////////////////

        Vector3 text = cam3D.perspectiveCamera.project(new Vector3(0, 1, 0));

        batch.begin();
        font.draw(batch, "pene", text.x, text.y);
        batch.end();
    }

    @Override
    public void dispose() {
        shaper.dispose();
        pjModel.dispose();
        wallModel.dispose();
        modelBatch.dispose();
        batch.dispose();
        font.dispose();
    }
}

class Camera3D {
    Vector2 pos = new Vector2();
    Vector2 dir = new Vector2(1, 0);
    float angle;
    float distance = 2;
    boolean independent;
    PerspectiveCamera perspectiveCamera;
}

class Pj3D {
    Vector2 pos = new Vector2(0, 0);
    Vector2 dir = new Vector2(1, 0);
    float angle;
    ModelInstance modelInstance;

    private static final Vector3 tmp = new Vector3();
    void draw(ShapeRenderer shaper) {
        shaper.setColor(Color.WHITE);
        Vector3 pos3 = new Vector3(pos.x, 0.5f, pos.y);

        tmp.x = MathUtils.cosDeg(angle);
        tmp.y = 0;
        tmp.z = MathUtils.sinDeg(angle);
        tmp.scl(0.5f).add(pos3);

        shaper.line(pos3, tmp);
    }
}

class CameraController3D {
    private final Camera3D cam;
    private final Pj3D pj;

    CameraController3D(Camera3D cam, Pj3D pj) {
        this.cam = cam;
        this.pj = pj;
    }

    void update(AABB[][] grid, float tileSize) {

        /* input */
        float v = 0.1f;
        // pj movement & rotation
        if(Gdx.input.isKeyPressed(Input.Keys.A)) rotatePj(false);
        if(Gdx.input.isKeyPressed(Input.Keys.D)) rotatePj(true);
        if(Gdx.input.isKeyPressed(Input.Keys.W)) pj.pos.add(new Vector2(pj.dir).scl(v));
        if(Gdx.input.isKeyPressed(Input.Keys.S)) pj.pos.add(new Vector2(pj.dir).scl(-v));
        if(Gdx.input.isKeyPressed(Input.Keys.Q)) pj.pos.add(new Vector2(pj.dir.y, -pj.dir.x).scl(v));
        if(Gdx.input.isKeyPressed(Input.Keys.E)) pj.pos.add(new Vector2(-pj.dir.y, pj.dir.x).scl(v));
        // cam2D rotation (pj-independent)
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            cam.independent = true;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) rotateCam(false);
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) rotateCam(true);
        } else cam.independent = false;
        // cam2D distance
        if(Gdx.input.isKeyPressed(Input.Keys.Z)) cam.distance += 0.05f;
        if(Gdx.input.isKeyPressed(Input.Keys.X)) cam.distance -= 0.05f;
        cam.distance = MathUtils.clamp(cam.distance, 0, 4);


        /* set pj translation */
        pj.modelInstance.transform.setTranslation(pj.pos.x, 0.5f, pj.pos.y);

        /* set cam2D */
        Vector2 tmp = new Vector2(cam.dir).scl(cam.distance);
        cam.pos.set(pj.pos).sub(tmp);

        /* cam2D collision with the walls */
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

        cam.perspectiveCamera.position.set(cam.pos.x, 1, cam.pos.y);
        cam.perspectiveCamera.direction.set(cam.dir.x, 0, cam.dir.y);
        cam.perspectiveCamera.up.set(Vector3.Y);
        cam.perspectiveCamera.update();
    }

    private void rotatePj(boolean rightDirection) {
        float rotation_v = 1.5f;
        pj.angle += rightDirection ? rotation_v : -rotation_v;
        pj.dir.x = MathUtils.cosDeg(pj.angle);
        pj.dir.y = MathUtils.sinDeg(pj.angle);

        if(!cam.independent) {
            cam.angle = pj.angle;
            cam.dir.set(pj.dir);
        }
    }

    private void rotateCam(boolean rightDirection) {
        float rotation_v = 3;
        cam.angle += rightDirection ? rotation_v : -rotation_v;
        cam.dir.x = MathUtils.cosDeg(cam.angle);
        cam.dir.y = MathUtils.sinDeg(cam.angle);
    }
}
