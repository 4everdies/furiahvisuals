#version 120
uniform sampler2D uTex;
uniform vec2 uHalf;
uniform float uRadius;
varying vec2 vLocal;
varying vec2 vUv;
#include "gui/ui_fade.glsl"
float roundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}
void main() {
    float cover = clamp(0.5 - roundBox(vLocal, uHalf, uRadius), 0.0, 1.0);
    vec4 c = texture2D(uTex, vUv) * gl_Color;
    gl_FragColor = vec4(c.rgb, c.a * cover * fadeMask());
}
