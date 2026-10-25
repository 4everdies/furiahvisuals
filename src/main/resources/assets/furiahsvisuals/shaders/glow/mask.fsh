#version 120
// Solid white wherever the drawn texture is opaque enough (Alpha cutoff): the exact silhouette of what is on
// screen, spikes, notches and holes included.
uniform sampler2D uTex;
uniform float uCutoff;
varying vec2 vUv;
varying float vAlpha;

void main() {
    if (texture2D(uTex, vUv).a * vAlpha < uCutoff) {
        discard;
    }
    gl_FragColor = vec4(1.0);
}
