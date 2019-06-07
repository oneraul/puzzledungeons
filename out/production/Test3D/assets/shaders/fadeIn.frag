#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_diffuseUV;

uniform sampler2D u_sampler2D;
uniform sampler2D u_mask;
uniform vec4 u_diffuseColor;
uniform float u_time;
uniform float u_duration;

void main() {
    float mask = texture2D(u_mask, v_diffuseUV).r;
    if(mask.r > u_time/u_duration) discard;

    gl_FragColor = texture2D(u_sampler2D, v_diffuseUV) + u_diffuseColor;
}