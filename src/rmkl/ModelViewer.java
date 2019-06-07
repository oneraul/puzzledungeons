package rmkl;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.Debug3dRenderer;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.SerializationException;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import rmkl.shaders.MyShaderProvider;

public class ModelViewer extends ApplicationAdapter {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("ModelViewer");
        cfg.setWindowedMode(800, 600);
        new Lwjgl3Application(new ModelViewer(), cfg);
    }

    private PerspectiveCamera cam;
    private ShapeRenderer shaper;
    private ModelBatch modelBatch;
    private Debug3dRenderer debugger;
    private Model model;
    private ModelInstance modelInstance;
    private ModelLoader modelLoader;
    private AnimationController animationController;
    private Environment environment;
    private Stage stage;
    private boolean debug;

    @Override
    public void create() {

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0, 2f, 2f);
        cam.lookAt(0, 0, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        modelBatch = new ModelBatch(new MyShaderProvider());
        debugger = new Debug3dRenderer();
        shaper = new ShapeRenderer();
        modelLoader = new G3dModelLoader(new JsonReader());

        stage = new Stage();
        VisUI.load();

        VisTextField textField = new VisTextField();
        VisTextButton button = new VisTextButton("loadMap");
        VisSelectBox animation_selectBox = new VisSelectBox();
        animation_selectBox.setPosition(0, textField.getHeight());
        button.setPosition(textField.getWidth(), 0);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if(model != null) {
                    model.dispose();
                    modelInstance = null;
                }

                try {
                    model = modelLoader.loadModel(Gdx.files.internal("assets/" + textField.getText()));
                    modelInstance = new ModelInstance(model);
                    animationController = new AnimationController(modelInstance);
                    Array<String> animations = new Array<>();
                    for(Animation animation : model.animations) animations.add(animation.id);
                    animation_selectBox.setItems(animations);
                } catch(SerializationException e) {
                    e.printStackTrace();
                }
            }
        });
        animation_selectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if(animationController != null) {
                    animationController.setAnimation((String) animation_selectBox.getSelected(), -1);
                }
            }
        });
        VisCheckBox debug_checkbox = new VisCheckBox("debug");
        debug_checkbox.setPosition(0, animation_selectBox.getY()+animation_selectBox.getHeight());
        debug_checkbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                debug = debug_checkbox.isChecked();
            }
        });
        stage.addActor(textField);
        stage.addActor(button);
        stage.addActor(animation_selectBox);
        stage.addActor(debug_checkbox);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new CameraInputController(cam));
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render() {
        if(animationController != null) animationController.update(Gdx.graphics.getDeltaTime());

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        shaper.setProjectionMatrix(cam.combined);
        shaper.begin(ShapeRenderer.ShapeType.Line);

        /* grid */
        shaper.setColor(Color.DARK_GRAY);
        for(int i = -10; i < 11; i++) {
            shaper.line(i, 0, -10,     i, 0, 10);
            shaper.line(-10, 0, i,     10, 0, i);
        }

        /* axis */
        shaper.setColor(Color.RED);     shaper.line(Vector3.Zero, Vector3.X);
        shaper.setColor(Color.GREEN);   shaper.line(Vector3.Zero, Vector3.Y);
        shaper.setColor(Color.BLUE);    shaper.line(Vector3.Zero, Vector3.Z);
        shaper.end();

        if(modelInstance != null) {

            if(debug) {
                MeshPartBuilder builder = debugger.begin();
                BoundingBox box = new BoundingBox();
                modelInstance.calculateBoundingBox(box);
                BoxShapeBuilder.build(builder, box);
                debugger.end();
            }

            modelBatch.begin(cam);
            modelBatch.render(modelInstance, environment);
            if(debug) modelBatch.render(debugger);
            modelBatch.end();
        }

        stage.act();
        stage.draw();
    }

    @Override
    public void dispose() {
        shaper.dispose();
        modelBatch.dispose();
        debugger.dispose();
        if(model != null) model.dispose();
        stage.dispose();
        VisUI.dispose();
    }
}
