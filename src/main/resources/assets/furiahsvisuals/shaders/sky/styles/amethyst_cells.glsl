// @colors 9D5CFF E0C2FF
// Amethyst Cells: a violet sky of slowly shifting cells, glowing walls around darker insides like a membrane or a
// crystal. The cells drift; each also grows and shrinks, so now and then one fades into its neighbours (they merge)
// and comes back (it splits off), smoothly; light ripples across the walls. Soft, so it is drawn at half resolution
// and smoothly scaled up.
// Options: uOptA.x cell size, uOptA.y border width, uOptA.z border glow, uOptA.w drift speed; uOptB.x ripple.

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime * 0.1 * uOptA.w;
    vec3 p = d * (3.0 / (uOptA.x * uScale)) + uParallax * 0.2;
    vec3 gi = floor(p);
    float f1 = 9.0;
    float f2 = 9.0;
    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 id = gi + vec3(float(x), float(y), float(z));
                vec3 h = hash33(id);
                vec3 seed = id + 0.5 + 0.38 * sin(h * TAU + t * (0.6 + h.zxy));
                // A weighted cell: growing and shrinking over time.
                float grow = 0.12 * sin(t * (0.7 + h.x) + h.y * TAU);
                float dd = length(seed - p) - grow;
                if (dd < f1) {
                    f2 = f1;
                    f1 = dd;
                } else if (dd < f2) {
                    f2 = dd;
                }
            }
        }
    }
    float e = f2 - f1;
    float w = 0.05 * uOptA.y;
    float wall = exp(-e * e / (w * w));
    float halo = w * 4.0 / (w * 4.0 + e * e * 30.0);
    // Light rippling across the walls in slow waves.
    float ripple = 1.0 - uOptB.x * 0.55 * (0.5 + 0.5 * sin(dot(p, vec3(1.0, 0.7, -0.4)) * 2.6 - uTime * 1.1));
    // Darker insides, a little lighter towards their walls.
    vec3 inside = mix(uColA * 0.035, uColA * 0.13, smoothstep(0.05, 0.6, 1.0 - clamp(e * 1.6, 0.0, 1.0)));
    vec3 col = inside + vec3(0.01, 0.004, 0.02);
    col += uColA * halo * 0.45 * uOptA.z * ripple;
    col += mix(uColA, uColB, 0.6) * wall * uOptA.z * ripple;
    col = col / (1.0 + luma(col) * 0.25);
    vec3 hz = mix(vec3(0.051, 0.0275, 0.098), uColA, 0.25);
    return horizon(col, d, hz);
}
