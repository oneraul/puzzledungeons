attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;
uniform mat4 u_viewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;
uniform vec4 u_diffuseUVTransform;
uniform vec4 u_ambientLight;
uniform vec3 u_dirLight;

varying vec4 v_viewSpace;
varying vec3 v_normal;
varying vec2 v_diffuseUV;
varying float v_brightness;

void main() {
    v_viewSpace = u_viewTrans * u_worldTrans * vec4(a_position, 1.0);
    gl_Position = u_projTrans * u_worldTrans * vec4(a_position, 1.0);

    vec3 normal = normalize(u_normalMatrix * a_normal);

    v_normal = normal;
    v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;



    vec3 lightDir = -u_dirLight;
    float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
    v_brightness = clamp(NdotL + u_ambientLight.a, 0.0, 1.0);
}
