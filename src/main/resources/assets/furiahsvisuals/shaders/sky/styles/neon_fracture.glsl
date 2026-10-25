// @colors FF4FB8 FFFFFF
// Neon Fracture: bright pink-white lightning cracks web the sky, meeting in star-shaped junctions; flashes run along
// them, and the web slowly re-forms as its cells wander. Brightest in a band above the horizon, darker at the zenith.
// Options: uOptA.x crack density, uOptA.y flash rate, uOptA.z band height (degrees), uOptA.w band brightness.
//
// Cost: each cell's seed swings along its own direction on a cheap smooth wave (no sines in the eight-cell search),
// and the jagged bend is two octaves from two texture fetches (each fetch gives two noises).

// A smooth wave through -1..1 with period 1, shaped like a sine (a smoothed triangle).
float fractureWave(float x) {
    float tri = abs(fract(x) - 0.5) * 4.0 - 1.0;
    return tri * (1.5 - 0.5 * tri * tri);
}

vec3 skyColor(vec3 d) {
    gStarMask = 0.3;
    float t = uTime;
    vec3 p = d * (3.0 * uOptA.x / uScale);
    // Jagged like lightning: the cells' space is bent by noise.
    vec2 n1 = noise2(p * 1.9 + vec3(0.0, 0.0, t * 0.05));
    vec2 n2 = noise2(OCT * p * 3.9 + vec3(7.1, 0.0, -t * 0.05));
    p += vec3(n1.x + 0.8 * n2.x, n1.y + 0.8 * n2.y, 0.6 * (n1.x - n2.y)) * 0.13;
    // Cells (3D Voronoi over the eight cells around the point; their seeds stay near their cells' middles, so these
    // hold every nearest one). The seeds wander slowly: the web re-forms.
    vec3 gi = floor(p - 0.5);
    float f1 = 9.0;
    float f2 = 9.0;
    float f3 = 9.0;
    vec3 id1 = vec3(0.0);
    vec3 id2 = vec3(0.0);
    for (int z = 0; z <= 1; z++) {
        for (int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 1; x++) {
                vec3 id = gi + vec3(float(x), float(y), float(z));
                vec3 h = hash33(id);
                vec3 seed = id + 0.5 + (h - 0.5) * (0.55 * fractureWave(h.x + t * 0.0127 * (0.5 + h.y)));
                vec3 v = seed - p;
                float dd = sqrt(dot(v, v));
                if (dd < f1) {
                    f3 = f2;
                    f2 = f1;
                    id2 = id1;
                    f1 = dd;
                    id1 = id;
                } else if (dd < f2) {
                    f3 = f2;
                    f2 = dd;
                    id2 = id;
                } else if (dd < f3) {
                    f3 = dd;
                }
            }
        }
    }
    float e = f2 - f1;
    float crack = lineAA(e, 0.022);
    float halo = 0.012 / (0.012 + e * e * 22.0);
    // Where three cells meet, a star: the cracks' junction lights up.
    float j = f3 - f1;
    float junction = exp(-j * j * 90.0);
    // Flashes run along each crack now and then (each crack, the pair of cells it parts, has its own timing).
    vec3 pair = id1 + id2;
    float hp = hash13(pair);
    vec3 dir = normalize(id2 - id1 + 1e-3);
    float runner = fract(dot(p, cross(dir, vec3(0.3, 1.0, 0.2))) * 0.6 - t * (0.6 + 1.6 * uOptA.y) + hp * 7.0);
    float gate = step(1.0 - 0.15 - 0.5 * uOptA.y, hash13(pair + floor(t * 0.6 + hp * 3.0)));
    float r2 = runner * runner;
    float r4 = r2 * r2;
    float flash = r4 * r4 * r2 * gate;
    // The band above the horizon; darker overhead.
    float el = elevation(d);
    float b = (el - radians(uOptA.z)) / 0.24;
    float band = exp(-b * b) * uOptA.w;
    float vis = 0.28 + 0.72 * (1.0 - smoothstep(0.35, 1.4, el)) + band;
    vec3 col = vec3(0.012, 0.004, 0.02);
    col += uColA * halo * 0.45 * vis;
    col += mix(uColA, uColB, 0.55) * crack * vis;
    col += uColB * junction * 0.7 * vis;
    col += uColB * crack * flash * 2.2 + uColA * halo * flash * 0.8;
    col = col / (1.0 + luma(col) * 0.25);
    vec3 hz = mix(vec3(0.0314, 0.0118, 0.0627), uColA, 0.22);
    return horizon(col, d, hz);
}
