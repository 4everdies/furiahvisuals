// @colors FF8A3D E0608F
// Golden Hour: a sunset that never ends. The low sun lights slowly drifting clouds gold and pink under a sky that
// deepens to violet overhead.
vec3 skyColor(vec3 d) {
    float t = uTime * 0.02;
    vec3 sunDir = dirFrom(-1.35, 0.07);
    float mu = dot(d, sunDir);
    vec3 horizon = mix(vec3(1.0, 0.62, 0.35), uColA, 0.4);
    vec3 col = gradient(d, horizon, vec3(0.15, 0.19, 0.42), 0.42);
    // Pinker away from the sun, a warm glow and a bright halo around it.
    col = mix(col, vec3(0.62, 0.36, 0.55), exp(-max(d.y, 0.0) * 6.0) * 0.3 * (1.0 - smoothstep(-0.2, 0.8, mu)));
    col += vec3(1.0, 0.68, 0.38) * pow(max(mu, 0.0), 8.0) * 0.55;
    col += vec3(1.0, 0.86, 0.6) * pow(max(mu, 0.0), 350.0) * 1.3;
    if (uSunOwn > 0.5) {
        col = mix(col, vec3(1.0, 0.93, 0.78) * 1.6, disk(d, sunDir, 0.035));
    }
    // Rays fanning out from the sun.
    vec2 sp = tangentCoords(d, sunDir);
    float ang = atan(sp.y, sp.x);
    float rays = pow(0.5 + 0.5 * noise(vec3(cos(ang) * 4.0, sin(ang) * 4.0, t * 4.0)), 3.0);
    col += vec3(1.0, 0.8, 0.5) * rays * 0.14 * pow(max(mu, 0.0), 3.0) * step(0.0, d.y);
    // Clouds: a deck of soft clouds, lit from the side by the low sun (gold where it faces the sun, pink-violet
    // in its own shadow).
    if (uClouds > 0.5 && d.y > 0.0) {
        vec2 pos = deck(d, 1.0) * 0.45 / uScale + vec2(t, t * 0.35) + uParallax.xz * 0.05;
        float dens = fbm(vec3(pos, 0.5));
        float cover = smoothstep(0.02, 0.4, dens);
        vec2 toSun = normalize(sunDir.xz) * 0.09;
        float dens2 = fbmN(vec3(pos + toSun, 0.5), 4.0);
        float lit = clamp(0.5 + (dens - dens2) * 3.5, 0.0, 1.0);
        vec3 cloudCol = mix(uColB * 0.55 + vec3(0.12, 0.06, 0.16), vec3(1.0, 0.74, 0.44), lit);
        cloudCol += vec3(1.0, 0.8, 0.5) * pow(max(mu, 0.0), 10.0) * (1.0 - cover) * 1.2;
        col = mix(col, cloudCol, cover * smoothstep(0.0, 0.1, d.y) * 0.95);
    }
    if (d.y < 0.0) {
        col = mix(horizon * 0.45, vec3(0.09, 0.05, 0.09), smoothstep(0.0, -0.3, d.y));
    }
    // The first stars come out high up, never below the horizon.
    gStarMask = smoothstep(0.05, 0.4, d.y);
    return col;
}
