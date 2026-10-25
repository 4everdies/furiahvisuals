// @colors 7FC8FF F2FAFF
// Frozen Tide: a vast, misty wave of frost curls across the sky, as if the sea had frozen mid-crash overhead: a broad
// arc of swirling mist, brightest along its curling crest, slowly rolling along its length. Tall, thin needles of
// glowing ice rise from a band of white mist along the horizon, each with a bright core and sparkles drifting off its
// tip, under faint stars. Crisp (the spires and the crest), so it is drawn at full resolution.
// Options: uOptA.x wave size (0.5-2), uOptA.y wave speed (0-2), uOptA.z spires (0-24), uOptA.w spire height
// (0.5-1.5); uOptB.x sparkles (0-1), uOptB.y starfield (0/1).
// React to Music: the spires pulse brighter, and the wave's crest flares.

// lineAA with the pixel's size given: worked out once, before any branch (derivatives inside one can't be trusted).
float tideLine(float dist, float halfWidth, float px) {
    float aa = px * 0.75 + 1e-6;
    return (1.0 - smoothstep(halfWidth - aa, halfWidth + aa, dist)) * min(1.0, halfWidth / aa);
}

// The shortest way from angle b to angle a (radians, -PI..PI).
float tideTurn(float a, float b) {
    return mod(a - b + PI, TAU) - PI;
}

