// @colors FFB3D1 C3B6FF
// Cherry Blossom: a soft pink evening sky with pale clouds and petals drifting far away on the wind.
vec3 skyColor(vec3 d) {
    float t = uTime;
    vec3 col = gradient(d, vec3(1.0, 0.84, 0.78), mix(uColB, vec3(0.42, 0.38, 0.72), 0.45), 0.6);
    col = mix(col, uColA, exp(-abs(d.y - 0.15) * 6.0) * 0.35);
    gStarMask = 0.25;
    if (uClouds > 0.5 && d.y > 0.0) {
        vec2 pos = deck(d, 1.0) * 0.35 / uScale + vec2(t * 0.012, 0.0) + uParallax.xz * 0.05;
        float dens = fbm(vec3(pos, 3.0));
        float cover = smoothstep(0.05, 0.5, dens) * smoothstep(0.0, 0.12, d.y);
        vec3 cloudCol = mix(vec3(1.0, 0.9, 0.93), uColA, 0.25 + 0.3 * smoothstep(0.2, 0.6, dens));
        col = mix(col, cloudCol, cover * 0.75);
    }
    // Petals: three layers of sparse petals, drifting sideways and slowly falling, each turning as it goes.
    vec3 right;
    vec3 up;
    frameOf(d, right, up);
    for (int layer = 0; layer < 3; layer++) {
        float fl = float(layer);
        float cells = (4.5 + fl * 3.0) / uScale;
        vec3 g = d * cells + vec3(t * (0.22 + fl * 0.08), -t * (0.12 + fl * 0.04), t * 0.05) + uParallax * (0.3 + fl * 0.2);
        vec3 id = floor(g);
        vec3 h = hash33(id + fl * 13.0);
        vec3 rel = fract(g) - (0.3 + 0.4 * h);
        vec2 q = vec2(dot(rel, right), dot(rel, up));
        float ang = t * (0.5 + h.y) + h.z * TAU;
        q = vec2(cos(ang) * q.x - sin(ang) * q.y, sin(ang) * q.x + cos(ang) * q.y);
        // A petal: an oval with a notch at its tip, narrowing as it turns edge-on, paler along its middle.
        float turn = 0.35 + 0.65 * abs(sin(t * (0.7 + h.x) + h.y * 6.0));
        vec2 e = q / vec2(0.07 * turn, 0.11);
        float r2 = dot(e, e);
        // (fwidth jumps where the grid cell changes; capped so cell borders never show as lines)
        float aa = min(fwidth(r2), 0.3) + 0.02;
        float shape = 1.0 - smoothstep(1.0 - aa, 1.0 + aa, r2);
        shape *= smoothstep(0.012, 0.03, length(q - vec2(0.0, 0.105)));
        float show = step(h.x, 0.2);
        vec3 petalCol = mix(uColA, vec3(1.0, 0.95, 0.97), clamp(0.3 + 0.35 * turn * (1.0 - r2), 0.0, 1.0));
        col = mix(col, petalCol, shape * show * (0.95 - fl * 0.2));
    }
    return col;
}
