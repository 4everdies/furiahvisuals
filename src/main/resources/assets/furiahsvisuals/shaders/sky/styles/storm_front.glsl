// @colors 44587A CAD8FF
// Storm Front: dark clouds churning fast overhead, lit from inside by flickers of lightning, now and then a bolt.
vec3 skyColor(vec3 d) {
    float t = uTime * 0.08;
    vec3 p = d * (1.3 / uScale) + uParallax * 0.2;
    vec3 q = vec3(fbmN(p + vec3(0.0, t, 0.0), 3.0), fbmN(p + vec3(4.1, 1.3, -t), 3.0), fbmN(p + vec3(8.3, t, 2.7), 3.0));
    float c = fbm(p * 1.6 + q * 1.8 + vec3(t * 0.5, 0.0, t));
    float billow = ridge(p * 2.2 + q * 1.2 + vec3(0.0, -t, 0.0), 4.0);
    // Deep gaps between heavy clouds, whose rolling tops catch a cold light.
    vec3 col = mix(vec3(0.01, 0.012, 0.02), uColA * 0.5, smoothstep(-0.35, 0.45, c));
    col = mix(col, uColA * 1.25, smoothstep(0.5, 0.85, billow) * smoothstep(-0.1, 0.4, c) * 0.6);
    col = mix(col, uColA * 0.4, exp(-abs(d.y) * 9.0) * 0.35);
    gStarMask = 0.0;
    // Lightning: in some time slots a burst lights up a patch of cloud from inside, flickering; in some of those a
    // bolt drops to the horizon.
    float period = 1.6;
    float slot = floor(uTime / period);
    float local = uTime - slot * period;
    vec3 h = hash33(vec3(slot, 3.0, 7.0));
    vec3 where = dirFrom(h.x * TAU, 0.12 + h.y * 0.5);
    float on = step(0.25, h.z) * step(local, 0.45);
    float flicker = on * (0.55 + 0.45 * sin(local * 70.0 + h.x * 10.0)) * (1.0 - local / 0.45);
    float a = angleTo(d, where);
    col += uColB * flicker * exp(-a * a * 20.0) * (0.35 + 1.0 * smoothstep(0.3, 0.9, billow));
    vec2 bp = tangentCoords(d, where);
    float jag = bp.x - 0.035 * fbmN(vec3(bp.y * 7.0, slot, 1.0), 4.0) - 0.01 * noise(vec3(bp.y * 40.0, slot, 3.0));
    float along = step(-0.55, bp.y) * step(bp.y, 0.02) * smoothstep(0.1, 0.3, dot(d, where));
    float bw = min(fwidth(jag), 0.01) + 0.0015;
    float isBolt = step(0.55, h.z);
    float bolt = (1.0 - smoothstep(0.0, bw, abs(jag))) * along * isBolt;
    col += (uColB * 1.6 + 0.4) * bolt * flicker;
    col += uColB * 0.25 * exp(-jag * jag / 0.0004) * along * isBolt * flicker;
    return col;
}