vec3 skyColor(vec3 d) {
    gBeatOwn = 1.0;
    float t = uTime;
    float el = elevation(d);
    float az = azimuth(d);
    float waveSize = uOptA.x;
    float waveSpeed = uOptA.y;
    float spires = floor(uOptA.z + 0.5);
    float spireHeight = uOptA.w;
    float sparkles = uOptB.x;
    float starfield = uOptB.y;
    vec3 ice = uColA;
    vec3 white = uColB;
    float pulse = 1.0 + uBeat * 1.0;
    // A pixel's size on the sky (radians).
    float px = length(fwidth(d));

    // The sky: a dark blue-grey night, a little lighter low down.
    float up = clamp(el, 0.0, 1.5);
    vec3 col = mix(ice * 0.1 + vec3(0.02, 0.025, 0.035), vec3(0.012, 0.016, 0.028), smoothstep(0.0, 1.0, up));

    // The wave, as from the shore of a breaking sea: its crest follows a great circle crossing the sky from the east
    // horizon, over high in the north (50°), to the west; its misty face hangs below the crest, down toward the
    // northern horizon, and over the crest its lip curls on toward the zenith in wisps of spray. (Its frame, and the
    // noise on the sky itself, have no seam.)
    vec3 n = vec3(0.0, 0.643, 0.766);
    vec3 e = vec3(1.0, 0.0, 0.0);
    vec3 b = cross(n, e);
    // How far off the crest's circle (radians, toward the zenith positive), and the mist rolling along the arc.
    float s = asin(clamp(dot(d, n), -1.0, 1.0));
    vec3 dr = rotAxis(d, n, t * 0.04 * waveSpeed);
    // Coordinates stretched along the arc: the mist runs in long wisps, like spray along a breaking wave.
    vec3 p = vec3(dot(dr, e), dot(dr, b), s * 4.0) * 3.0;
    float width = 0.45 * waveSize;
    // The crest: wavy along its length, rolling with the mist; its foam comes and goes along it.
    vec2 along = noise2(p * vec3(1.6, 1.6, 0.0) + vec3(0.0, 0.0, t * 0.1));
    float fromCrest = s - 0.03 * along.x;
    float foam = smoothstep(-0.4, 0.6, along.y);
    float waveBody = 0.0;
    if (fromCrest > -width && fromCrest < 0.14) {
        vec2 w = fbm2N(p * 0.5 + vec3(0.0, 0.0, t * 0.03), 2.0);
        float m = fbmN(p + vec3(w * 0.9, t * 0.05), min(uDetail, 4.0));
        // The face: densest and brightest up at the crest, thinning down toward the horizon, cut clean at the top;
        // just under the crest, the hollow beneath the curling lip lies in shadow.
        float nearCrest = exp(min(fromCrest, 0.0) / (0.09 * waveSize));
        float face = smoothstep(-width, -width * 0.35, fromCrest) * (1.0 - smoothstep(-0.006, 0.004, fromCrest));
        float mist = smoothstep(-0.35, 0.45, m) * face * (0.4 + 0.6 * nearCrest);
        float hollow = smoothstep(-0.1 * waveSize, -0.04 * waveSize, fromCrest) * (1.0 - smoothstep(-0.022 * waveSize, -0.008, fromCrest));
        // The lip: broken spray curling on past the crest, thinning out.
        float lip = (1.0 - smoothstep(0.0, 0.14, fromCrest)) * step(0.0, fromCrest) * smoothstep(0.05, 0.6, m) * foam;
        waveBody = clamp(mist + lip * 0.7, 0.0, 1.0);
        vec3 mistCol = mix(ice * 0.7, mix(ice, white, 0.65), nearCrest) * (0.55 + 0.45 * nearCrest) * (1.0 - 0.55 * hollow);
        col = mix(col, mistCol, waveBody * 0.9);
        // The air the ice lights, round the crest.
        col += ice * 0.12 * face * nearCrest;
        // The crest's ridge of foam: thicker where the foam piles up, textured by the mist.
        float ridgeW = 0.005 + 0.012 * foam;
        float ridge = exp(min(fromCrest, 0.0) / ridgeW) * (1.0 - smoothstep(-0.002, 0.003, fromCrest));
        col += mix(ice, white, 0.8) * ridge * (0.55 + 0.45 * smoothstep(-0.2, 0.5, m)) * 0.85 * (1.0 + uBeat * 1.2);
    }
    // The crest's edge: a fine line of light along the top, brightest where the foam is (its pixel size from the top).
    float crestLine = tideLine(abs(fromCrest), 0.0012, px) * (0.4 + 0.6 * foam);
    float crestGlow = exp(-abs(fromCrest) / 0.01) * (0.15 + 0.3 * foam);
    col += mix(ice, white, 0.7) * (crestLine * 0.6 + crestGlow * 0.4) * smoothstep(-0.1, 0.05, el) * (1.0 + uBeat * 1.2);

    // The mist along the horizon: a soft white band.
    col += mix(ice, white, 0.6) * (0.45 * exp(-abs(el) * 9.0) + 0.06 * exp(-max(el, 0.0) * 3.0));

    // The spires: thin needles of ice round the horizon, each standing somewhere in its own slice, so only this
    // direction's slice and its two neighbours can reach here.
    float spireLight = 0.0;
    if (spires > 0.0 && el > -0.06 && el < 0.7 * spireHeight + 0.05) {
        float slice = floor(fract(az / TAU) * spires);
        for (int j = 0; j < 3; j++) {
            float fi = mod(slice + float(j) - 1.0, spires);
            vec3 h = hash33(vec3(fi, 31.0, 11.0));
            float sAz = (fi + 0.2 + 0.6 * h.x) / spires * TAU;
            float x = tideTurn(az, sAz) * cos(el);
            if (abs(x) > 0.08) {
                continue;
            }
            float height = (0.18 + 0.3 * h.y) * spireHeight;
            float y = el + 0.03;
            // A needle: widest at its foot in the mist, narrowing to a sharp tip.
            float k = clamp(y / height, 0.0, 1.0);
            float hw = (0.0055 + 0.003 * h.z) * (1.0 - k) * step(0.0, y) * step(y, height);
            float edge = abs(x) - hw;
            float inside = (1.0 - smoothstep(-px, px, edge)) * clamp(hw / px, 0.0, 1.0);
            float core = inside * (1.0 - smoothstep(0.0, hw * 0.8 + px, abs(x)));
            float glowN = exp(-max(edge, 0.0) / 0.006) * (1.0 - smoothstep(height * 0.9, height * 1.15, y)) * step(-0.03, y);
            // (Its glow is gone before this stops being looked at: no seam.)
            glowN *= 1.0 - smoothstep(0.04, 0.075, abs(x));
            float shimmer = 0.8 + 0.2 * noise(vec3(fi * 3.0, t * 1.7, 2.0));
            col += (ice * (inside * 0.8 + glowN * 0.35) + white * core * 0.9) * shimmer * pulse;
            spireLight = max(spireLight, inside);
            // Sparkles drifting off the tip: three, each rising and fading on its own cycle.
            if (sparkles > 0.0) {
                for (int i = 0; i < 3; i++) {
                    vec3 g = hash33(vec3(fi, float(i), 47.0));
                    float life = fract(t * (0.2 + 0.15 * g.x) + g.y);
                    if (g.z > sparkles * 1.2) {
                        continue;
                    }
                    vec2 sp = vec2(sin(t * (0.7 + g.x) + g.y * 9.0) * 0.012 * life + (g.z - 0.5) * 0.01, height + life * 0.09);
                    vec2 dl = vec2(x, y) - sp;
                    float r = max(0.0025, px * 1.1);
                    float tw = exp(-dot(dl, dl) / (r * r)) * (1.0 - life) * smoothstep(0.0, 0.15, life);
                    col += white * tw * 1.4 * pulse;
                }
            }
        }
    }

    // Faint stars above, where neither the wave nor the mist hides them (Starfield).
    gStarMask = starfield * (1.0 - waveBody) * smoothstep(0.02, 0.2, el) * 0.6;
    col = col / (1.0 + luma(col) * 0.1);
    vec3 hz = mix(vec3(0.102, 0.129, 0.161), ice, 0.3);
    return horizon(col, d, hz);
}
