// @colors FF3DB8 FFFFFF
// Megastructure: inside a vast dark structure of metal panels and girders, lit by thousands of tiny pink, magenta and
// white lights that twinkle. You drift slowly through it: three layers of it pass at their own speeds (the near ones
// faster), the near panels hiding the far ones.
// Options: uOptA.x light density, uOptA.y drift speed, uOptA.z drift direction (compass degrees), uOptA.w panel
//          darkness; uOptB.x twinkle.
//
// Cost: the layers are drawn front to back, and where the near panels cover the view nothing behind them is computed;
// the far layers have fewer lights per cell (they are tiny there anyway), and anti-aliasing uses one pixel size per
// layer (from how fast the view direction changes across a pixel) instead of a derivative per shape.

// Anti-aliased line of half width hw at distance dist, px being a pixel in the same units.
float megaLine(float dist, float hw, float px) {
    return (1.0 - smoothstep(hw - px, hw + px, dist)) * min(1.0, hw / px);
}

// One layer on a cube around the viewer: its color (panels, girders, lights) and how much of it is solid (a).
// pxAngle is the size of a pixel in radians; lights is how many lights a cell may have (1 to 3).
vec4 megaLayer(vec3 d, float cells, float seed, float near, float lights, float pxAngle) {
    vec3 cf = cubeFace(d);
    vec2 g = (cf.xy * 0.5 + 0.5) * cells;
    vec2 id = floor(g);
    vec2 f = g - id;
    // A pixel in cell units (a cube face stretches towards its edges).
    float px = pxAngle * cells * 0.5 * (1.0 + 0.5 * dot(cf.xy, cf.xy)) + 1e-4;
    vec3 h = hash33(vec3(id, cf.z * 13.0 + seed));
    // A panel: a box inset in its cell; some cells are open (you see deeper through them).
    vec2 hs = 0.27 + 0.2 * h.xy;
    vec2 q = abs(f - 0.5) - hs;
    float box = max(q.x, q.y);
    float present = step(0.3 - 0.12 * near, h.z);
    float inside = (1.0 - smoothstep(-px, px, box)) * present;
    // Dark metal, a little lighter at the top, each panel its own shade, with a rim catching the light.
    float shade = (0.045 + 0.05 * f.y + 0.025 * (h.x - 0.5)) * mix(1.0, 0.3, uOptA.w);
    vec3 metal = vec3(shade) * vec3(0.92, 0.86, 1.0);
    float rim = megaLine(abs(box), 0.016, px) * present;
    // Girders along some rows and columns of cells.
    vec2 e = min(f, 1.0 - f);
    float girder = megaLine(min(e.x, e.y), 0.04, px) * step(0.45, hash13(vec3(floor(g * 0.5), seed + 3.0)));
    vec3 col = metal * inside + mix(uColA, uColB, 0.3) * rim * 0.08 + vec3(0.035, 0.03, 0.045) * girder;
    float cover = max(inside, girder * 0.9);
    // Lights on the panels and the girders, twinkling.
    float sz = 0.028 + 0.02 * near;
    float s = max(sz, px) * 1.5;
    float small = min(1.0, sz / px);
    for (int i = 0; i < 3; i++) {
        if (float(i) >= lights) {
            break;
        }
        vec3 r = hash33(vec3(id, seed + float(i) * 7.1 + cf.z * 3.3));
        if (r.x > uOptA.x * 0.95) {
            continue;
        }
        vec2 v = f - (0.12 + 0.76 * r.yz);
        float dd = dot(v, v);
        float k = max(0.0, 1.0 - dd / (s * s));
        float spot = k * k * small;
        float tw = 1.0 - uOptB.x * 0.85 * abs(fract(uTime * (0.24 + 0.8 * r.x) + r.y * 6.4) * 2.0 - 1.0);
        vec3 lc = mix(uColA, uColB, fract(r.z * 3.7)) * (1.1 + near);
        col += lc * (spot * 1.4 + 0.0025 / (0.0025 + dd * 30.0) * 0.25) * tw;
    }
    return vec4(col, cover);
}

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime * 0.012 * uOptA.y;
    float az = radians(uOptA.z);
    // Drifting that way: the structure turns past overhead, the near layers faster.
    vec3 toward = vec3(sin(az), 0.0, -cos(az));
    vec3 axis = normalize(cross(toward, vec3(0.0, 1.0, 0.0)));
    float sc = 1.0 / uScale;
    float pxAngle = length(fwidth(d));
    // Front to back: what shows through the near layer, then through the middle one.
    vec4 near = megaLayer(rotAxis(d, axis, t * 2.3 + 3.1), 17.0 * sc, 9.0, 1.0, 3.0, pxAngle);
    vec3 col = near.rgb;
    float through = 1.0 - near.a;
    if (through > 0.004) {
        vec4 mid = megaLayer(rotAxis(d, axis, t * 1.1 + 1.7), 38.0 * sc, 5.0, 0.5, 2.0, pxAngle);
        col += through * mid.rgb;
        through *= 1.0 - mid.a;
        if (through > 0.004) {
            vec4 far = megaLayer(rotAxis(d, axis, t * 0.5), 76.0 * sc, 1.0, 0.0, 1.0, pxAngle);
            // Far away the structure is lost in a pink haze.
            vec3 haze = mix(vec3(0.02, 0.01, 0.03), uColA * 0.1, 0.5 + 0.5 * noise(d * 1.5));
            col += through * (mix(far.rgb, uColA * 0.08, 0.35 * far.a) + (1.0 - far.a) * haze);
        }
    }
    vec3 hz = mix(vec3(0.0275, 0.0196, 0.0471), uColA, 0.14);
    return horizon(col, d, hz);
}
