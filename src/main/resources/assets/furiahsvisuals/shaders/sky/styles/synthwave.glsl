// @colors FF3CAC 22D3FF
// Synthwave: a retro sunset. A striped neon sun sinks behind glowing wireframe mountains under a purple sky, and a
// neon grid races towards you below the horizon.
vec3 skyColor(vec3 d) {
    float t = uTime;
    float el = elevation(d);
    float az = azimuth(d);
    // Sky: hot orange at the horizon, then pink, then deep indigo overhead.
    vec3 col = mix(vec3(1.0, 0.5, 0.3), uColA, smoothstep(0.0, 0.12, el));
    col = mix(col, vec3(0.28, 0.04, 0.4), smoothstep(0.1, 0.4, el));
    col = mix(col, vec3(0.025, 0.008, 0.08), smoothstep(0.35, 1.1, el));
    gStarMask = smoothstep(0.2, 0.45, el);
    // The sun, low in the north: yellow on top, pink below, cut by bars that thicken towards the bottom and
    // slide down.
    vec3 sunDir = dirFrom(0.0, 0.13);
    vec2 s = tangentCoords(d, sunDir) / uScale;
    float R = 0.21;
    float sd = length(s);
    // Only the half of the sky facing the sun: behind it the flat projection breaks down.
    float front = smoothstep(0.1, 0.3, dot(d, sunDir));
    float aa = min(fwidth(sd), 0.02) + 1e-4;
    float body = (1.0 - smoothstep(R - aa, R + aa, sd)) * front;
    float y = s.y / R;
    float bars = fract(y * 7.0 + t * 0.35);
    float thick = clamp((0.25 - y) * 0.42, 0.0, 0.48);
    float baw = min(fwidth(y * 7.0), 0.2) + 1e-4;
    float gap = smoothstep(thick - baw, thick + baw, bars) * step(-0.99, y);
    vec3 sunCol = mix(uColA, vec3(1.0, 0.9, 0.35), smoothstep(-0.8, 0.8, y));
    if (uSunOwn > 0.5) {
        col = mix(col, sunCol * 1.25, body * mix(1.0, gap, step(y, 0.25)));
        col += sunCol * 0.45 * exp(-max(sd - R, 0.0) * 9.0) * front * (1.0 - body);
    }
    // Mountains along the horizon, dark with a neon ridge line.
    vec3 hp = vec3(cos(az), sin(az), 0.0) * 3.0;
    float ridgeLine = 0.012 + 0.07 * pow(max(0.5 + 0.5 * fbmN(hp + 7.0, 5.0), 0.0), 1.6);
    float rw = fwidth(el) * 1.5 + 0.0015;
    float mountain = 1.0 - smoothstep(ridgeLine - rw, ridgeLine + rw, el);
    col = mix(col, vec3(0.07, 0.015, 0.14), mountain * step(0.0, el));
    col += uColB * exp(-(el - ridgeLine) * (el - ridgeLine) / 0.000004) * step(0.0, el) * 1.2;
    col += uColB * 0.15 * exp(-(el - ridgeLine) * (el - ridgeLine) / 0.0004) * step(0.0, el);
    // Below the horizon: a neon grid on a dark floor, lines racing towards you.
    if (el < 0.0) {
        vec2 g = d.xz / max(-d.y, 0.003) * 0.35 / uScale;
        g.y -= t * 0.9;
        vec2 cell = abs(fract(g) - 0.5);
        vec2 w = fwidth(g) * 1.2 + 0.015;
        float lines = max(1.0 - smoothstep(0.0, w.x, 0.5 - cell.x), 1.0 - smoothstep(0.0, w.y, 0.5 - cell.y));
        float dist = length(d.xz / max(-d.y, 0.003));
        float fade = exp(-dist * 0.025);
        col = mix(vec3(0.05, 0.0, 0.1), vec3(0.2, 0.02, 0.2), exp(-dist * 0.01));
        col += mix(uColA, uColB, fade) * lines * (0.3 + 0.9 * fade);
        col += uColA * 0.35 * exp(el * 40.0);
    }
    return col;
}
