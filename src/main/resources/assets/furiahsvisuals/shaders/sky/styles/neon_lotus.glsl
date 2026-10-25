// @colors C24BFF 3D5BFF
// Neon Lotus: a huge bloom of neon light trails rising from the northern horizon over rippling rings, with streaks
// of light fanning out to both sides.

// Distance to the outline of one petal (a pointed lens shape) standing on `base`, leaning by `tilt` radians,
// and how far up the petal that closest point is (0 = base, 1 = tip).
vec2 petal(vec2 p, vec2 base, float tilt, float h, float w) {
    vec2 axis = vec2(sin(tilt), cos(tilt));
    vec2 q = p - base;
    float y = dot(q, axis);
    float x = dot(q, vec2(axis.y, -axis.x));
    float yc = clamp(y, 0.0, h);
    float halfWidth = w * pow(sin(PI * yc / h), 0.8);
    return vec2(length(vec2(abs(x) - halfWidth, y - yc)), yc / h);
}

vec3 neon(float dist, float width, vec3 color) {
    float core = exp(-dist * dist / (width * width));
    float halo = width * 4.0 / (width * 4.0 + dist * dist * 180.0);
    return hot(color) * core + color * halo * 0.45;
}

vec3 skyColor(vec3 d) {
    float t = uTime;
    vec3 center = normalize(vec3(0.0, 0.02, -1.0));
    vec2 uv = tangentCoords(d, center) * 0.55 / uScale;
    float front = smoothstep(0.0, 0.3, dot(d, center));
    vec2 m = vec2(abs(uv.x), uv.y);    // mirror symmetry
    float base = -0.01;
    vec3 col = vec3(0.012, 0.008, 0.04);
    // Violet haze around the bloom, brightest at its heart.
    col += mix(uColB, uColA, 0.6) * (0.18 * exp(-dot(uv * vec2(1.0, 1.8), uv * vec2(1.0, 1.8)) * 3.0)
            + 0.35 * exp(-dot(uv * vec2(1.0, 3.0), uv * vec2(1.0, 3.0)) * 60.0)) * front;
    // Faint streaks all around the horizon, so the rest of the sky is not empty.
    float hz = abs(d.y + 0.005);
    float far = exp(-hz * hz * 1400.0) * (0.5 + 0.5 * sin(azimuth(d) * 31.0 + t * 0.7)) * (1.0 - 0.7 * front);
    col += mix(uColB, uColA, 0.5) * far * 0.2;
    // Everything else is on the side facing the bloom: skip it behind.
    if (front <= 0.0) {
        return col;
    }
    vec3 light = vec3(0.0);
    // Petals: nested, leaning further out as they grow, breathing slowly. Light keeps flowing up each petal,
    // so the outlines pulse from the heart to the tips.
    for (int k = 0; k < 7; k++) {
        float fk = float(k);
        float rnd = hash12(vec2(fk, 9.0));
        float breathe = 0.05 * sin(t * 0.6 + fk * 1.3);
        float h = 0.22 + fk * 0.12 + breathe + rnd * 0.05;
        float w = 0.04 + fk * 0.04 + rnd * 0.02;
        float tilt = fk * 0.15 + 0.06 * sin(t * 0.45 + fk);
        vec2 pd = petal(m, vec2(0.0, base), tilt, h, w);
        float flow = 0.3 + 0.7 * pow(0.5 + 0.5 * sin(pd.y * 7.0 - t * 2.2 + fk * 1.7), 2.0);
        vec3 c = mix(uColA, uColB, fk / 6.0);
        light += neon(pd.x, 0.0035 + fk * 0.0005, c) * (1.0 - fk * 0.07) * flow;
    }
    light += neon(petal(m, vec2(0.0, base), 0.0, 0.16 + 0.03 * sin(t * 0.8), 0.03).x, 0.004, hot(uColA));
    // Streaks of light fanning out sideways just above the horizon, running outwards.
    for (int j = 0; j < 14; j++) {
        float fj = float(j);
        float h1 = hash12(vec2(fj, 1.0));
        float h2 = hash12(vec2(fj, 2.0));
        float y0 = base + 0.004 + h1 * 0.12;
        float slope = (h2 - 0.35) * 0.2;
        float dist = abs(m.y - (y0 + slope * m.x));
        float dash = smoothstep(0.1, 0.9, sin(m.x * (3.0 + h2 * 3.0) - t * (1.2 + h1) + fj * 2.1));
        float reach = smoothstep(0.02, 0.1, m.x) * exp(-m.x * (0.35 + 0.6 * h1));
        vec3 c = mix(uColB, uColA, h2);
        light += neon(dist, 0.0022 + h1 * 0.0025, c) * dash * reach * 1.3;
    }
    // Ripples spreading on the "water" below the bloom.
    vec2 rp = vec2(uv.x, (uv.y - base + 0.012) * 4.0);
    float r = length(rp);
    for (int i = 0; i < 5; i++) {
        float phase = fract(t * 0.2 + float(i) * 0.2);
        float ring = abs(r - phase * 0.6);
        light += neon(ring, 0.0035, mix(uColA, uColB, phase)) * (1.0 - phase) * step(uv.y, base + 0.015);
    }
    col += light * front;
    return col / (1.0 + luma(col) * 0.3);
}
