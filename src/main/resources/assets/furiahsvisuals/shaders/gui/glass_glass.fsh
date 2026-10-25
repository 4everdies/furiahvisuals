#version 120
#include "gui/glass_shape_common.glsl"
uniform sampler2D uSharp;  // the world, not blurred (for the polished rim)
uniform vec2 uSharpScale;
uniform float uBevel;  // width of the curved rim that bends light, pixels
uniform float uRefract;  // how far beyond its edge the rim gathers the world, pixels
uniform float uHair;  // width of the lit hairline, pixels
uniform vec4 uTint;  // tint color, alpha = strength
uniform vec4 uRim;  // color of the light on the rim, alpha = intensity
uniform vec2 uLight;  // direction the light comes from (unit vector, y down)
vec3 sharp(vec2 uv) { return texture2D(uSharp, clamp(uv, vec2(0.0), vec2(1.0)) * uSharpScale).rgb; }
void main() {
    float d = roundBox(vLocal, uHalf, uRadius);
    float cover = clamp(0.5 - d, 0.0, 1.0);
    if (cover <= 0.0) discard;
    float inside = max(-d, 0.0);
    // Outward normal: radial in the rounded corners, straight out along the sides.
    vec2 q = abs(vLocal) - uHalf + vec2(uRadius);
    vec2 s = vec2(vLocal.x < 0.0 ? -1.0 : 1.0, vLocal.y < 0.0 ? -1.0 : 1.0);
    vec2 n = (q.x > 0.0 && q.y > 0.0) ? normalize(q) * s : (q.x > q.y ? vec2(s.x, 0.0) : vec2(0.0, s.y));
    // Lens: the curved rim gathers in the world from just beyond the edge, squeezed into a thin
    // band. In the corners it gathers radially and reaches further, so the world visibly bends
    // around the curve.
    float edge = 1.0 - clamp(inside / uBevel, 0.0, 1.0);
    float corner = smoothstep(-0.7 * uRadius, 0.25 * uRadius, min(q.x, q.y));
    float bend = uRefract * edge * edge * (1.0 + 1.2 * corner);
    vec2 uv = screenUv();
    vec2 shift = vec2(n.x, -n.y) * bend / uViewport.zw;
    // Glass splits colors a little where it bends the most.
    vec2 uvR = uv + shift, uvG = uv + shift * 0.9, uvB = uv + shift * 0.8;
    vec3 col = vec3(bg(uvR).r, bg(uvG).g, bg(uvB).b);
    // The face is frosted, the rim polished: towards the edge the sharp image takes over.
    float clear = 0.85 * edge * edge;
    if (clear > 0.004) {
        col = mix(col, vec3(sharp(uvR).r, sharp(uvG).g, sharp(uvB).b), clear);
    }
    // Vibrancy, then the tint (a little lighter on the polished rim); over bright areas the tint
    // gets stronger so text stays readable.
    float l = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col = max(mix(vec3(l), col, 1.25), 0.0);
    float tint = uTint.a * (1.0 + 0.7 * smoothstep(0.35, 1.0, l)) * (1.0 - 0.3 * clear);
    col = mix(col, uTint.rgb, clamp(tint, 0.0, 0.92));
    // Light: a crisp hairline on the rim (brightest facing the light, a softer bounce on the far
    // side), a glow inside the curved rim, a little shade on the far side and a soft top sheen.
    float facing = dot(n, uLight);
    float band = edge * edge * edge;
    col *= 1.0 - 0.14 * band * max(-facing, 0.0);
    float hair = 1.0 - smoothstep(0.0, uHair * 1.6, inside);
    vec3 rim = uRim.rgb * uRim.a;
    col += rim * hair * (0.14 + 0.72 * max(facing, 0.0) + 0.34 * max(-facing, 0.0));
    col += rim * band * (0.025 + 0.13 * max(facing, 0.0) + 0.04 * max(-facing, 0.0));
    float top = 1.0 - clamp((vLocal.y + uHalf.y) / (2.0 * uHalf.y), 0.0, 1.0);
    col += vec3(0.05) * top * top * (1.0 - band);
    gl_FragColor = vec4(col + dither(), cover * uAlpha);
}
