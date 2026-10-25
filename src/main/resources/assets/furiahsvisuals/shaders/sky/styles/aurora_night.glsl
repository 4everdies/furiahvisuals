// @colors 38FF9C B04DFF
// Aurora Night: a clear starry night with curtains of northern lights rippling slowly across the northern sky.
vec3 skyColor(vec3 d) {
    float t = uTime * 0.12;
    vec3 col = gradient(d, vec3(0.02, 0.045, 0.085), vec3(0.003, 0.008, 0.028), 0.45);
    float az = azimuth(d);
    float el = elevation(d);
    // Noise is sampled on a circle around the horizon, so the curtains never have a seam.
    vec2 cs = vec2(cos(az), sin(az));
    // The northern half of the sky holds the lights, fading out towards the south.
    float north = smoothstep(-0.35, 0.55, cos(az));
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        vec3 q = vec3(cs * (1.4 + fi * 0.6) / uScale, t * (0.5 + fi * 0.13) + fi * 7.3);
        // Each curtain hangs from a wavy lower edge and fades upwards; where it folds it shines brighter.
        float base = 0.1 + fi * 0.12 + 0.13 * fbmN(q, 3.0);
        float height = 0.16 + 0.14 * (0.5 + 0.5 * noise(q * 1.7 + 3.0));
        float over = el - base;
        float edge = smoothstep(-0.008, 0.004, over);
        float glowUp = exp(-max(over, 0.0) / height) * edge;
        float under = exp(min(over, 0.0) * 45.0) * (1.0 - edge) * 0.35;
        // Rays: fine vertical streaks that shimmer along the curtain.
        float rays = 0.35 + 0.65 * pow(0.5 + 0.5 * noise(vec3(cs * 24.0 / uScale, t * 1.6 + fi * 3.0)), 2.0);
        rays *= 0.7 + 0.3 * (0.5 + 0.5 * noise(vec3(cs * 70.0 / uScale, t * 2.5 + fi)));
        float reach = smoothstep(0.1, 0.5, 0.5 + 0.5 * noise(vec3(cs * 0.9, t * 0.3 + fi * 2.0)));
        vec3 c = mix(uColA, uColB, smoothstep(0.02, height * 1.4, over));
        acc += c * (glowUp + under) * rays * reach * (1.0 - fi * 0.2);
    }
    col += acc * north * 1.3 * smoothstep(0.0, 0.05, el);
    // A faint green glow along the northern horizon.
    col += uColA * 0.06 * exp(-abs(d.y) * 9.0) * north;
    return col;
}
