// @colors B0101A FF4A2E
// Blood Moon: a crimson haze under a giant red moon; ash drifts past and the whole sky pulses slowly.
vec3 skyColor(vec3 d) {
    float t = uTime;
    float pulse = 0.85 + 0.15 * sin(t * 0.55);
    vec3 col = gradient(d, uColA * 0.5, vec3(0.012, 0.0, 0.004), 0.7) * pulse;
    col += uColA * 0.16 * (0.5 + 0.5 * fbmN(d * 3.0 + vec3(t * 0.02, 0.0, 0.0), 4.0)) * exp(-max(d.y, 0.0) * 2.5);
    col += uColA * 0.25 * exp(-abs(d.y) * 14.0) * pulse;
    // The moon: huge, cratered, darker at its edge, with a wide glow.
    vec3 moonDir = dirFrom(0.35, 0.4);
    float R = 0.2 * uScale;
    if (uSunOwn > 0.5) {
        float a = angleTo(d, moonDir);
        vec2 m = tangentCoords(d, moonDir) / R;
        float body = disk(d, moonDir, R);
        float craters = fbmN(vec3(m * 2.5, 4.0), 5.0);
        float limb = sqrt(max(0.0, 1.0 - dot(m, m)));
        vec3 surface = uColB * (0.5 + 0.7 * limb) * (0.8 + 0.5 * craters) * 1.2;
        surface = mix(surface, uColB * 0.45, smoothstep(0.1, 0.35, fbmN(vec3(m * 1.2, 9.0), 3.0)) * 0.5);
        col = mix(col, surface * pulse, body);
        col += uColB * 0.4 * exp(-max(a - R, 0.0) * 12.0) * (1.0 - body) * pulse;
        gStarMask = smoothstep(R * 1.2, R * 3.0, a);
        // Dark wisps of cloud drifting across the moon.
        float wisp = smoothstep(0.1, 0.5, fbmN(vec3(m.x * 0.7 + t * 0.03, m.y * 2.2, 2.0), 4.0));
        col *= 1.0 - 0.45 * wisp * exp(-max(a - R, 0.0) * 6.0);
    }
    // Ash: grey flakes drifting slowly sideways and down.
    vec3 g = d * 34.0 / uScale + vec3(t * 0.25, -t * 0.15, t * 0.05);
    vec3 id = floor(g);
    vec3 f = fract(g);
    vec3 h = hash33(id);
    float dd = length(f - (0.25 + 0.5 * h));
    float px = fwidth(g.x) + fwidth(g.y);
    float r = max(0.035, px * 0.8);
    col += vec3(0.55, 0.45, 0.45) * exp(-dd * dd / (r * r)) * step(0.8, h.x) * (0.035 / r) * 0.5;
    return col;
}
