#version 120
uniform sampler2D uTex;
uniform vec2 uTexel;
uniform vec2 uMax;
varying vec2 vUv;
vec3 at(vec2 uv) { return texture2D(uTex, clamp(uv, vec2(0.0), uMax)).rgb; }
void main() {
    vec3 c = at(vUv + uTexel * vec2(-1.0, -1.0)) + at(vUv + uTexel * vec2(1.0, -1.0))
           + at(vUv + uTexel * vec2(-1.0, 1.0)) + at(vUv + uTexel * vec2(1.0, 1.0));
    gl_FragColor = vec4(c * 0.25, 1.0);
}
