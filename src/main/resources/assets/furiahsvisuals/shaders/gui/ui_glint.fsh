#version 120
uniform vec2 uHalf;  // half size of the box, pixels
uniform float uRadius;  // corner radius, pixels
uniform vec2 uAxis;  // unit vector across the band
uniform vec3 uBand;  // band center along uAxis and half width (pixels), trailing side
varying vec2 vLocal;
#include "gui/ui_fade.glsl"
float roundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}
void main() {
    float cover = clamp(0.5 - roundBox(vLocal, uHalf, uRadius), 0.0, 1.0);
    float t = (dot(vLocal, uAxis) - uBand.x) / uBand.y;
    float s = t + 1.9 * uBand.z;
    float shine = min(1.0, exp(-t * t * 2.5) + 0.45 * exp(-s * s * 9.0));
    gl_FragColor = vec4(gl_Color.rgb, gl_Color.a * cover * shine * fadeMask());
}
