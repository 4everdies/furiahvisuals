// @colors D43CFF FFFFFF
// Violet Storm: bright magenta and white clouds churning like marbled liquid (noise bent by noise bent by noise), dark
// streaks where the flow folds, white-hot hearts; fast and dramatic. With Match Fog the world's fog turns violet too.
// Soft, so it is drawn at half resolution and smoothly scaled up.
// Options: uOptA.x turbulence, uOptA.y flow speed, uOptA.z flow direction (compass degrees), uOptA.w cloud contrast.

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime * 0.22 * uOptA.y;
    float az = radians(uOptA.z);
    vec3 flow = vec3(sin(az), 0.0, -cos(az)) * t;
    vec3 p = d * (1.25 / uScale) + flow * 0.6 + uParallax * 0.15;
    float tb = uOptA.x;
    vec3 q = vec3(fbmN(p + vec3(0.0, 0.0, t * 0.3), 4.0), fbmN(p + vec3(5.2, 1.3, -t * 0.2), 4.0),
            fbmN(p + vec3(1.7, 9.2, t * 0.25), 4.0));
    vec2 r = vec2(fbmN(p + 2.2 * tb * q + vec3(1.7, 9.2, t * 0.4), 4.0), fbmN(p + 2.2 * tb * q + vec3(8.3, 2.8, -t * 0.35), 4.0));
    float f = fbm(p + 2.6 * tb * vec3(r, r.x - r.y));
    float c = uOptA.w;
    // Deep violet between the clouds, magenta bodies, white-hot hearts.
    vec3 deep = uColA * 0.06 + vec3(0.018, 0.0, 0.045);
    vec3 col = mix(deep, uColA * 0.8, smoothstep(-0.4, 0.2, f * (0.6 + 0.4 * c)));
    col = mix(col, hot(uColB), smoothstep(0.28, 0.72, f + 0.12 * length(q)) * clamp(0.35 + 0.45 * c, 0.2, 1.2));
    // Dark streaks where the flow folds over itself.
    float fold = exp(-pow((r.x - r.y) * 8.0, 2.0));
    col *= 1.0 - 0.55 * c * fold * smoothstep(-0.2, 0.3, f);
    // A little lighter overhead, heavier towards the horizon.
    col *= 0.85 + 0.25 * smoothstep(-0.1, 0.9, d.y);
    vec3 hz = mix(vec3(0.102, 0.0235, 0.149), uColA, 0.45);
    return horizon(col, d, hz);
}
