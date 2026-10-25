#version 120
uniform vec2 uHalf;  // half size of the shape, pixels
uniform float uRadius;  // corner radius, pixels
uniform float uBorder;  // > 0: outline of this width instead of a fill
uniform float uSoft;  // > 0: soft shadow with this falloff
uniform float uChecker;  // > 0: grey checkerboard with this cell size
uniform float uPremul;  // 1: premultiplied output (the crosshair's invert and additive blends)
varying vec2 vLocal;  // pixel offset from the shape's center
#include "gui/ui_fade.glsl"
float roundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}
void main() {
    float d = roundBox(vLocal, uHalf, uRadius);
    float cover;
    if (uSoft > 0.0) {
        cover = 1.0 - smoothstep(-uSoft, uSoft, d);
    } else {
        cover = clamp(0.5 - d, 0.0, 1.0);
        if (uBorder > 0.0) cover *= clamp(0.5 + d + uBorder, 0.0, 1.0);
    }
    vec4 color = gl_Color;
    if (uChecker > 0.0) {
        vec2 cell = floor((vLocal + uHalf) / uChecker);
        color.rgb = mix(vec3(0.80), vec3(0.58), mod(cell.x + cell.y, 2.0));
    }
    float alpha = color.a * cover * fadeMask();
    gl_FragColor = uPremul > 0.5 ? vec4(color.rgb * alpha, alpha) : vec4(color.rgb, alpha);
}
