// @colors E0457E F3E9EC
// Acrylic Pour: fluid-art cells of pearl and pink drifting very slowly across black, trailing fine pink lace.
vec3 skyColor(vec3 d) {
    vec3 p0 = d * (1.5 / uScale) + uParallax * 0.25 + vec3(uTime * 0.008, 0.0, 0.0);
    vec3 p = stir(p0, uTime * 0.02, 0.2, 0.03);
    vec3 col = vec3(0.012, 0.006, 0.01);
    // Paint flows in broad streams; between them the black shows.
    float stream = fbmN(p * 0.7 + vec3(7.0, 3.0, 1.0), 3.0);
    float paint = smoothstep(-0.12, 0.1, stream);
    // Lace: fine pink threads along the flow, densest where the paint thins out.
    float s = p.y * 9.0 + p.x * 0.8;
    float x = fract(s) - 0.5;
    float id = floor(s);
    float thread = exp(-x * x / 0.0012) * step(0.3, hash12(vec2(id, 2.0)));
    float lace = smoothstep(-0.4, -0.05, stream) * (1.0 - smoothstep(0.1, 0.35, stream) * 0.5);
    col += uColA * thread * lace * 1.1;
    col += uColA * thread * 0.12;
    // Cells: round pearly blobs with pink rims; they only drift a little with the flow, so they stay round.
    // Only the 2x2x2 cells around the pixel are searched: every cell point within 0.4 cells of it (all the blobs
    // are smaller than that) lies in one of them.
    vec3 g = (p0 + (p - p0) * 0.35) * 2.2;
    vec3 gi = floor(g - 0.5);
    float best = 8.0;
    vec3 bestId = vec3(0.0);
    vec3 bestR = vec3(0.0);
    for (int z = 0; z <= 1; z++) {
        for (int y = 0; y <= 1; y++) {
            for (int xx = 0; xx <= 1; xx++) {
                vec3 id = gi + vec3(float(xx), float(y), float(z));
                vec3 r = id + hash33(id) * 0.8 + 0.1 - g;
                float dd = dot(r, r);
                if (dd < best) {
                    best = dd;
                    bestId = id;
                    bestR = r;
                }
            }
        }
    }
    float present = step(0.4, hash13(bestId)) * smoothstep(-0.1, 0.08, stream);
    float size = 0.06 + 0.1 * hash13(bestId + 5.0);
    float inside = 1.0 - smoothstep(size * 0.75, size, best);
    float rim = smoothstep(size * 0.8, size, best) * (1.0 - smoothstep(size, size * 1.6, best));
    float grain = 0.7 + 0.3 * noise(p * 24.0) + 0.1 * noise(p * 60.0);
    // Pearl, with swirls of pink inside like a real cell, and a darker ring just inside the rim.
    float swirl = sin(sqrt(best / size) * 9.0 + noise(g * 3.0) * 4.0);
    vec3 pearl = mix(uColB, uColA, 0.1 + 0.3 * smoothstep(0.3, 1.0, swirl)) * grain;
    pearl *= 1.0 - 0.35 * smoothstep(size * 0.5, size * 0.75, best);
    col = mix(col, pearl, inside * present);
    col += uColA * rim * present;
    // Pink tendrils reaching out from each cell.
    float ang = atan(bestR.y, bestR.x);
    float tendril = exp(-abs(sin(ang * 11.0 + best * 25.0 + hash13(bestId) * 6.0)) * 7.0);
    col += uColA * 0.45 * tendril * present * smoothstep(size * 3.5, size, best) * (1.0 - inside);
    return col;
}
