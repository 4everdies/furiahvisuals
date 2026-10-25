// @colors 4F8DFF FFD3A8
// Sea of Clouds: you are above the clouds. A calm ocean of cloud rolls out to the horizon under a clear sky,
// warmed by a low sun.
vec3 skyColor(vec3 d) {
    float t = uTime * 0.01;
    vec3 sunDir = dirFrom(1.1, 0.18);
    float mu = max(dot(d, sunDir), 0.0);
    vec3 col = gradient(d, vec3(0.76, 0.86, 0.96), uColA * 0.72, 0.5);
    col += uColB * (pow(mu, 6.0) * 0.35 + pow(mu, 250.0) * 1.1);
    if (uSunOwn > 0.5) {
        col = mix(col, vec3(1.0, 0.97, 0.9) * 1.5, disk(d, sunDir, 0.03));
    }
    gStarMask = 0.0;
    if (uClouds < 0.5) {
        return mix(col, vec3(0.6, 0.65, 0.72), smoothstep(0.02, -0.05, d.y));
    }
    // The sea's surface lies just below eye level; its billows rise a little above the horizon.
    float az = azimuth(d);
    float el = elevation(d);
    vec3 hp = vec3(cos(az), sin(az), 0.0) * 2.0;
    float billow = 0.012 + 0.05 * (0.5 + 0.5 * fbmN(hp * 1.6 + vec3(0.0, 0.0, t * 4.0), 4.0));
    float bw = fwidth(el) * 1.5 + 0.002;
    float sea = 1.0 - smoothstep(billow - bw, billow + bw, el);
    // Its surface seen from above: soft rolling cloud tops, lit warm on the sun side.
    vec2 pos = d.xz * (0.6 / max(billow + 0.04 - d.y, 0.03)) * 0.5 / uScale + vec2(t * 2.0, 0.0);
    float dens = fbm(vec3(pos, 1.0));
    float light = smoothstep(-0.35, 0.5, dens);
    vec3 cloudCol = mix(vec3(0.6, 0.64, 0.76), vec3(1.0, 0.96, 0.9), light);
    cloudCol += uColB * pow(mu, 3.0) * 0.45 * light;
    // The billow tops catch the light; the far sea fades into haze.
    cloudCol = mix(cloudCol, vec3(1.0, 0.97, 0.93), smoothstep(billow - 0.02, billow, el) * 0.6);
    cloudCol = mix(cloudCol, col, exp(-abs(el) * 25.0) * 0.35);
    return mix(col, cloudCol, sea);
}
