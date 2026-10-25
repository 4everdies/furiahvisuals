#version 120
uniform sampler2D uTex;
uniform float uAlpha;
varying vec2 vUv;
void main() {
    gl_FragColor = vec4(texture2D(uTex, vUv).rgb, uAlpha);
}
