package rmkl.test;

import collision.AABB;
import collision.Circle;
import collision.Hit;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;

public class FenceCollisionTest extends ApplicationAdapter {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("3D Test");
        cfg.setWindowedMode(800, 600);
        new Lwjgl3Application(new FenceCollisionTest(), cfg);
    }


    private ShapeRenderer shaper;
    private Stage stage;
    private VisLabel fps;
    private PerspectiveCamera cam;
    private Circle pj;
    private TestFence[] fences;

    @Override
    public void create() {
        VisUI.load();
        stage = new Stage();
        stage.addActor(fps = new VisLabel());
        fps.setPosition(10, Gdx.graphics.getHeight()-20);

        shaper = new ShapeRenderer();

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0, 0, 2f);
        cam.lookAt(0, 0, 0);
        cam.near = 0.1f;
        cam.far = 150f;
        cam.update();

        Gdx.input.setInputProcessor(stage);

        pj = new Circle(0, 0, 0.1f);

        fences = new TestFence[] {
                new TestFence(1.5f, 0.5f),
                new TestFence(2.5f, 0.5f),
                new TestFence(1.5f, 1.5f),
        };
    }

    @Override
    public void render() {
        cam.direction.set(Vector3.Z).scl(-1);
        cam.up.set(Vector3.Y);

        float v = 0.02f;
        if(Gdx.input.isKeyPressed(Input.Keys.W)) pj.center.y += v;
        if(Gdx.input.isKeyPressed(Input.Keys.A)) pj.center.x -= v;
        if(Gdx.input.isKeyPressed(Input.Keys.S)) pj.center.y -= v;
        if(Gdx.input.isKeyPressed(Input.Keys.D)) pj.center.x += v;

        for(TestFence fence : fences) {
            for(AABB collider : fence.colliders) {
                Hit hit = pj.intersectAABB(collider);
                if(hit != null) {
                    pj.center.set(collider.pos).add(hit.delta);
                }
            }
        }


        cam.position.x = pj.center.x; cam.position.y = pj.center.y; cam.update();

        fps.setText(Gdx.graphics.getFramesPerSecond() + "fps");
        stage.act();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        shaper.setProjectionMatrix(cam.combined);
        shaper.begin(ShapeRenderer.ShapeType.Line);

        /* grid */
        shaper.setColor(Color.DARK_GRAY);
        for(int i = -10; i < 11; i++) {
            shaper.line(i, -10,     i, 10);
            shaper.line(-10, i,     10, i);
        }
        /* axis */
        shaper.setColor(Color.RED);     shaper.line(Vector3.Zero, Vector3.X);
        shaper.setColor(Color.GREEN);   shaper.line(Vector3.Zero, Vector3.Y);
        shaper.end();

        shaper.begin(ShapeRenderer.ShapeType.Filled);
        shaper.setColor(Color.FIREBRICK);
        shaper.circle(pj.center.x, pj.center.y, pj.radius, 32);
        for(TestFence fence : fences) for(AABB collider : fence.colliders) collider.debug(shaper);
        shaper.end();

        stage.draw();
    }

    @Override
    public void dispose() {
        shaper.dispose();
        stage.dispose();
        VisUI.dispose();
    }
}

class TestFence {

    AABB[] colliders;

    TestFence(float x, float y) {
        Vector2 pos = new Vector2(x, y);

        colliders = new AABB[] {
            new AABB(pos.x, pos.y, 0.1f, 0.1f),
            new AABB(pos.x - 0.25f, pos.y, 0.25f, 0.06f),
            new AABB(pos.x + 0.25f, pos.y, 0.25f, 0.06f),
            new AABB(pos.x, pos.y + 0.25f, 0.06f, 0.25f),
            new AABB(pos.x, pos.y - 0.25f, 0.06f, 0.25f),
        };
    }
}