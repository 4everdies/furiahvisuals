#version 120
uniform vec4 uRing;  // inner radius, outer radius, half angle (rad; 0: a whole ring), half gap (px)
uniform vec2 uDir;  // the slot's middle direction (unit, y down)
uniform vec2 uSinCos;  // sin and cos of the half angle
uniform vec2 uLift;  // the shape moved by this (px)
uniform float uRound;  // corner radius (px)
uniform vec4 uMode;  // mode, border width or glow reach (px)
uniform vec4 uGlint;  // axis, band middle, band half width (px)
uniform float uTrail;
uniform sampler2D uBg;  // the glass blur (the island's capture of the screen)
uniform vec4 uViewport;
uniform vec2 uUvScale;
uniform vec2 uUvMax;
uniform vec4 uTint;  // glass tint, alpha = strength
uniform vec4 uBorderColor;  // a fill's border (fill and border in one)
varying vec2 vLocal;  // pixels from the wheel's middle (y down)
float shape(vec2 p) {
    float len = length(p);
    float k = uRound;
    float dr = uRing.x > 0.0 ? max(len - (uRing.y - k), (uRing.x + k) - len) : len - (uRing.y - k);
    if (uRing.z <= 0.0) return dr - k;
    vec2 q = vec2(dot(p, uDir), abs(dot(p, vec2(-uDir.y, uDir.x))));
    float da = (uRing.w + k) - (q.x * uSinCos.x - q.y * uSinCos.y);
    vec2 dd = vec2(dr, da);
    return length(max(dd, 0.0)) + min(max(dd.x, dd.y), 0.0) - k;
}
void main() {
    float d = shape(vLocal - uLift);
    float mode = uMode.x;
    vec3 rgb = gl_Color.rgb;
    float a = clamp(0.5 - d, 0.0, 1.0);
    if (mode > 4.5) {
        // The fill with its border over it: the border's band (uMode.y wide) along the edge.
        float ba = uBorderColor.a * clamp(0.5 + d + uMode.y, 0.0, 1.0);
        float fa = gl_Color.a;
        float oa = ba + fa * (1.0 - ba);
        vec3 c = oa > 0.0 ? (uBorderColor.rgb * ba + gl_Color.rgb * fa * (1.0 - ba)) / oa : gl_Color.rgb;
        gl_FragColor = vec4(c, oa * a);
        return;
    }
    if (mode > 3.5) {
        float t = (dot(vLocal, uGlint.xy) - uGlint.z) / uGlint.w;
        float s = t + 1.9 * uTrail;
        a *= min(1.0, exp(-t * t * 2.5) + 0.45 * exp(-s * s * 9.0));
    } else if (mode > 2.5) {
        vec2 uv = (gl_FragCoord.xy - uViewport.xy) / uViewport.zw;
        vec3 col = texture2D(uBg, clamp(uv * uUvScale, vec2(0.0), uUvMax)).rgb;
        float l = dot(col, vec3(0.2126, 0.7152, 0.0722));
        col = max(mix(vec3(l), col, 1.2), 0.0);
        float tint = uTint.a * (1.0 + 0.6 * smoothstep(0.35, 1.0, l));
        rgb = mix(col, uTint.rgb, clamp(tint, 0.0, 0.94));
    } else if (mode > 1.5) {
        float t = clamp(max(d, 0.0) / uMode.y, 0.0, 1.0);
        a = (1.0 - t) * (1.0 - t) * clamp(d + 0.5, 0.0, 1.0);
    } else if (mode > 0.5) {
        a *= clamp(0.5 + d + uMode.y, 0.0, 1.0);
    }
    gl_FragColor = vec4(rgb, gl_Color.a * a);
}
