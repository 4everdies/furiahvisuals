#version 120
uniform sampler2D uTex;
uniform vec2 uDir;
uniform vec2 uMax;
varying vec2 vUv;
vec3 at(vec2 uv) { return texture2D(uTex, clamp(uv, vec2(0.0), uMax)).rgb; }
void main() {
    vec3 c = at(vUv) * 0.2270270270;
    c += at(vUv + uDir * 1.3846153846) * 0.3162162162;
    c += at(vUv - uDir * 1.3846153846) * 0.3162162162;
    c += at(vUv + uDir * 3.2307692308) * 0.0702702703;
    c += at(vUv - uDir * 3.2307692308) * 0.0702702703;
    gl_FragColor = vec4(c, 1.0);
}
