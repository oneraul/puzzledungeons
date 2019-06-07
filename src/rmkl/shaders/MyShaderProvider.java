package rmkl.shaders;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;

public class MyShaderProvider extends BaseShaderProvider {

    @Override
    protected Shader createShader(Renderable renderable) {

        boolean bones = renderable.bones != null;

        if(renderable.material.has(TextureAttribute.Diffuse)) {

            if(bones) return new SkinningTextureShader();
            else if(renderable.environment == null) return new FlatTextureShader();
            else return new TextureShader();

        } else if(renderable.material.has(ColorAttribute.Diffuse)) {

            if(bones) return new SkinningColorShader();
            else if(renderable.environment == null) return new FlatColorShader();
            else return new ColorShader();
        }

        // fallback to default shader
        System.out.println("DefaultShader loaded");
        return new DefaultShader(renderable);
    }
}
