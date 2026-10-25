// @colors FF2BD6 FFC82B
// Circuit Maze: a near-black sky covered in a dense maze of tiny right-angled circuit traces, laid on the faces of a
// cube (no pinching overhead). Clusters of them light up and fade, bright pulses run along them, some flicker, and the
// colors cycle from magenta through red and orange to yellow (or between your two colors).
// Options: uOptA.x trace density, uOptA.y pulses, uOptA.z flicker.

vec3 circuitColor(float k) {
    if (uCustom > 0.5) {
        return mix(uColA, uColB, k);
    }
    // magenta -> red -> orange -> yellow
    return hsv(fract(mix(-0.12, 0.135, k)), 0.86, 1.0);
}

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime;
    vec3 cf = cubeFace(d);
    float face = cf.z;
    // An even number of cells, so the faces meet on cell borders.
    float cells = max(8.0, floor(30.0 * uOptA.x / uScale) * 2.0);
    vec2 g = (cf.xy * 0.5 + 0.5) * cells;
    vec2 id = floor(g);
    vec2 f = g - id;
    // Regions a few cells wide lean towards horizontal or vertical runs: long parallel traces, like a real board.
    float lean = noise(vec3(id * 0.11, face * 3.7 + 0.5));
    float ph = 0.44 + 0.34 * lean;
    float pv = 0.44 - 0.34 * lean;
    // Each link between two cells is on or off (the hash of that link, shared by both cells).
    float right = step(hash13(vec3(id, face + 11.0)), ph);
    float left = step(hash13(vec3(id - vec2(1.0, 0.0), face + 11.0)), ph);
    float up = step(hash13(vec3(id, face + 23.0)), pv);
    float down = step(hash13(vec3(id - vec2(0.0, 1.0), face + 23.0)), pv);
    float dh = 9.0;
    float dv = 9.0;
    if (right + left > 0.5) {
        dh = segDist(f, vec2(left > 0.5 ? 0.0 : 0.5, 0.5), vec2(right > 0.5 ? 1.0 : 0.5, 0.5));
    }
    if (up + down > 0.5) {
        dv = segDist(f, vec2(0.5, down > 0.5 ? 0.0 : 0.5), vec2(0.5, up > 0.5 ? 1.0 : 0.5));
    }
    float links = right + left + up + down;
    // Pads: a ring where a trace ends, a dot where three or four meet.
    float r = length(f - 0.5);
    float pad = 9.0;
    if (links > 0.5 && links < 1.5) {
        pad = abs(r - 0.12);
    } else if (links > 2.5) {
        pad = max(0.0, r - 0.1);
    }
    float line = min(min(dh, dv), pad);
    float core = lineAA(line, 0.055);
    float halo = 0.045 / (0.045 + line * line * 26.0);

    // Clusters light up and fade; a few traces flicker.
    float cluster = smoothstep(-0.3, 0.5, noise(vec3(id * 0.06 + face * 5.3, t * 0.13)));
    float flick = 1.0 - uOptA.z * 0.75 * step(0.88, hash13(vec3(id, floor(t * 11.0) + face * 3.0)));
    float on = cluster * flick;

    // Pulses running along the traces (in the direction of the nearest one).
    float along = dh < dv ? g.x + hash12(vec2(id.y, face)) * 29.0 : g.y + hash12(vec2(id.x, face + 7.0)) * 31.0;
    float lane = floor(along * 0.07);
    float pp = fract(along * 0.07 - t * 0.35 * (0.6 + 0.8 * hash12(vec2(lane, face + 3.0))));
    float pulse = pow(pp, 14.0) * step(0.35, hash12(vec2(lane, face + 9.0)));

    // The palette moves across the sky and through time.
    float k = fract(t * 0.035 + noise(vec3(id * 0.018, face * 2.1)) * 0.45 + face * 0.13);
    vec3 base = circuitColor(abs(k * 2.0 - 1.0));

    vec3 col = vec3(0.012, 0.006, 0.015);
    col += base * halo * 0.3 * (0.2 + on);
    col += mix(base, hot(base), 0.45) * core * (0.1 + 0.9 * on);
    col += hot(base) * core * pulse * uOptA.y * 1.6;
    col += base * halo * pulse * uOptA.y * 0.5;
    vec3 hz = mix(vec3(0.0157, 0.0078, 0.0235), uColA, 0.1);
    return horizon(col, d, hz);
}
