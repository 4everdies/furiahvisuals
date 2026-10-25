// @colors 2EE6FF 7A5CFF
// Plasma Veins: filaments of plasma twist and flow across a black sky like a living electric web: a thin white-hot
// core in each, a soft glow around it, slowly changing shape, with sparks running along them. Soft, so it is drawn
// at half resolution and smoothly scaled up.
// Options: uOptA.x vein density, uOptA.y thickness, uOptA.z flow speed, uOptA.w sparks.

vec3 skyColor(vec3 d) {
    float t = uTime * 0.12 * uOptA.z;
    vec3 p = d * (1.7 * uOptA.x / uScale) + uParallax * 0.2;
    // The web twists: the space it lives in flows.
    vec3 w = vec3(fbmN(p + vec3(0.0, t, 1.7), 3.0), fbmN(p + vec3(5.2, -t, 0.0), 3.0), fbmN(p + vec3(-2.3, 3.1, t), 3.0));
    vec3 q = p + w * 0.85;
    float n1 = noise(q * 1.8 + vec3(0.0, 0.0, t * 0.9));
    float n2 = noise(q * 3.3 + vec3(4.0, t * 1.1, 0.0));
    float th = 0.014 * uOptA.y;
    // The veins are where the noise crosses zero: a hot core, a soft glow.
    float a1 = abs(n1);
    float a2 = abs(n2);
    float core = exp(-a1 * a1 / (th * th)) + 0.55 * exp(-a2 * a2 / (th * th * 0.4));
    float halo = th * 3.0 / (th * 3.0 + a1 * a1 * 90.0) + 0.4 * th * 3.0 / (th * 3.0 + a2 * a2 * 160.0);
    // Sparks: bright beads carried along the veins.
    float s = noise(q * 7.0 + vec3(t * 6.0, -t * 5.0, t * 4.0));
    float spark = min(2.0, pow(max(0.0, s), 7.0) * 60.0 * core) * uOptA.w;
    vec3 col = vec3(0.004, 0.006, 0.012);
    col += mix(uColB, uColA, 0.7) * halo * 0.45;
    col += hot(uColA) * core * 0.9;
    col += vec3(1.0) * spark;
    gStarMask = 0.4;
    vec3 hz = mix(vec3(0.0039, 0.0157, 0.0314), uColA, 0.1);
    return horizon(col / (1.0 + luma(col) * 0.2), d, hz);
}
