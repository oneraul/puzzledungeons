package rmkl.shaders;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

class LightingSetter {

    private Environment environment;
    private boolean dirtyFlag;

    private final int u_ambientLight;
    private final int u_dirLight;

    LightingSetter(ShaderProgram program) {
        u_ambientLight = program.getUniformLocation("u_ambientLight");
        u_dirLight = program.getUniformLocation("u_dirLight");
    }

    void set(ShaderProgram program, Environment environment) {

        // set environment
        if(environment != null && (environment != this.environment || dirtyFlag)) {
            this.environment = environment;
            dirtyFlag = false;

            // set ambient light
            if(environment.has(ColorAttribute.AmbientLight)) {
                ColorAttribute ca = (ColorAttribute)environment.get(ColorAttribute.AmbientLight);
                program.setUniformf(u_ambientLight, ca.color);
            }

            // set directional light
            if(environment.has(DirectionalLightsAttribute.Type)) {
                DirectionalLight dl = ((DirectionalLightsAttribute)environment.get(DirectionalLightsAttribute.Type)).lights.peek();
                program.setUniformf(u_dirLight, dl.direction);
            }
        }
    }

    void setDirtyFlag() {
        dirtyFlag = true;
    }
}
