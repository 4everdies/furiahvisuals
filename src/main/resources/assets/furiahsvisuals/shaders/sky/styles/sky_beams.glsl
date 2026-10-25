// @colors A64DFF FF6AD5
// Sky Beams: a dark sky with searchlight beams sweeping up from the horizon like at a concert: soft-edged, widening and
// fading as they rise, slowly swaying and crossing, with a glowing haze along the horizon. Soft, so it is drawn at
// half resolution and smoothly scaled up.
// Options: uOptA.x beams (2-12), uOptA.y sway speed, uOptA.z width, uOptA.w height; uOptB.x haze.

vec3 skyColor(vec3 d) {
    gStarMask = 0.6;
    float t = uTime;
    float el = elevation(d);
    vec3 col = mix(vec3(0.028, 0.012, 0.05), vec3(0.004, 0.002, 0.01), smoothstep(0.0, 1.2, el));
    float n = floor(uOptA.x + 0.5);
    float reach = mix(0.4, 1.45, uOptA.w);
    vec3 light = vec3(0.0);
    for (int i = 0; i < 12; i++) {
        float fi = float(i);
        if (fi >= n) {
            break;
        }
        vec3 h = hash33(vec3(fi, 3.0, 9.0));
        // Where it stands on the horizon, and where it points: leaning, swaying slowly.
        float baseAz = (fi + 0.2 + 0.6 * h.x) / n * TAU;
        float sway = 0.6 * sin(t * 0.3 * uOptA.y * (0.6 + 0.8 * h.y) + h.z * TAU);
        vec3 base = dirFrom(baseAz, -0.03);
        vec3 aim = normalize(dirFrom(baseAz + sway, 0.0) * (0.25 + 0.3 * h.y) + vec3(0.0, 1.0, 0.0));
        // The beam runs along the great circle from its base towards where it aims.
        vec3 nrm = normalize(cross(base, aim));
        vec3 ahead = normalize(cross(nrm, base));
        float across = dot(d, nrm);
        float ang = atan(dot(d, ahead), dot(d, base));
        if (ang <= 0.0 || ang > reach + 0.4) {
            continue;
        }
        // Wider and fainter as it rises; soft edges, a hot core, and the light it scatters in the air around it.
        float w = (0.018 + 0.08 * ang) * uOptA.z;
        float a2 = across * across / (w * w);
        float body = exp(-a2);
        float b2 = body * body;
        float core = b2 * b2 * b2 * b2;
        float scatter = 1.0 / (1.0 + a2 * 0.12);
        float fade = exp(-ang / (reach * 0.55)) * smoothstep(0.0, 0.06, ang) * (1.0 - smoothstep(reach, reach + 0.4, ang));
        vec3 c = mix(uColA, uColB, h.x);
        light += (c * (body + scatter * 0.12) + hot(c) * core * 0.9) * fade;
    }
    col += light * 1.1;
    // The haze the lights make low over the horizon.
    col += mix(uColA, uColB, 0.5) * uOptB.x * (0.3 * exp(-max(el, 0.0) * 10.0) + 0.06 * exp(-max(el, 0.0) * 3.0));
    col = col / (1.0 + luma(col) * 0.2);
    vec3 hz = mix(vec3(0.0431, 0.0196, 0.0863), uColA, 0.3);
    return horizon(col, d, hz);
}
