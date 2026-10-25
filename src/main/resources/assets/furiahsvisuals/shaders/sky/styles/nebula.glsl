// @colors D23C8C 2A8FD0
// Nebula: deep space full of glowing gas and dark dust, crossed by the band of the Milky Way; it drifts slowly.
vec3 skyColor(vec3 d) {
    float t = uTime * 0.012;
    vec3 p = d * (1.3 / uScale) + uParallax * 0.1 + vec3(t, 0.0, t * 0.6);
    vec3 q = vec3(fbmN(p + vec3(1.3, 2.1, 0.7), 3.0), fbmN(p + vec3(5.2, 1.7, 3.3), 3.0), fbmN(p + vec3(2.9, 8.1, 4.4), 3.0));
    float gas = fbm(p * 1.3 + q * 1.7);
    float tone = fbmN(p * 2.1 - q * 1.2 + 4.0, 4.0);
    // The galactic band: a tilted great circle where gas and stars crowd together.
    vec3 bandNormal = normalize(vec3(0.35, 1.0, 0.25));
    float along = dot(d, bandNormal);
    float band = exp(-along * along * 12.0);
    float density = smoothstep(-0.15, 0.55, gas + band * 0.4);
    vec3 c = mix(uColB, uColA, smoothstep(-0.35, 0.35, tone));
    c = mix(c, vec3(1.0, 0.68, 0.38), smoothstep(0.3, 0.65, gas) * 0.35);
    vec3 col = vec3(0.004, 0.004, 0.012) + c * density * density * 0.6;
    col += vec3(0.75, 0.7, 0.9) * band * 0.05;
    // Dark dust lanes through the band.
    float dust = smoothstep(0.55, 0.8, ridge(p * 2.2 + q * 0.8 + 11.0, 4.0));
    col *= 1.0 - 0.75 * dust * smoothstep(0.1, 0.8, band + density * 0.5);
    // Crowds of faint stars in the band.
    col += starLayer(d, 180.0, 0.35 * band * uStars, 0.12, 7.0) * 0.7;
    return col;
}
