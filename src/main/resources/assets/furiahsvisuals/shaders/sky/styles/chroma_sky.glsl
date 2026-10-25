// @colors FF5A5A 5AFFD8
// Chroma Sky: a calm, glowing sky whose colors flow through the whole rainbow (or between your own two colors).
vec3 skyColor(vec3 d) {
    float t = uTime * 0.05;
    vec3 p = d * (0.85 / uScale);
    float n = fbmN(p + vec3(0.0, t * 0.8, t), 4.0);
    float n2 = fbmN(p * 1.8 - vec3(t, 0.0, 0.0) + 3.0, 3.0);
    float hue = t * 0.6 + n * 0.7 + d.y * 0.2;
    // A smooth cosine rainbow: every hue equally bright, no muddy bands between them.
    vec3 rainbow = 0.55 + 0.4 * cos(TAU * (hue + vec3(0.0, 0.33, 0.67)));
    vec3 custom = mix(uColA, uColB, 0.5 + 0.5 * sin(hue * TAU));
    vec3 col = mix(rainbow, custom, uCustom);
    // Soft luminous blobs, a pearly sheen and a slightly dimmer lower sky.
    col *= 0.8 + 0.3 * smoothstep(-0.3, 0.6, n2);
    col = mix(col, vec3(1.0), 0.15 * smoothstep(0.35, 0.8, n));
    col *= mix(0.65, 1.0, smoothstep(-0.5, 0.2, d.y));
    return col;
}
