// @colors B3200E FF7A1A
// Firefall: a smoldering sky of dark smoke, lit red from below near the horizon. Streaks of fire fall at a slant like
// spears, a few at a time; glowing sword-shaped blades of fire stand along the horizon, flickering; one colossal
// burning blade towers into the smoke in a chosen direction, white-hot at its core with flames licking up its edges;
// embers drift upward. The blades, the great blade and the streaks rise out of the haze along the horizon: there is no
// ground. Crisp (the streaks and the blades' edges), so it is drawn at full resolution.
// Options: uOptA.x streak rate (0-1), uOptA.y fall angle (degrees from straight down), uOptA.z horizon blades (0-16),
// uOptA.w great blade (0/1); uOptB.x its direction (degrees, 0 north, 90 east), uOptB.y embers, uOptB.z smoke.
// React to Music: bursts of streaks, and the blades flare.

// The third color of the fire, past orange: a warm yellow (or, with the user's colors, the second one gone hot).
vec3 firefallYellow() {
    return uCustom > 0.5 ? hot(uColB) : vec3(1.0, 0.827, 0.42);
}

// The fire's colors, from a dull red (0) through orange and yellow to white-hot (1).
vec3 firefallRamp(float k, vec3 yellow) {
    vec3 c = mix(uColA, uColB, smoothstep(0.0, 0.5, k));
    c = mix(c, yellow, smoothstep(0.45, 0.8, k));
    return mix(c, vec3(1.0, 0.97, 0.9), smoothstep(0.82, 1.0, k));
}

// The shortest way from angle b to angle a (radians, -PI..PI).
float firefallTurn(float a, float b) {
    return mod(a - b + PI, TAU) - PI;
}

// lineAA with the pixel's size given: worked out once, before any branch (derivatives inside one can't be trusted).
float firefallLine(float dist, float halfWidth, float px) {
    float aa = px * 0.75 + 1e-6;
    return (1.0 - smoothstep(halfWidth - aa, halfWidth + aa, dist)) * min(1.0, halfWidth / aa);
}

// A sword of fire standing at (0, 0) in local sky coordinates (x across, y up, radians): `height` tall, `width` the
// blade's half-width; returns {how deep inside (1 at its middle .. 0 at the edge), its glow around}. The tip is a
// point, a crossguard crosses it low down, and the flames along its edges come from `flame` (-1..1). `px` is a
// pixel's size on the sky (radians).
vec2 firefallSword(vec2 p, float height, float width, float flame, float px) {
    float y = p.y;
    if (y < -0.05 || y > height + width * 8.0) {
        return vec2(0.0, 0.0);
    }
    // Straight edges, narrowing over the top fifth to a point; the flames barely move the edge (they lick outside it).
    float taper = clamp((height - y) / (height * 0.2), 0.0, 1.0);
    float hw = width * taper * (1.0 + 0.08 * flame);
    // The crossguard: a bar a sixth of the way up, with a short grip below it into the haze.
    float guardY = height * 0.15;
    float guard = (1.0 - smoothstep(width * 0.45, width * 0.75, abs(y - guardY))) * width * 2.8;
    float grip = (1.0 - step(guardY, y)) * width * 0.45;
    hw = max(max(hw, guard), grip);
    float edge = abs(p.x) - hw;
    float aa = px * 1.2 + 1e-5;
    // (Thinner than a pixel, it fades rather than leaving a hairline: above the tip, nothing.)
    float inside = (1.0 - smoothstep(-aa, aa, edge)) * clamp(hw / aa, 0.0, 1.0);
    float depth = inside * clamp(-edge / max(hw, 1e-4), 0.0, 1.0);
    float glowAround = exp(-max(edge, 0.0) / (width * 1.6)) * (1.0 - smoothstep(height, height + width * 8.0, y));
    return vec2(max(depth, inside * 0.12), glowAround);
}

