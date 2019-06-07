package rmkl.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;

class SkinningTextureShader implements Shader {

    private ShaderProgram program;
    private int u_projViewTrans;
    private int u_viewTrans;
    private int u_worldTrans;
    private int u_normalMatrix;
    private int u_bones;
    private int u_diffuseUVTransform;

    private final static Matrix4 idtMatrix = new Matrix4();
    private static final int numBones = 10;
    private final float bones[] = new float[numBones * 16];

    private static Matrix3 tmpMatrix = new Matrix3();

    private LightingSetter lightingSetter;

    @Override
    public void init() {
        String vert = Gdx.files.internal("assets/shaders/skinningLighting.vert").readString();
        String frag = Gdx.files.internal("assets/shaders/texture.frag").readString();
        program = new ShaderProgram(vert, frag);
        if(!program.isCompiled()) throw new GdxRuntimeException("SkinningFlatColorShader: " + program.getLog());

        lightingSetter = new LightingSetter(program);

        u_projViewTrans = program.getUniformLocation("u_projViewTrans");
        u_viewTrans = program.getUniformLocation("u_viewTrans");
        u_worldTrans = program.getUniformLocation("u_worldTrans");
        u_normalMatrix = program.getUniformLocation("u_normalMatrix");
        u_bones = program.fetchUniformLocation("u_bones", true);
        u_diffuseUVTransform = program.getUniformLocation("u_diffuseUVTransform");
    }

    @Override
    public void begin(Camera camera, RenderContext renderContext) {
        program.begin();
        program.setUniformMatrix(u_projViewTrans, camera.combined);
        program.setUniformMatrix(u_viewTrans, camera.view);
        renderContext.setDepthTest(GL20.GL_LEQUAL);
        renderContext.setCullFace(GL20.GL_BACK);

        lightingSetter.setDirtyFlag();
    }

    @Override
    public void render(Renderable renderable) {
        program.setUniformMatrix(u_worldTrans, renderable.worldTransform);

        // set diffuse
        TextureAttribute diffuseAttribute = (TextureAttribute)renderable.material.get(TextureAttribute.Diffuse);
        diffuseAttribute.textureDescription.texture.bind(0);
        program.setUniformf(u_diffuseUVTransform, diffuseAttribute.offsetU, diffuseAttribute.offsetV, diffuseAttribute.scaleU, diffuseAttribute.scaleV);

        // set normal matrix
        program.setUniformMatrix(u_normalMatrix, tmpMatrix.set(renderable.worldTransform).inv().transpose());

        // set environment
        lightingSetter.set(program, renderable.environment);

        // set bones' matrices
        for (int i = 0; i < bones.length; i++) {
            final int idx = i/16;
            bones[i] = (renderable.bones == null || idx >= renderable.bones.length || renderable.bones[idx] == null) ?
                    idtMatrix.val[i%16] : renderable.bones[idx].val[i%16];
        }
        program.setUniformMatrix4fv(u_bones, bones, 0, bones.length);

        renderable.meshPart.render(program);
    }

    @Override
    public void end() {
        program.end();
    }

    @Override
    public boolean canRender(Renderable renderable) {
        return renderable.bones != null
            && renderable.material.has(TextureAttribute.Diffuse)
            && renderable.environment != null;
    }

    @Override
    public int compareTo(Shader other) {
        if(other == null) return -1;
        if(other == this) return 0;
        return 0;
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
