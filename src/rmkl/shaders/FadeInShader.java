package rmkl.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FadeInShader implements Shader {

    private ShaderProgram program;
    private int u_projTrans;
    private int u_worldTrans;
    private int u_diffuseColor;
    private int u_time;
    private float timer;
    public final float duration = 1.5f;
    private final int mask_texture_unit = 3;
    private Texture maskTexture = new Texture("assets/mask.png");

    @Override
    public void init() {
        String vert = Gdx.files.internal("assets/shaders/lighting.vert").readString();
        String frag = Gdx.files.internal("assets/shaders/fadeIn.frag").readString();
        program = new ShaderProgram(vert, frag);
        if(!program.isCompiled()) throw new GdxRuntimeException(program.getLog());

        u_projTrans = program.getUniformLocation("u_projTrans");
        u_worldTrans = program.getUniformLocation("u_worldTrans");
        u_diffuseColor = program.getUniformLocation("u_diffuseColor");
        u_time = program.getUniformLocation("u_time");

        program.begin();
        program.setUniformf("u_duration", duration);
        program.setUniformi("u_mask", mask_texture_unit);
        program.end();
    }

    public void resetTimer() {
        timer = 0;
    }

    @Override
    public void dispose() {
        program.dispose();
        maskTexture.dispose();
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        program.begin();
        program.setUniformMatrix(u_projTrans, camera.combined);
        context.setDepthTest(GL20.GL_LEQUAL);
        context.setCullFace(GL20.GL_BACK);

        maskTexture.bind(mask_texture_unit);

        timer += Gdx.graphics.getDeltaTime();
        //if(timer >= duration) timer = 0;  //automatic reset, for debug purposes
        program.setUniformf(u_time, timer);
    }

    @Override
    public void render(Renderable renderable) {
        program.setUniformMatrix(u_worldTrans, renderable.worldTransform);

        // TODO use DefaultTextureBinder
        if(renderable.material.has(TextureAttribute.Diffuse)) {
            TextureDescriptor<Texture> textureDescription = ((TextureAttribute) renderable.material.get(TextureAttribute.Diffuse)).textureDescription;
            textureDescription.texture.bind(0);
        }
        if(renderable.material.has(ColorAttribute.Diffuse)) {
            Color color = ((ColorAttribute)renderable.material.get(ColorAttribute.Diffuse)).color;
            program.setUniformf(u_diffuseColor, color);
        }

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
        return renderable.material.has(TextureAttribute.Diffuse) || renderable.material.has(ColorAttribute.Diffuse);
    }
}
