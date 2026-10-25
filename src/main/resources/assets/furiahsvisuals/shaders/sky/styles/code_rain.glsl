// @colors 3DFF6E C8FFD8
// Code Rain: columns of glowing characters fall from high in the sky to the horizon: a bright character leading each
// trail and the ones behind it fading, characters flickering into others. The glyphs are the mod's own (strokes on a
// small grid, like katakana, digits and symbols), drawn as lines, so they are sharp at any size. The columns meet
// overhead, so the rain fades out before the zenith, into a faint haze where it comes from.
// Options: uOptA.x column density, uOptA.y fall speed, uOptA.z tail length, uOptA.w glyph size.

// Distance to a glyph (cell coordinates 0..1, y up): two to five strokes between points of a 3 x 4 grid.
float glyph(vec2 p, float seed) {
    float dist = 9.0;
    for (int i = 0; i < 5; i++) {
        vec3 h = hash33(vec3(seed, float(i) * 3.7, 11.0));
        if (i > 1 && h.z < 0.4) {
            continue;
        }
        vec2 a = vec2(floor(h.x * 3.0), floor(h.y * 4.0));
        vec3 h2 = hash33(vec3(seed, float(i) * 5.3, 29.0));
        vec2 b = clamp(a + vec2(floor(h2.x * 3.0) - 1.0, floor(h2.y * 3.0) - 1.0) * vec2(1.0, 2.0), vec2(0.0), vec2(2.0, 3.0));
        if (length(a - b) < 0.5) {
            b = vec2(a.x, 3.0 - a.y);
            if (length(a - b) < 0.5) {
                b = vec2(2.0 - a.x, a.y);
            }
        }
        vec2 A = vec2(0.2, 0.14) + a * vec2(0.3, 0.24);
        vec2 B = vec2(0.2, 0.14) + b * vec2(0.3, 0.24);
        dist = min(dist, segDist(p, A, B));
    }
    return dist;
}

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime;
    float el = elevation(d);
    float cols = max(24.0, floor(150.0 * uOptA.x / (uOptA.w * uScale)));
    float cw = TAU / cols;
    float chh = cw * 1.3;
    vec2 g = vec2(azimuth(d) / cw, el / chh);
    vec2 id = floor(g);
    vec2 f = g - id;
    // The same column on both sides of the azimuth's wrap (south): no seam there.
    id.x = mod(id.x, cols);
    float top = floor(1.32 / chh);
    vec3 cs = hash33(vec3(id.x, 7.0, 3.0));
    float speed = (0.7 + 1.3 * cs.x) * uOptA.y * 5.0;
    float tail = max(2.0, (5.0 + 13.0 * cs.y) * uOptA.z);
    float span = top + tail + 6.0;
    float bright = 0.0;
    float lead = 0.0;
    // Two drops in each column, each falling at the column's speed.
    for (int k = 0; k < 2; k++) {
        float off = hash12(vec2(id.x, float(k) * 13.0 + 1.0)) * span;
        float head = top - mod(t * speed + off + float(k) * span * 0.5, span);
        float behind = id.y - floor(head);
        if (behind >= 0.0 && behind < tail) {
            bright = max(bright, pow(1.0 - behind / tail, 1.6));
        }
        if (abs(behind) < 0.5) {
            lead = 1.0;
        }
    }
    if (id.y < 0.0 || id.y > top) {
        bright = 0.0;
        lead = 0.0;
    }
    // The characters change now and then; the leading one more often.
    float rate = 0.5 + 2.2 * hash12(id) + lead * 6.0;
    float change = floor(t * rate + hash12(id + 7.0) * 10.0);
    float gd = glyph(f, floor(hash13(vec3(id, change)) * 4096.0));
    float stroke = lineAA(gd, 0.055);
    float halo = 0.02 / (0.02 + gd * gd * 50.0);
    vec3 col = vec3(0.0, 0.006, 0.002);
    col += uColA * (stroke * 0.95 + halo * 0.22) * bright;
    col += uColB * (stroke * 1.4 + halo * 0.5) * lead;
    // A green glow low over the horizon; the rain fades out high up (the columns meet overhead) into a faint shimmering
    // haze where it comes from.
    col *= 1.0 - smoothstep(0.95, 1.32, el);
    col += uColA * 0.035 * exp(-max(el, 0.0) * 9.0);
    col += uColA * (0.025 + 0.045 * (0.5 + 0.5 * noise(d * 9.0 + vec3(0.0, t * 0.6, 0.0)))) * smoothstep(1.0, 1.45, el);
    vec3 hz = mix(vec3(0.0039, 0.0157, 0.0078), uColA, 0.1);
    return horizon(col, d, hz);
}
