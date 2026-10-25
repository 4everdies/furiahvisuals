// @colors FF52E4 7466FF
// Liquid Vortex: glossy bands of pink and violet liquid twisting into a vortex overhead and stretching down to the
// horizon as long streaks.

// One spiral layer: returns the band value (-1..1) and which color it takes (-1..1).
vec2 spiral(float th, float r, vec2 warp, float arms, float twist, float t) {
    float phase = th * arms + log(r + 0.03) * twist - t + warp.x * 3.0 + warp.y * 1.2;
    return vec2(sin(phase), sin(phase * 0.5 + warp.y * 1.5));
}

vec3 skyColor(vec3 d) {
    float t = uTime * 0.25;
    // The eye of the vortex wanders slowly around the zenith.
    vec3 eye = normalize(vec3(0.3 * sin(t * 0.37), 1.0, 0.3 * cos(t * 0.29)));
    float r = acos(clamp(dot(d, eye), -1.0, 1.0));
    vec3 ref = normalize(cross(eye, vec3(0.0, 0.0, 1.0)));
    vec3 ref2 = cross(eye, ref);
    float th = atan(dot(d, ref2), dot(d, ref));
    vec3 np = d * (2.0 / uScale) + vec3(0.0, t * 0.35, 0.0);
    vec2 warp = fbm2N(np, 3.0);
    // Back layer: wider, dimmer bands turning the other way, seen through the gaps of the front layer. Arm counts
    // must be even: the colors alternate every other band, so an odd count would swap them along one line.
    vec2 back = spiral(th, r, warp * 0.7, 6.0, -3.5 / uScale, -t * 0.9);
    vec3 backTube = mix(uColA, uColB, 0.5 + 0.5 * back.y);
    vec3 col = backTube * 0.18 * smoothstep(0.1, 0.6, back.x);
    // Front layer: glossy tubes with dark outlines and a sharp highlight.
    vec2 front = spiral(th, r, warp, 12.0, 8.0 / uScale, t * 1.6);
    float b = front.x;
    vec3 tube = mix(uColB, uColA, smoothstep(-0.25, 0.25, front.y));
    float u = clamp((b - 0.3) / 0.7, 0.0, 1.0);
    float body = smoothstep(0.3, 0.38, b);
    float outline = smoothstep(0.2, 0.27, b) * (1.0 - smoothstep(0.27, 0.35, b));
    vec3 lit = tube * (0.3 + 0.7 * sqrt(u)) + mix(tube, vec3(1.0), 0.65) * pow(u, 9.0) * 0.6;
    col = mix(col, lit, body);
    col = mix(col, tube * 0.1, outline);
    // Thin contour lines of liquid between the bands.
    float fine = sin(asin(clamp(b, -1.0, 1.0)) * 3.0 + 1.0);
    col += tube * 0.7 * smoothstep(0.9, 1.0, fine) * (1.0 - body) * smoothstep(-0.95, -0.2, b);
    // Ripples around the eye, and a glow of both colors that grows near it.
    float rings = sin(r * 60.0 - t * 5.0 + warp.x * 5.0);
    col += tube * 0.4 * smoothstep(0.7, 1.0, rings) * exp(-r * 5.0) * (1.0 - body);
    col += mix(uColA, uColB, 0.5) * (0.02 + 0.12 * exp(-r * 3.0)) * (1.0 - body);
    return col;
}