vec3 skyColor(vec3 d) {
    gBeatOwn = 1.0;
    float t = uTime;
    float el = elevation(d);
    float az = azimuth(d);
    vec3 yellow = firefallYellow();
    float rate = uOptA.x;
    float fall = radians(uOptA.y);
    float blades = floor(uOptA.z + 0.5);
    float greatOn = uOptA.w;
    float greatAz = radians(uOptB.x);
    float embers = uOptB.y;
    float smokeAmt = uOptB.z;
    vec3 ember = mix(uColA, uColB, 0.45);
    // A pixel's size on the sky (radians).
    float px = length(fwidth(d));

    // Behind the smoke: a dull glow, red low down, nearly black overhead.
    float up = clamp(el, 0.0, 1.5);
    vec3 col = mix(uColA * 0.22 + vec3(0.02, 0.004, 0.0), vec3(0.016, 0.006, 0.005), smoothstep(0.0, 0.75, up));

    // The smoke: a deck of heavy clouds overhead, churning slowly (folded by a slow warp), dark where it is thick, lit
    // red-orange from below by the fires, most of all low over the horizon.
    float n = 0.12;
    if (smokeAmt > 0.0) {
        vec2 uv = deck(d, 1.0) * (0.42 / uScale);
        vec2 w = fbm2N(vec3(uv * 0.55, t * 0.018), 2.0);
        n = fbmN(vec3(uv + w * 0.9 + vec2(t * 0.012, -t * 0.006), t * 0.025), min(uDetail, 4.0));
        // (Far away the deck's detail is finer than a pixel: it evens out there instead of shimmering.)
        n = mix(0.12, n, smoothstep(0.015, 0.16, d.y));
    }
    float dens = smoothstep(-0.32, 0.42, n) * smokeAmt;
    // Thin places and the folds' edges catch the most light from below.
    float rim = clamp(1.0 - abs(n - 0.02) * 3.2, 0.0, 1.0);
    float thin = 1.0 - smoothstep(-0.1, 0.6, n);
    float below = exp(-max(el, 0.0) * 2.4);
    vec3 smoke = mix(vec3(0.075, 0.027, 0.018), vec3(0.014, 0.006, 0.005), smoothstep(-0.1, 0.65, n));
    vec3 underLit = ember * (below * (0.22 + 0.55 * rim + 0.3 * thin) + 0.05 * rim);
    col = mix(col, smoke, dens) + underLit * dens;

    // The haze the fires make along the horizon.
    col += ember * (0.3 * exp(-abs(el) * 11.0) + 0.05 * exp(-max(el, 0.0) * 3.0));

    float flare = 1.0 + uBeat * 1.1;

    // The great blade: far away in its direction, towering into the smoke.
    if (greatOn > 0.5 && el > -0.06 && el < 1.3) {
        float x = firefallTurn(az, greatAz) * cos(el);
        if (abs(x) < 0.28) {
            float flame = fbmN(vec3(x * 22.0, el * 6.0 - t * 1.4, 3.0), 2.0);
            vec2 s = firefallSword(vec2(x, el + 0.025), 0.98, 0.058, flame, px);
            // (Its glow is gone before this stops being looked at: no seam.)
            s.y *= 1.0 - smoothstep(0.15, 0.27, abs(x));
            // Its upper part is lost in the smoke.
            float veil = 1.0 - smoothstep(0.5, 1.0, el) * dens * 0.8;
            // A burning gold blade with a thin white-hot line down its middle.
            float inside = step(0.001, s.x);
            float core = smoothstep(0.8, 0.97, s.x);
            vec3 c = firefallRamp(0.42 + 0.38 * s.x, yellow) * inside * (0.75 + 0.6 * s.x) + vec3(1.0, 0.97, 0.9) * core * 1.3;
            // Flames licking up its edges, and its light on the air and the smoke around it.
            float lick = smoothstep(0.05, 0.6, flame) * s.y * (1.0 - inside);
            c += firefallRamp(0.4, yellow) * lick * 0.9 + ember * s.y * 0.35;
            col += c * veil * flare;
        }
    }

    // The horizon blades: swords of fire standing along the horizon, each flickering on its own. Each stands somewhere
    // in its own slice of the horizon, so only this direction's slice and its two neighbours can reach here.
    if (blades > 0.0 && el < 0.62 && el > -0.08) {
        float slice = floor(fract(az / TAU) * blades);
        for (int j = 0; j < 3; j++) {
            float fi = mod(slice + float(j) - 1.0, blades);
            vec3 h = hash33(vec3(fi, 13.0, 7.0));
            float bAz = (fi + 0.2 + 0.6 * h.x) / blades * TAU;
            float x = firefallTurn(az, bAz) * cos(el);
            if (abs(x) > 0.12) {
                continue;
            }
            float height = 0.2 + 0.2 * h.y;
            float width = 0.013 + 0.008 * h.z;
            float flame = noise(vec3(x * 55.0, el * 12.0 - t * 2.2, fi * 5.0));
            vec2 s = firefallSword(vec2(x, el + 0.03), height, width, flame, px);
            s.y *= 1.0 - smoothstep(0.06, 0.115, abs(x));
            float flicker = 0.72 + 0.28 * noise(vec3(fi * 3.0, t * 2.6, 1.0));
            float inside = step(0.001, s.x);
            vec3 c = firefallRamp(0.4 + 0.45 * s.x, yellow) * inside * (0.9 + 0.8 * s.x) + ember * s.y * 0.6;
            col += c * flicker * flare;
        }
    }

    // The streaks: spears of fire falling at a slant, a few at a time (bursts of more on the beat). Each falls along a
    // line of its own in a sheared frame (azimuth + elevation x the slant, which stays seamless all round the sky), one
    // line to each slice of that frame, so a pixel only looks at its own slice and the nearer neighbour.
    if (el > -0.08 && el < 1.0 && rate + uBeat > 0.0) {
        float slant = tan(fall);
        float cosFall = cos(fall);
        float slices = 26.0;
        float sw = TAU / slices;
        float sPos = (az + el * slant) / sw;
        float s0 = floor(sPos) - (fract(sPos) < 0.5 ? 1.0 : 0.0);
        for (int j = 0; j < 2; j++) {
            float c = s0 + float(j);
            float id = mod(c, slices);
            vec3 h = hash33(vec3(id, 41.0, 3.0));
            float period = 3.0 + 2.5 * h.x;
            float u = t / period + h.y;
            float tau = fract(u) * period;
            // (No streak lives longer than 2.4 s.)
            if (tau > 2.4) {
                continue;
            }
            vec3 g = hash33(vec3(id, floor(u), 17.0));
            // Whether this cycle has a streak at all: the rate, and more on the beat.
            float life = 1.6 + 0.8 * g.y;
            if (g.x > rate + uBeat * 0.6 || tau > life) {
                continue;
            }
            float speed = 0.42 + 0.3 * g.z;
            float headEl = 0.3 + 0.6 * fract(g.y * 7.31) - cosFall * speed * tau;
            // Across its line (radians on the sky), and how far above its head along it.
            float across = abs(sPos - (c + 0.25 + 0.5 * g.z)) * sw * cos(el) * cosFall;
            float behind = (el - headEl) / cosFall;
            float tail = 0.13 + 0.09 * g.x;
            float along = clamp(behind / tail, 0.0, 1.0);
            float on = step(0.0, behind) * (1.0 - along);
            float body = firefallLine(across, 0.0022 * (1.0 - along * 0.75), px) * on * (1.0 - along);
            float glowS = exp(-across * across / 0.00004) * on * 0.35;
            float head = exp(-(across * across + behind * behind) / 0.000025);
            float fade = smoothstep(0.0, 0.12, tau) * (1.0 - smoothstep(life - 0.25, life, tau)) * step(-0.06, headEl);
            col += (firefallRamp(0.78 - along * 0.5, yellow) * (body * 1.7 + glowS) + vec3(1.0, 0.95, 0.85) * head * 1.8) * fade;
        }
    }

    // Embers drifting up: a few cells of a grid that rises with time hold one, a short glowing streak swaying a little.
    if (embers > 0.0 && el > -0.04 && el < 0.95) {
        float cols = 72.0;
        vec2 cellUv = vec2(az / TAU * cols, el * 11.5 - t * 0.5);
        vec2 id = floor(cellUv);
        vec3 h = hash33(vec3(id, 29.0));
        if (h.x < embers * 0.22) {
            vec2 pos = id + vec2(0.3 + 0.4 * h.y + 0.1 * sin(t * (0.8 + h.z) + h.x * 30.0), 0.3 + 0.4 * h.z);
            vec2 dlt = cellUv - pos;
            // (Across, a cell is narrower on the sky by cos(el): in row units. Along, the streak is three times longer.)
            dlt.x *= cos(el) * TAU * 11.5 / cols;
            dlt.y *= 0.34;
            // (A row of cells is 1/11.5 of a radian high.)
            float r = max(0.032, px * 11.5 * 0.8);
            float glowE = exp(-dot(dlt, dlt) / (r * r));
            float twinkle = 0.55 + 0.45 * sin(t * (3.0 + 4.0 * h.y) + h.z * 20.0);
            float high = 1.0 - smoothstep(0.55, 0.95, el);
            col += firefallRamp(0.55 + 0.4 * h.y, yellow) * glowE * twinkle * high * 1.4;
        }
    }

    gStarMask = (1.0 - dens) * 0.12;
    col = col / (1.0 + luma(col) * 0.12);
    vec3 hz = mix(vec3(0.0627, 0.0118, 0.0078), uColA, 0.25);
    return horizon(col, d, hz);
}
