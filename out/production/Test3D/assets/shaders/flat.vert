attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_viewTrans;
uniform mat4 u_projTrans;
uniform vec4 u_diffuseUVTransform;

varying vec4 v_viewSpace;
varying vec2 v_diffuseUV;
varying float v_brightness;

void main() {
    v_viewSpace = u_viewTrans * u_worldTrans * vec4(a_position, 1.0);
    gl_Position = u_projTrans * u_worldTrans * vec4(a_position, 1.0);

    v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;
    v_brightness = 1.0;
}
