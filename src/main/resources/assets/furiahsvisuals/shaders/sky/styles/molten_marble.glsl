// @colors FF1414 8A0000
// Molten Marble: red and black liquid marble that keeps flowing sideways in folded waves.
vec3 skyColor(vec3 d) {
    vec3 p = d * (1.4 / uScale) + uParallax * 0.25;
    p.x += uTime * 0.06;
    p = stir(p, uTime * 0.14, 0.18, 0.035);
    // Layers of paint: black, then ribbons of two reds separated by thin dark seams.
    float v = sin(p.y * 6.5 + p.x * 0.8);
    float shade = 0.5 + 0.5 * sin(p.y * 13.0 + p.z * 2.0 + 1.3);
    vec3 red = mix(uColB, uColA, smoothstep(0.25, 0.6, shade));
    vec3 col = mix(vec3(0.0), uColB * 0.6, smoothstep(-0.12, 0.0, v));
    col = mix(col, red, smoothstep(0.02, 0.14, v));
    float seam = sin(p.y * 27.0 + p.z * 3.0);
    col *= 1.0 - 0.8 * smoothstep(0.82, 0.98, seam) * smoothstep(0.08, 0.3, v);
    // A glossy highlight along the brightest part of each ribbon.
    col += uColA * 0.25 * smoothstep(0.8, 1.0, v) * smoothstep(0.55, 0.9, shade);
    return col;
}
