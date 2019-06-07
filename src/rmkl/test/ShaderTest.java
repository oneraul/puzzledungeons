package rmkl.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import rmkl.shaders.FadeInShader;

public class ShaderTest extends ApplicationAdapter {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("3D Test");
        cfg.setWindowedMode(800, 600);
        new Lwjgl3Application(new ShaderTest(), cfg);
    }

    private Shader shader;
    private ModelBatch modelBatch;
    private Model model, model2;
    private Texture diffuse, normal, specular;
    private ModelInstance instance, instance1, instance2, instance3;
    private PerspectiveCamera cam;
    private Environment environment;

    @Override
    public void create() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        diffuse = new Texture("assets/nobiax/pattern_100_diffus.png");
        normal = new Texture("assets/nobiax/pattern_100_normal.png");
        specular = new Texture("assets/nobiax/pattern_100_specular.png");

        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(1, 1, 1,
                new Material(new TextureAttribute(TextureAttribute.Diffuse, diffuse),
                             new TextureAttribute(TextureAttribute.Normal, normal),
                             new TextureAttribute(TextureAttribute.Specular, specular)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        instance = new ModelInstance(model);
        instance1 = new ModelInstance(model, 3, 0, 0);

        model2 = modelBuilder.createSphere(1, 1, 1, 16, 16,
                new Material(new TextureAttribute(TextureAttribute.Diffuse, diffuse)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        instance2 = new ModelInstance(model2, 3, 0, 3);
        instance3 = new ModelInstance(model2, 0, 0, 3);

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(5f, 5f, 5f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        modelBatch = new ModelBatch();

        Gdx.input.setInputProcessor(new CameraInputController(cam));

        shader = new FadeInShader();
        shader.init();
    }

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(cam);
        modelBatch.render(instance, environment, shader);
        modelBatch.render(instance1, environment);
        modelBatch.render(instance2, environment, shader);
        modelBatch.render(instance3, environment);
        modelBatch.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
        model2.dispose();
        shader.dispose();
        diffuse.dispose();
        normal.dispose();
        specular.dispose();
    }
}
