// @colors F2F6FC 5C6B82
// Chrome Ink: glossy silver strands of liquid curling slowly through a black sky, like liquid metal in ink.
vec3 skyColor(vec3 d) {
    vec3 p = d * (1.2 / uScale) + uParallax * 0.25;
    p.x += uTime * 0.02;
    p = stir(p, uTime * 0.06, 0.2, 0.022);
    // Each band of the stirred field may carry one strand along its middle; about half stay empty (black).
    float s = p.y * 3.6 + p.x * 0.5;
    float id = floor(s);
    float x = fract(s) - 0.5;
    float keep = step(0.4, hash12(vec2(id, 7.0)));
    float wide = 0.6 + 0.8 * hash12(vec2(id, 3.0));
    // A strand is a small liquid ribbon: a bright rim on the lit side, a darker middle, a thinner rim on the other
    // side, all over a soft grey sheen. The ribbon slowly twists, so the lit side swaps now and then.
    float w = 0.05 * wide;
    float twist = sin(p.x * 1.7 + p.z * 1.3 + uTime * 0.25 + id);
    float lit = exp(-(x - w * 0.55 * twist) * (x - w * 0.55 * twist) / (w * w * 0.05));
    float rim = exp(-(x + w * 0.6 * twist) * (x + w * 0.6 * twist) / (w * w * 0.03)) * 0.45;
    float body = exp(-x * x / (w * w)) * 0.3;
    float sheen = exp(-x * x / (w * w * 9.0)) * 0.12;
    // Glints travel along the strands, as if light caught the liquid.
    float glint = pow(0.5 + 0.5 * sin(p.x * 2.6 + p.z * 2.1 + uTime * 0.45 + id * 1.7), 5.0);
    float light = keep * ((lit * 1.3 + rim) * (0.55 + 0.9 * glint) + body + sheen);
    vec3 col = mix(uColB, uColA, clamp(lit * (0.6 + 0.6 * glint), 0.0, 1.0)) * light;
    // A soft bloom where the light catches a strand.
    col += uColA * 0.1 * exp(-x * x / (w * w * 30.0)) * keep * (0.3 + glint);
    return col;
}
