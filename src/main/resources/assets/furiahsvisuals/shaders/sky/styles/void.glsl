// @colors 8C9BC4 2E2640
// Void: an almost black sky with faint dust drifting slowly through it. Nothing to distract you.
vec3 skyColor(vec3 d) {
    float t = uTime;
    vec3 col = mix(vec3(0.006, 0.006, 0.01), uColB * 0.14, exp(-abs(d.y) * 4.0) * 0.6);
    col += uColB * 0.035 * (0.5 + 0.5 * fbmN(d * 2.0 + vec3(t * 0.01, 0.0, 0.0), 3.0));
    // Dust motes in two depths, drifting and fading in and out.
    for (int i = 0; i < 2; i++) {
        float fi = float(i);
        vec3 g = d * (26.0 + fi * 18.0) / uScale + vec3(t * 0.06, t * 0.02 * (fi + 1.0), -t * 0.04) + uParallax * (0.5 + fi);
        vec3 id = floor(g);
        vec3 h = hash33(id + fi * 7.0);
        float dd = length(fract(g) - (0.25 + 0.5 * h));
        float px = fwidth(g.x) + fwidth(g.y);
        float r = max(0.05, px * 0.8);
        float blink = 0.5 + 0.5 * sin(t * (0.4 + h.y) + h.z * 6.0);
        col += uColA * exp(-dd * dd / (r * r)) * step(0.75, h.x) * blink * (0.05 / r) * 0.3;
    }
    return col;
}
