#version 120
uniform sampler2D uTex;
uniform vec2 uTexel;
varying vec2 vUv;
void main() {
    vec3 c = texture2D(uTex, vUv + uTexel * vec2(-1.0, -1.0)).rgb;
    c += texture2D(uTex, vUv + uTexel * vec2(1.0, -1.0)).rgb;
    c += texture2D(uTex, vUv + uTexel * vec2(-1.0, 1.0)).rgb;
    c += texture2D(uTex, vUv + uTexel * vec2(1.0, 1.0)).rgb;
    gl_FragColor = vec4(c * 0.25, 1.0);
}
