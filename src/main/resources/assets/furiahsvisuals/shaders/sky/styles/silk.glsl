// @colors A24DF5 79E84A
// Silk: glossy sheets of colored silk sweeping around you and folding into the horizon, violet on one side and
// lime on the other, with deep teal where they turn away from the light.
vec3 skyColor(vec3 d) {
    float t = uTime * 0.12;
    float az = azimuth(d);
    float el = elevation(d);
    // Sheets get denser towards the horizon, like the folds of fabric seen edge-on, and sway gently.
    float sway = 0.18 * sin(az * 2.0 + t * 1.3) + 0.1 * sin(az * 5.0 - t * 0.9) + 0.06 * noise(vec3(d * 2.0) + vec3(t));
    float k = sign(el) * pow(abs(el) / (PI * 0.5), 0.55) * 7.5 / uScale + sway * (0.4 + abs(el));
    float band = fract(k + t * 0.25);
    // Each sheet: a crisp bright edge, then a glossy falloff into shadow.
    float edge = exp(-band * band * 900.0) + exp(-(1.0 - band) * (1.0 - band) * 5000.0) * 0.5;
    float gloss = pow(1.0 - band, 2.2);
    // Colors travel slowly around you: violet, teal shadow, lime, teal shadow. Worked out from the direction
    // itself (not its angle), so they meet smoothly overhead instead of pinching to a point.
    float a = t * 0.35;
    float side = dot(d.xz, vec2(cos(a), sin(a))) / (0.35 + 0.65 * length(d.xz));
    float hue = clamp(0.5 + 0.5 * side + 0.08 * sin(k * 0.4), 0.0, 1.0);
    vec3 shadow = vec3(0.02, 0.1, 0.13);
    vec3 tint = mix(uColA, uColB, smoothstep(0.35, 0.65, hue));
    float turn = 1.0 - smoothstep(0.0, 0.35, abs(hue - 0.5));
    vec3 base = mix(tint, shadow, turn * 0.85);
    vec3 col = mix(shadow * 0.6, base, 0.25 + 0.75 * gloss);
    col += mix(vec3(1.0), tint, 0.5) * edge * (0.45 + 0.4 * (1.0 - turn));
    // Towards the zenith and nadir the sheets open into a smooth, soft color.
    float pole = smoothstep(0.75, 0.98, abs(d.y));
    col = mix(col, mix(base, shadow, 0.3) * 0.8, pole);
    return col;
}
