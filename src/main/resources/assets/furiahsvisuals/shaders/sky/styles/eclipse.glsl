// @colors FFE6B8 1B2342
// Eclipse: day turns to dusk; a black sun hangs high, its corona streaming out in slow white-gold flames, with a
// sunset glow all around the horizon.
vec3 skyColor(vec3 d) {
    float t = uTime;
    vec3 sunDir = dirFrom(-0.4, 0.62);
    vec3 col = gradient(d, vec3(0.3, 0.2, 0.25), uColB * 0.55, 0.35);
    col += vec3(1.0, 0.55, 0.3) * 0.35 * exp(-abs(d.y) * 12.0);
    // Only the half of the sky facing the sun: behind it the flat projection breaks down.
    float front = smoothstep(0.1, 0.3, dot(d, sunDir));
    vec2 s = tangentCoords(d, sunDir) / uScale;
    float r = length(s);
    float R = 0.07;
    // Corona: streamers of different lengths, slowly changing (noise on the angle, sampled on a circle so it has
    // no seam).
    vec2 dir = s / max(r, 1e-5);
    float streams = 0.5 + 0.5 * fbmN(vec3(dir * 2.5, t * 0.04), 4.0);
    float outside = step(R, r) * front;
    float corona = exp(-(r - R) / (R * (0.25 + 0.9 * streams * streams))) * outside;
    col += uColA * corona * 1.2;
    col += uColA * 0.22 * exp(-(r - R) / (R * 3.5)) * outside;
    col += hot(uColA) * exp(-(r - R) * (r - R) / (R * R * 0.0025)) * 1.4 * front;
    // Small red prominences on the rim.
    float prom = smoothstep(0.55, 0.85, noise(vec3(dir * 7.0, t * 0.08))) * exp(-(r - R) / (R * 0.07)) * outside;
    col += vec3(1.0, 0.25, 0.32) * prom * 1.2;
    // The diamond ring: one glint of sunlight at the edge, breathing.
    vec2 gp = vec2(cos(2.3), sin(2.3)) * R;
    float glint = exp(-dot(s - gp, s - gp) / (R * R * 0.012)) * (0.65 + 0.35 * sin(t * 0.7));
    col += vec3(1.0, 0.97, 0.9) * glint * 2.0 * front;
    // The moon in front: a black disk.
    float aa = min(fwidth(r), 0.01) + 1e-4;
    float body = (1.0 - smoothstep(R - aa, R + aa, r)) * front;
    col = mix(col, vec3(0.004, 0.004, 0.008), body);
    gStarMask = 1.0 - front * (1.0 - smoothstep(R * 1.5, R * 6.0, r));
    return col;
}
