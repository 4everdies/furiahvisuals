#version 120
uniform sampler2D uTex;
// Blocks on the window from the portrait's coordinates: b = uv * uSpan + uOff (across, up from the ground).
uniform vec2 uSpan;
uniform vec2 uOff;
uniform float uCell;
uniform float uLeadW;
uniform vec3 uLead;
uniform vec3 uSet0;
uniform vec3 uSet1;
uniform vec3 uSet2;
uniform vec3 uSet3;
uniform vec3 uBack0;
uniform vec3 uBack1;
uniform vec3 uBack2;
uniform vec3 uBack3;
uniform float uPalette;
uniform float uPortrait;
uniform float uLight;
uniform vec3 uLightColor;
uniform float uGone;
uniform vec2 uRange;
uniform float uGlow;
uniform float uDim;
uniform float uOpacity;
varying vec2 vUv;
varying vec4 vColor;
vec2 jitter(vec2 p) {
    vec2 h = vec2(dot(p, vec2(113.7, 271.9)), dot(p, vec2(197.3, 61.1)));
    return 0.15 + 0.7 * fract(sin(h) * 35791.123);
}
float rand(vec2 p) {
    return fract(sin(dot(p, vec2(37.17, 211.3))) * 24683.71);
}
float smooth2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(rand(i), rand(i + vec2(1.0, 0.0)), f.x), mix(rand(i + vec2(0.0, 1.0)), rand(i + vec2(1.0, 1.0)), f.x), f.y);
}
// Hue (0..1), saturation, value, worked out from the channels' largest and smallest.
vec3 hsvOf(vec3 c) {
    float top = max(c.r, max(c.g, c.b));
    float low = min(c.r, min(c.g, c.b));
    float span = top - low;
    float h = 0.0;
    if (span > 0.00001) {
        if (top == c.r) {
            h = mod((c.g - c.b) / span, 6.0);
        } else if (top == c.g) {
            h = (c.b - c.r) / span + 2.0;
        } else {
            h = (c.r - c.g) / span + 4.0;
        }
    }
    return vec3(h / 6.0, top > 0.00001 ? span / top : 0.0, top);
}
vec3 rgbOf(vec3 hsv) {
    vec3 k = clamp(abs(mod(hsv.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return hsv.z * mix(vec3(1.0), k, hsv.y);
}
float hueGap(float a, float b) {
    float d = abs(a - b);
    return min(d, 1.0 - d);
}
// The figure's color as glass: from the skin (richer, brighter), the Cathedral set's nearest hue, or Tinted by brightness.
vec3 asGlass(vec3 c) {
    vec3 hsv = hsvOf(c);
    if (uPalette < 0.5) {
        return rgbOf(vec3(hsv.x, min(1.0, hsv.y * 1.25 + 0.12), 0.5 + 0.5 * hsv.z));
    }
    if (uPalette > 1.5) {
        float l = dot(c, vec3(0.299, 0.587, 0.114));
        return l < 0.33 ? uSet0 : l < 0.62 ? uSet1 : uSet2;
    }
    if (hsv.y < 0.18) {
        return hsv.z < 0.35 ? uSet1 : hsv.z < 0.6 ? uSet2 : uSet3;
    }
    vec3 best = uSet0;
    float gap = hueGap(hsv.x, hsvOf(uSet0).x);
    float g1 = hueGap(hsv.x, hsvOf(uSet1).x);
    if (g1 < gap) { gap = g1; best = uSet1; }
    float g2 = hueGap(hsv.x, hsvOf(uSet2).x);
    if (g2 < gap) { gap = g2; best = uSet2; }
    float g3 = hueGap(hsv.x, hsvOf(uSet3).x);
    if (g3 < gap) { best = uSet3; }
    return best;
}
void main() {
    vec2 b = vUv * uSpan + uOff;
    vec2 q = b / uCell;
    vec2 n = floor(q);
    vec2 f = fract(q);
    // The nearest seed...
    vec2 mg = vec2(0.0);
    vec2 mr = vec2(0.0);
    float md = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 g = vec2(float(i), float(j));
            vec2 r = g + jitter(n + g) - f;
            float d = dot(r, r);
            if (d < md) {
                md = d;
                mr = r;
                mg = g;
            }
        }
    }
    // ...and how far the nearest border is (to the halfway line toward each neighbour seed).
    float border = 8.0;
    for (int j = -2; j <= 2; j++) {
        for (int i = -2; i <= 2; i++) {
            vec2 g = mg + vec2(float(i), float(j));
            vec2 r = g + jitter(n + g) - f;
            vec2 e = r - mr;
            if (dot(e, e) > 0.00001) {
                border = min(border, dot(0.5 * (mr + r), normalize(e)));
            }
        }
    }
    vec2 cell = n + mg;
    vec2 seed = (cell + jitter(cell)) * uCell;
    float pick = rand(cell + vec2(3.7, 1.3));
    // The dissolve: one cell at a time, the higher ones first.
    float up = clamp((seed.y - uRange.x) / max(uRange.y, 0.01), 0.0, 1.0);
    float order = 0.6 * pick + 0.4 * (1.0 - up);
    float keep = clamp((order + 0.08 - uGone) / 0.08, 0.0, 1.0);
    if (keep <= 0.0) discard;
    // The figure: where the portrait covers this spot. Its outline is leaded too (a spot whose neighbours a lead's
    // width away fall on the other side of it), so the figure stands out whole however the cells cut it.
    vec2 here = (b - uOff) / uSpan;
    vec2 reach = uLeadW * 1.6 * uCell / uSpan;
    float inside = 0.0;
    float outline = 0.0;
    if (uPortrait > 0.5) {
        inside = step(0.5, texture2D(uTex, here).a);
        float n1 = step(0.5, texture2D(uTex, here + vec2(reach.x, 0.0)).a);
        float n2 = step(0.5, texture2D(uTex, here - vec2(reach.x, 0.0)).a);
        float n3 = step(0.5, texture2D(uTex, here + vec2(0.0, reach.y)).a);
        float n4 = step(0.5, texture2D(uTex, here - vec2(0.0, reach.y)).a);
        outline = step(0.5, abs(n1 - inside) + abs(n2 - inside) + abs(n3 - inside) + abs(n4 - inside));
    }
    // The color: inside the figure, the figure's at the cell's seed (or here, where the seed lies outside it);
    // round it, the window's own glass.
    vec3 glass;
    if (inside > 0.5) {
        vec2 at = (seed - uOff) / uSpan;
        vec4 s = texture2D(uTex, clamp(at, 0.0, 1.0));
        if (s.a < 0.5 || at.x < 0.0 || at.x > 1.0 || at.y < 0.0 || at.y > 1.0) {
            s = texture2D(uTex, here);
        }
        glass = asGlass(s.rgb);
    } else {
        float k = floor(rand(cell + vec2(9.1, 4.2)) * 3.999);
        glass = k < 0.5 ? uBack0 : k < 1.5 ? uBack1 : k < 2.5 ? uBack2 : uBack3;
    }
    float mid = clamp(1.0 - sqrt(md) * 1.4, 0.0, 1.0);
    float streak = (smooth2(b * vec2(7.0, 1.6) / max(uCell, 0.01) * 0.12) - 0.5) * 0.14;
    vec3 col = glass * (0.82 + 0.24 * mid + streak);
    float w = fwidth(border) * 0.8;
    float lead = max(1.0 - smoothstep(uLeadW - w, uLeadW + w, border), outline);
    if (uGlow > 0.0) {
        // Only its light, added: the glass's own color, brighter in the cells' middles; the lead lets none through.
        gl_FragColor = vec4(col * (0.55 + 0.45 * mid) * uGlow * (1.0 - lead), keep * vColor.a);
        return;
    }
    col = col * uDim * (1.0 + 0.35 * uLight) + uLightColor * (0.07 * uLight);
    vec3 leadCol = uLead * uDim * (1.0 + 0.3 * uLight);
    gl_FragColor = vec4(mix(col, leadCol, lead), mix(uOpacity, 1.0, lead) * keep * vColor.a);
}
