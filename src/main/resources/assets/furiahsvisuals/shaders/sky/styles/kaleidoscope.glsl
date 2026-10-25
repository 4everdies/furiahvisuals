// @colors C04BFF FF5AD9
// Kaleidoscope: a tunnel of nested neon star outlines opening out of the sky, each ring the last one grown, flowing
// outwards and turning; white-hot cores in a colored glow, the big outer rings soft (a little depth of field). It opens
// straight overhead, or on the horizon in a compass direction.
// Options: uOptA.x petals (4-12), uOptA.y ring spacing, uOptA.z zoom speed, uOptA.w rotation;
//          uOptB.x outer blur, uOptB.y center (0 zenith, 1 horizon), uOptB.z direction (degrees).

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime;
    vec3 center = uOptB.y > 0.5 ? dirFrom(radians(uOptB.z), 0.04) : vec3(0.0, 1.0, 0.0);
    vec3 right;
    vec3 up;
    frameOf(center, right, up);
    float th = angleTo(d, center);
    float phi = atan(dot(d, up), dot(d, right));
    float n = floor(uOptA.x + 0.5);
    float spacing = 0.3 * uOptA.y * uScale;
    // Log-polar: equal steps outwards are equal growth, which makes the rings a tunnel.
    float z = log(max(th, 1e-4)) / spacing - t * 0.32 * uOptA.z;
    // Where the rings get closer than a pixel (towards the middle), they fade into a glow instead of shimmering.
    float fine = clamp(1.6 - fwidth(z) * 3.0, 0.0, 1.0);
    float soft = mix(0.004, 0.07, uOptB.x * smoothstep(0.35, 2.3, th));
    float k0 = floor(z);
    vec3 light = vec3(0.0);
    for (int i = -1; i <= 1; i++) {
        float k = k0 + float(i);
        float rot = t * 0.22 * uOptA.w * (1.0 + 0.2 * mod(k, 3.0)) + k * 0.41;
        // A star of n points: its outline sits further out at the points.
        float star = pow(abs(cos((phi + rot) * n * 0.5)), 0.7);
        float outline = k + 0.2 + 0.6 * star;
        float dz = abs(z - outline) * spacing;
        float coreW = 0.0055 + soft * 0.3;
        float core = exp(-dz * dz / (coreW * coreW)) * (0.0055 / coreW);
        float halo = (soft * 2.5 + 0.004) / (soft * 2.5 + 0.004 + dz * dz * 150.0);
        vec3 c = mix(uColA, uColB, fract(k * 0.37 + 0.15));
        light += hot(c) * core * 1.2 + c * (core * 0.6 + halo * 0.75);
    }
    vec3 col = vec3(0.012, 0.006, 0.024);
    col += light * fine;
    // The heart of the tunnel: a soft white-hot glow.
    col += hot(uColA) * (0.25 / (1.0 + th * th * 260.0) + 0.12 * (1.0 - fine) * exp(-th * 6.0));
    // A faint haze of the colors fills the sky around.
    col += mix(uColA, uColB, 0.5) * 0.06 * exp(-th * 1.2);
    col = col / (1.0 + luma(col) * 0.22);
    vec3 hz = mix(vec3(0.0235, 0.0118, 0.0627), mix(uColA, uColB, 0.5), 0.16);
    return horizon(col, d, hz);
}
