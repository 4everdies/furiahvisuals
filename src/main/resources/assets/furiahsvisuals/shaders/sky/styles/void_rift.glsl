// @colors 5B1FD1 E040FB
// Void Rift: a colossal black rift hangs in the sky, wreathed in boiling violet energy. Its ragged edge shifts slowly
// and has a thin bright rim; it hides the stars behind it and slowly breathes. Around it, violet and magenta energy
// churns (domain-warped noise, turning slowly round the rift), crackling with bright filaments, with small sparks
// flying off its edge. Long, thin arcs of light curve across the sky around it and slowly drift outward. Crisp (the
// rim, the filaments, the arcs), so it is drawn at full resolution.
// Options: uOptA.x rift size (0.5-2), uOptA.y energy (0-1), uOptA.z streaks (0-1), uOptA.w sparks (0-1);
// uOptB.x breathing (0-1), uOptB.y placement (0 high in the north, 1 the zenith, 2 a direction), uOptB.z that
// direction (degrees, 0 north, 90 east).
// React to Music: the energy flares, and the streaks speed up (the sky's clock runs faster on the beat).

// The rift's centre: high in the north, straight up, or as high in a chosen direction.
vec3 riftCenter(float placement, float dirDeg) {
    if (placement > 1.5) {
        return dirFrom(radians(dirDeg), radians(50.0));
    }
    if (placement > 0.5) {
        return vec3(0.0, 1.0, 0.0);
    }
    return dirFrom(0.0, radians(50.0));
}

// lineAA with the pixel's size given: worked out once, before any branch (derivatives inside one can't be trusted).
float riftLine(float dist, float halfWidth, float px) {
    float aa = px * 0.75 + 1e-6;
    return (1.0 - smoothstep(halfWidth - aa, halfWidth + aa, dist)) * min(1.0, halfWidth / aa);
}

