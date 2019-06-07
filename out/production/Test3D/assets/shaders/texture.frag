#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_viewSpace;
varying float v_brightness;
varying vec2 v_diffuseUV;

uniform sampler2D u_sampler2D;

const vec3 fogColor = vec3(0.05, 0.1, 0.15);
const float fogStart = 18.0;
const float fogEnd = 24.0;

void main() {

    float distanceToCamera = length(v_viewSpace);
    if(distanceToCamera > fogEnd) discard;

    float fogFactor = (fogEnd-distanceToCamera)/(fogEnd-fogStart);
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec4 pixel = texture2D(u_sampler2D, v_diffuseUV);
    pixel.rgb *= v_brightness;
    pixel.rgb = mix(fogColor, pixel.rgb, fogFactor);

    gl_FragColor = pixel;
}
