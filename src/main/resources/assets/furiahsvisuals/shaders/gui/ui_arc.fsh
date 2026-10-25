#version 120
uniform vec2 uArc;  // radius, half thickness (pixels)
uniform vec2 uAngles;  // start, sweep (radians, clockwise from 12 o'clock)
uniform float uRound;  // 1: round ends
uniform float uPremul;  // 1: premultiplied output
varying vec2 vLocal;  // pixel offset from the center (y down)
#include "gui/ui_fade.glsl"
void main() {
    float r = length(vLocal);
    float d = abs(r - uArc.x) - uArc.y;
    float cover = clamp(0.5 - d, 0.0, 1.0);
    if (uAngles.y < 6.2831) {
        float a = atan(vLocal.x, -vLocal.y);
        float rel = mod(a - uAngles.x, 6.2831853);
        if (rel > uAngles.y) {
            // Outside the arc: how far past its nearer end.
            float toEnd = rel - uAngles.y;
            float toStart = 6.2831853 - rel;
            if (uRound > 0.5) {
                float e = toEnd < toStart ? uAngles.x + uAngles.y : uAngles.x;
                vec2 p = vec2(sin(e), -cos(e)) * uArc.x;
                cover = clamp(0.5 - (length(vLocal - p) - uArc.y), 0.0, 1.0);
            } else {
                cover *= clamp(0.5 - min(toEnd, toStart) * max(r, 1.0), 0.0, 1.0);
            }
        }
    }
    float alpha = gl_Color.a * cover * fadeMask();
    gl_FragColor = uPremul > 0.5 ? vec4(gl_Color.rgb * alpha, alpha) : vec4(gl_Color.rgb, alpha);
}
