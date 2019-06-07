package rmkl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.BufferUtils;

/** http://stackoverflow.com/a/35281945 */

import java.nio.FloatBuffer;

class Anisotropy {

    private static boolean anisotropySupported = false;
    private static boolean checkComplete = false;
    private static float maxAnisotropySupported = 1.0f;

    /**Applies the given anisotropic level to the texture. Returns the anisotropy value that was applied,
     * based on device's maximum capability.
     * @param texture The texture to apply anisotropy to.
     * @param anisotropy The anisotropic level to apply. (Will be reduced if device capability is inferior)
     * @return The anisotropic level that has been applied, or -1.0 if anisotropy is not supported by the device.
     */
    static float setTextureAnisotropy(Texture texture, float anisotropy){
        if (isSupported()) {
            texture.bind();
            float valueApplied = Math.min(maxAnisotropySupported, anisotropy);
            Gdx.gl20.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, valueApplied);
            return valueApplied;
        } else {
            return -1f;
        }
    }

    private static boolean isSupported(){
        if(!checkComplete){
            GL20 gl = Gdx.gl;
            if(gl != null) {
                if(Gdx.graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")){
                    anisotropySupported = true;
                    FloatBuffer buffer = BufferUtils.newFloatBuffer(16);
                    Gdx.gl20.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer);
                    maxAnisotropySupported = buffer.get(0);
                }
                checkComplete = true;
            } else
                throw new UnsupportedOperationException("Cannot check GL state before libGDX is initialized");
        }
        return anisotropySupported;
    }
}
