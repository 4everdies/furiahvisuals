// @colors FF9A3C FFE9BD
// Black Hole: a huge black hole hangs over the north-eastern sky. Its disk of glowing matter spins around it and
// is bent over its top by gravity, and the stars behind it are warped into rings.
vec3 skyColor(vec3 d) {
    float t = uTime;
    vec3 c = dirFrom(0.7, 0.3);
    float front = smoothstep(0.0, 0.35, dot(d, c));
    vec2 uv = tangentCoords(d, c) / uScale;
    float rs = 0.15;
    float rho = length(uv);
    // The sky behind is pulled around the hole, so it is drawn here, stars included, and bends with it.
    gStarMask = 0.0;
    vec2 src = uv - uv * (rs * rs * 1.7) / max(rho * rho, 1e-5);
    vec3 bd = normalize(mix(d, fromTangent(src, c), front));
    vec3 col = vec3(0.006, 0.005, 0.012) + vec3(0.06, 0.035, 0.07) * smoothstep(0.0, 0.7, fbmN(bd * 2.0, 3.0));
    col += (starLayer(bd, 70.0, 0.55 * uStars, 0.09, 0.0) + starLayer(bd, 24.0, 0.28 * uStars, 0.07, 31.0) * 1.6) * (1.0 - uRain);
    // The accretion disk: a flat ring seen almost edge-on and tilted a little. Its near half crosses in front of
    // the hole; its far half is hidden behind it (and bent over its top, below).
    float tilt = 0.22;
    vec2 k = vec2(cos(tilt) * uv.x + sin(tilt) * uv.y, -sin(tilt) * uv.x + cos(tilt) * uv.y);
    float cosI = 0.13;
    float rd = length(vec2(k.x, k.y / cosI));
    float diskMask = smoothstep(rs * 1.4, rs * 2.0, rd) * (1.0 - smoothstep(rs * 4.5, rs * 7.5, rd));
    float ang = atan(k.y / cosI, k.x) - t * 0.9 * (rs * 2.0 / max(rd, rs));
    float swirl = 0.5 + 0.5 * fbmN(vec3(rd * 10.0, cos(ang) * 2.0, sin(ang) * 2.0), 3.0);
    float temp = smoothstep(rs * 6.5, rs * 1.6, rd);
    // Doppler beaming: the side spinning towards you is brighter and whiter.
    float doppler = 1.0 - 0.4 * clamp(k.x / (rs * 3.0), -1.0, 1.0);
    vec3 diskCol = mix(uColA * 0.9, uColB * 1.6, temp) * (0.6 + 1.0 * swirl) * doppler * 1.3 * diskMask;
    float nearSide = step(k.y, 0.0);
    // The far side of the disk, lensed into an arc that hugs the shadow, brightest over the top.
    float ringR = rs * (1.28 + 0.12 * smoothstep(-rs, rs, k.y));
    float arcMask = exp(-(rho - ringR) * (rho - ringR) / (rs * rs * 0.014));
    float top = smoothstep(-0.4, 0.8, k.y / max(rho, 1e-4));
    vec3 arc = mix(uColA, uColB, 0.6) * arcMask * (0.2 + 1.3 * top) * (0.6 + 0.5 * swirl);
    float photon = exp(-(rho - rs * 1.03) * (rho - rs * 1.03) / (rs * rs * 0.0012));
    float shadow = mix(1.0, smoothstep(rs * 0.97, rs * 1.02, rho), front);
    col = (col + diskCol * (1.0 - nearSide) * front) * shadow;
    col += (arc + uColB * photon * 1.2 + diskCol * nearSide) * front;
    col += uColA * 0.06 * exp(-rho * 7.0) * front * shadow;
    return col;
}