vec3 skyColor(vec3 d) {
    gBeatOwn = 1.0;
    float t = uTime;
    float el = elevation(d);
    float size = uOptA.x;
    float energy = uOptA.y;
    float streaks = uOptA.z;
    float sparks = uOptA.w;
    float breathing = uOptB.x;
    vec3 violet = uColA;
    vec3 magenta = uColB;
    float flare = 1.0 + uBeat * 1.0;
    // A pixel's size on the sky (radians).
    float px = length(fwidth(d));

    // The rift's own frame: how far from its centre (theta, radians) and which way round (phi). (Opposite the rift,
    // where phi has no meaning, is always below the horizon.)
    vec3 c = riftCenter(uOptB.y, uOptB.z);
    vec3 rx;
    vec3 ry;
    frameOf(c, rx, ry);
    vec2 side = vec2(dot(d, rx), dot(d, ry));
    float theta = atan(length(side), dot(d, c));
    float phi = atan(side.y, side.x);

    // Its edge: ragged (noise on the sky itself: no seam anywhere), slowly shifting, breathing in and out.
    float r0 = 0.24 * size * (1.0 + 0.07 * breathing * sin(t * 0.45));
    float rag = fbmN(d * (6.5 / size) + vec3(0.0, t * 0.04, t * 0.03), 4.0);
    float edgeR = r0 * (1.0 + 0.45 * rag);
    // Outside the edge positive (radians).
    float sd = theta - edgeR;
    float outside = max(sd, 0.0);

    // The sky: deep violet, darker overhead, with a faint glow far round the rift.
    float up = clamp(el, 0.0, 1.5);
    vec3 col = mix(violet * 0.16 + vec3(0.01, 0.0, 0.02), vec3(0.012, 0.004, 0.025), smoothstep(0.0, 1.2, up));
    col += violet * 0.12 * exp(-outside * 2.5);

    // The energy: violet and magenta churning round the rift, hotter close to it, turning slowly about it (twisted
    // near it, like a whirl), with crackling filaments. Only near the rift.
    float near = exp(-outside / (0.22 * size));
    float glowE = 0.0;
    if (energy > 0.0 && near > 0.01) {
        vec3 p = rotAxis(d, c, t * 0.05 + 1.2 * exp(-theta * 3.0)) * (3.2 / size);
        vec2 w = fbm2N(p * 0.6 + vec3(0.0, 0.0, t * 0.05), 2.0);
        float n = fbmN(p + vec3(w * 1.3, t * 0.07), min(uDetail, 4.0));
        float body = smoothstep(-0.15, 0.55, n);
        float heat = near * near;
        vec3 ecol = mix(violet, magenta, smoothstep(0.1, 0.8, n * 0.6 + heat * 0.7));
        ecol = mix(ecol, vec3(1.0, 0.85, 1.0), smoothstep(0.75, 1.0, heat) * 0.5 * body);
        glowE = body * near * energy * flare;
        col += ecol * glowE * 1.3;
        // The fire hugging the edge: hot magenta, boiling with the same noise.
        float hug = exp(-outside / (0.035 * size));
        col += mix(magenta, vec3(1.0, 0.85, 1.0), 0.3) * hug * (0.45 + 0.55 * body) * energy * flare * 0.8;
        // Filaments: the thin creases of two more noises, kinked like sparks of lightning, crawling fast and
        // flickering, close round the edge only.
        float cn = noise(p * 2.4 + vec3(w, t * 1.5)) + 0.45 * noise(p * 5.3 + vec3(t * 2.3, w));
        float flick = 0.55 + 0.45 * noise(vec3(t * 9.0, 3.0, 1.0));
        // (The two noises' sum can pass 1: pow of a negative base is undefined, so it is kept at zero or more.)
        float fil = pow(max(1.0 - abs(cn), 0.0), 24.0) * smoothstep(0.35, 0.9, near) * energy * flick;
        col += mix(magenta, vec3(1.0), 0.6) * fil * flare * 1.5;
    }

    // The streaks: long thin arcs round the rift, one to each ring of a pattern that drifts outward, each spanning
    // part of the way round and curving outward a little along its length (within its own ring, so a pixel only looks
    // at its own).
    if (streaks > 0.0 && sd > 0.0) {
        float bandW = 0.065;
        float u = (theta - t * 0.018) / bandW;
        float band = floor(u);
        vec3 h = hash33(vec3(band, 61.0, 2.0));
        float len = 0.8 + 2.2 * h.y;
        float along = mod(phi - h.z * TAU, TAU);
        if (h.x < streaks * 0.85 && along < len) {
            float k = along / len;
            float across = abs(u - (band + 0.3 + 0.4 * k)) * bandW;
            float taper = sin(PI * k);
            float line = riftLine(across, 0.0011 * (0.4 + 0.6 * taper), px) * taper;
            float halo = exp(-across * across / 0.00006) * taper * 0.3;
            // Brightest near the rift, fading far out.
            float fade = smoothstep(0.0, 0.08, sd) * exp(-sd * 0.9);
            col += mix(magenta, vec3(1.0, 0.8, 1.0), 0.35) * (line + halo) * fade * 1.3;
        }
    }

    // The rift itself: black, a faint violet sheen just inside its edge, and a thin bright rim.
    float inside = 1.0 - smoothstep(-px, px, sd);
    col = mix(col, violet * 0.05 * exp(min(sd, 0.0) / (0.05 * size)), inside);
    float rim = riftLine(abs(sd), 0.0018, px);
    float rimGlow = exp(-abs(sd) / 0.012);
    col += mix(magenta, vec3(1.0), 0.55) * (rim * 1.3 + rimGlow * 0.3) * flare;

    // Sparks: small bright points flying off the edge, one to a slice of the rim, each on its own short cycle.
    if (sparks > 0.0 && sd > -0.02 && sd < 0.2 * size) {
        // (As many slices as keep each about as long as the rim of a size-1 rift's: a spark never reaches its slice's
        // end, where it would be cut.)
        float cells = floor(48.0 * size + 0.5);
        float cell = floor((phi + PI) / TAU * cells);
        vec3 h = hash33(vec3(cell, 23.0, 5.0));
        if (h.x < sparks) {
            float life = fract(t * (0.25 + 0.2 * h.y) + h.z);
            float sa = (cell + 0.35 + 0.3 * h.y) / cells * TAU - PI;
            vec2 dlt = vec2(theta - (edgeR + life * 0.12 * size), (phi - sa) * sin(theta));
            float r = max(0.004, px * 1.2);
            float glowS = exp(-dot(dlt, dlt) / (r * r)) * (1.0 - life) * smoothstep(0.0, 0.1, life);
            col += mix(magenta, vec3(1.0), 0.7) * glowS * 1.6 * flare;
        }
    }

    // The stars: hidden by the rift, dimmed by the brightest energy.
    gStarMask = smoothstep(0.0, 0.02, sd) * (1.0 - min(glowE * 1.5, 0.8));
    col = col / (1.0 + luma(col) * 0.1);
    vec3 hz = mix(vec3(0.0314, 0.0118, 0.0588), violet, 0.2);
    return horizon(col, d, hz);
}
