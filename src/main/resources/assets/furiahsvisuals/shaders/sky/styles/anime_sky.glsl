// @colors 3D8BFF A5B8EA
// Anime Sky: a vivid blue sky with big, cel-shaded cumulus clouds piled along the horizon and drifting slowly past
// a bright sun.

// One bank of puffy clouds around the horizon: the union of round puffs sitting on a flat base. Returns
// (coverage, lit) for the pixel, where lit is 1 on the side facing the sun and 0 in shadow.
vec2 cloudBank(float az, float el, float base, float size, float cells, float drift, vec2 sun2) {
    float x = az * cells / TAU + drift;
    float cell = floor(x);
    float cover = 0.0;
    float lit = 0.0;
    float best = -1.0;
    for (int i = -2; i <= 2; i++) {
        float c = cell + float(i);
        // Hashed by the cell's place around the horizon, so the bank joins up seamlessly all the way round.
        float wrapped = mod(c, cells);
        vec3 h = hash33(vec3(wrapped, base * 17.0, cells));
        if (h.x < 0.25) {
            continue;
        }
        // A cloud is a row of puffs along a flat base with more puffs piled on top into a tower, like cauliflower.
        float scale = 0.7 + 0.6 * h.y;
        float mid = (c + 0.5 + 0.3 * (h.z - 0.5)) * TAU / cells - drift * TAU / cells;
        for (int j = 0; j < 8; j++) {
            float fj = float(j);
            vec3 k = hash33(vec3(wrapped, fj, base * 31.0));
            float upper = step(4.5, fj);
            float r = size * scale * mix(0.5 + 0.3 * k.z, 0.42 + 0.3 * k.z, upper);
            vec2 center = vec2(mid + (k.x - 0.5) * size * scale * mix(2.8, 1.5, upper),
                               base + mix(r * 0.55, size * scale * (0.75 + 0.6 * k.y), upper));
            vec2 p = vec2(az, el) - center;
            // Wrap around the horizon so puffs near north join up.
            p.x = mod(p.x + PI, TAU) - PI;
            float inside = r - length(p);
            if (inside > best) {
                best = inside;
                vec2 n = p / r;
                float nz = sqrt(max(0.0, 1.0 - dot(n, n)));
                lit = dot(normalize(vec3(n, nz)), normalize(vec3(sun2, 0.6)));
            }
        }
    }
    float aa = max(fwidth(el) * 1.5, 0.0015);
    cover = smoothstep(-aa, aa, best) * smoothstep(base - aa, base + aa * 3.0, el);
    return vec2(cover, lit);
}

vec3 skyColor(vec3 d) {
    float t = uTime * 0.01;
    vec3 sunDir = dirFrom(0.7, 0.72);
    float mu = dot(d, sunDir);
    vec3 col = gradient(d, vec3(0.62, 0.84, 1.0), uColA * 0.85, 0.5);
    col = mix(col, vec3(0.92, 0.97, 1.0), exp(-max(d.y, 0.0) * 16.0) * 0.55);
    col += vec3(1.0, 0.98, 0.92) * pow(max(mu, 0.0), 30.0) * 0.45;
    gStarMask = 0.0;
    if (uSunOwn > 0.5) {
        col = mix(col, vec3(1.0, 1.0, 0.96) * 1.4, disk(d, sunDir, 0.045));
        // Lens flare: a few soft rings along the line through the sun, like an anime background.
        vec2 sp = tangentCoords(d, sunDir);
        for (int i = 1; i < 4; i++) {
            float fi = float(i);
            float e = (length(sp - vec2(-0.12, -0.2) * fi) - 0.03 * fi) * 60.0;
            col += vec3(0.7, 0.85, 1.0) * 0.08 * exp(-e * e) * step(0.0, mu);
        }
    }
    if (uClouds > 0.5) {
        float az = azimuth(d);
        float el = elevation(d);
        vec2 sun2 = normalize(vec2(sin(0.7 - az), sunDir.y - d.y * 0.5));
        // Two banks: big towers on the horizon, smaller clouds higher up drifting the other way. A bank is only
        // worked out between its base and the highest its puffs can reach, which skips most of the sky.
        for (int b = 0; b < 2; b++) {
            float fb = float(b);
            float base = fb * 0.32;
            float size = (0.2 - fb * 0.1) * uScale;
            if (el < base - 0.01 || el > base + size * 2.9) {
                continue;
            }
            vec2 c = cloudBank(az, el, base, size, 8.0 + fb * 6.0, t * (1.0 - fb * 1.8), sun2);
            if (c.x > 0.0) {
                // Cel shading: two flat tones split by a crisp line, a pale rim where it faces the sun.
                float lit = smoothstep(0.08, 0.14, c.y);
                vec3 cloudCol = mix(uColB, vec3(1.0), 0.12 + 0.88 * lit);
                cloudCol = mix(cloudCol, vec3(1.0, 0.99, 0.95), smoothstep(0.75, 0.85, c.y) * 0.6);
                col = mix(col, cloudCol, c.x);
            }
        }
    }
    if (d.y < 0.0) {
        col = mix(vec3(0.75, 0.88, 1.0), vec3(0.45, 0.55, 0.7), smoothstep(0.0, -0.4, d.y));
    }
    return col;
}
