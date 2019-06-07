attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;
attribute vec2 a_boneWeight0;
attribute vec2 a_boneWeight1;
attribute vec2 a_boneWeight2;
attribute vec2 a_boneWeight3;

uniform mat4 u_projViewTrans;
uniform mat4 u_viewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;
uniform mat4 u_bones[10];
uniform vec4 u_diffuseUVTransform;
uniform vec4 u_ambientLight;
uniform vec3 u_dirLight;

varying vec4 v_viewSpace;
varying vec3 v_normal;
varying vec2 v_diffuseUV;
varying float v_brightness;

void main() {
	mat4 skinning = mat4(0.0);
	skinning += (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)];
	skinning += (a_boneWeight1.y) * u_bones[int(a_boneWeight1.x)];
	skinning += (a_boneWeight2.y) * u_bones[int(a_boneWeight2.x)];
	skinning += (a_boneWeight3.y) * u_bones[int(a_boneWeight3.x)];
	vec4 pos = u_worldTrans * skinning * vec4(a_position, 1.0);

    v_viewSpace = u_viewTrans * pos;
	gl_Position = u_projViewTrans * pos;

    vec3 normal = normalize((u_worldTrans * skinning * vec4(a_normal, 0.0)).xyz);

    v_normal = a_normal;
	v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;




    vec3 lightDir = -u_dirLight;
    float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
    v_brightness = clamp(NdotL + u_ambientLight.a, 0.0, 1.0);
}
