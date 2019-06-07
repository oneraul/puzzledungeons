package rmkl.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.utils.GdxRuntimeException;

class ColorShader implements Shader {

    private ShaderProgram program;
    private int u_projTrans;
    private int u_viewTrans;
    private int u_worldTrans;
    private int u_normalMatrix;
    private int u_diffuseColor;

    private static Matrix3 tmpMatrix = new Matrix3();

    private LightingSetter lightingSetter;

    @Override
    public void init() {
        String vert = Gdx.files.internal("assets/shaders/lighting.vert").readString();
        String frag = Gdx.files.internal("assets/shaders/color.frag").readString();
        program = new ShaderProgram(vert, frag);
        if(!program.isCompiled()) throw new GdxRuntimeException(program.getLog());

        lightingSetter = new LightingSetter(program);

        u_projTrans = program.getUniformLocation("u_projTrans");
        u_viewTrans = program.getUniformLocation("u_viewTrans");
        u_worldTrans = program.getUniformLocation("u_worldTrans");
        u_normalMatrix = program.getUniformLocation("u_normalMatrix");
        u_diffuseColor = program.getUniformLocation("u_diffuseColor");
    }

    @Override
    public void dispose() {
        program.dispose();
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        program.begin();
        program.setUniformMatrix(u_projTrans, camera.combined);
        program.setUniformMatrix(u_viewTrans, camera.view);
        context.setDepthTest(GL20.GL_LEQUAL);
        context.setCullFace(GL20.GL_BACK);

        lightingSetter.setDirtyFlag();
    }

    @Override
    public void render(Renderable renderable) {
        program.setUniformMatrix(u_worldTrans, renderable.worldTransform);

        // set diffuse
        Color color = ((ColorAttribute)renderable.material.get(ColorAttribute.Diffuse)).color;
        program.setUniformf(u_diffuseColor, color);

        // set normal matrix
        program.setUniformMatrix(u_normalMatrix, tmpMatrix.set(renderable.worldTransform).inv().transpose());

        // set environment
        lightingSetter.set(program, renderable.environment);

        renderable.meshPart.render(program);
    }

    @Override
    public void end() {
        program.end();
    }

    @Override
    public int compareTo(Shader other) {
        return 0;
    }

    @Override
    public boolean canRender(Renderable renderable) {
        return renderable.material.has(ColorAttribute.Diffuse)
            && !renderable.material.has(TextureAttribute.Diffuse)
            && renderable.bones == null
            && renderable.environment != null;
    }
}
