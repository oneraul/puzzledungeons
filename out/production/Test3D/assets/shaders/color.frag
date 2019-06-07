#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 u_diffuseColor;

varying vec4 v_viewSpace;
varying float v_brightness;

const vec3 fogColor = vec3(0.05, 0.1, 0.15);
const float fogStart = 18.0;
const float fogEnd = 24.0;

void main() {
    float distanceToCamera = length(v_viewSpace);
    if(distanceToCamera > fogEnd) discard;

    float fogFactor = (fogEnd-distanceToCamera)/(fogEnd-fogStart);
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec4 pixel = u_diffuseColor;
    pixel.rgb *= v_brightness;
    pixel.rgb = mix(fogColor, pixel.rgb, fogFactor);

    gl_FragColor = pixel;
}
