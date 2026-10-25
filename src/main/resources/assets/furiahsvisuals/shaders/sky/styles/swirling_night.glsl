// @colors 1F3F99 F2CF4E
// Swirling Night: a painterly night sky of swirling brush strokes, glowing star halos and a crescent moon.

// The wind of the painting: a gentle drift around the horizon plus three big whirls.
// Centers of the three whirls: dirFrom(0.25, 0.5), dirFrom(1.5, 0.38) and dirFrom(-1.0, 0.55).
const vec3 WHIRL1 = vec3(0.2171, 0.4794, -0.8503);
const vec3 WHIRL2 = vec3(0.9264, 0.3709, -0.0657);
const vec3 WHIRL3 = vec3(-0.7174, 0.5227, -0.4606);

vec3 swirlFlow(vec3 p) {
    // 1 - dot is about half the squared angle, which is cheaper than acos for these soft falloffs.
    vec3 v = cross(vec3(0.0, 1.0, 0.0), p) * 0.35;
    v += cross(WHIRL1, p) * 2.4 * exp(-(1.0 - dot(p, WHIRL1)) * 14.0);
    v += cross(WHIRL2, p) * 1.9 * exp(-(1.0 - dot(p, WHIRL2)) * 20.0);
    v -= cross(WHIRL3, p) * 1.7 * exp(-(1.0 - dot(p, WHIRL3)) * 16.0);
    return v;
}

// Angle to a direction, from the chord length (no acos; exact enough for the small angles it is used for).
float chordAngle(vec3 a, vec3 b) {
    return sqrt(max(0.0, 2.0 - 2.0 * dot(a, b)));
}

vec3 skyColor(vec3 d) {
    float t = uTime * 0.05;
    // Brush strokes: fine noise smeared along the wind (a short walk both ways along the flow, averaging what is
    // met), so every stroke follows the swirls.
    float steps = 4.0 + uDetail;
    float paint = 0.0;
    vec3 fwd = d;
    vec3 back = d;
    vec3 np = vec3(t * 0.8, 0.0, 0.0);
    for (int i = 0; i < 12; i++) {
        if (float(i) >= steps) {
            break;
        }
        paint += noise(fwd * 70.0 / uScale + np) + noise(back * 70.0 / uScale + np);
        fwd = normalize(fwd + normalize(swirlFlow(fwd) + 1e-4) * 0.011);
        back = normalize(back - normalize(swirlFlow(back) + 1e-4) * 0.011);
    }
    paint = paint / (2.0 * steps) * 2.6;
    // Bands of light around the whirls, the way the famous painting rings its swirls.
    float a1 = chordAngle(d, WHIRL1);
    float a2 = chordAngle(d, WHIRL2);
    float rings = max(sin(a1 * 38.0 - t * 2.0) * exp(-a1 * a1 * 5.0), sin(a2 * 44.0 + t * 2.0) * exp(-a2 * a2 * 8.0));
    float broad = fbmN(d * 2.5 + vec3(t, 0.0, 0.0), 3.0);
    vec3 deep = uColA * 0.45;
    vec3 light = mix(uColA * 1.7, vec3(0.62, 0.8, 1.0), 0.45);
    vec3 col = mix(deep, light, smoothstep(-0.35, 0.45, paint + broad * 0.6));
    col = mix(col, vec3(0.82, 0.93, 1.0), smoothstep(0.35, 0.8, paint + rings * 0.5) * 0.45);
    col = mix(col, mix(uColB, vec3(1.0), 0.45), smoothstep(0.6, 1.0, rings) * smoothstep(0.0, 0.5, paint + 0.3) * 0.4);
    // Towards the horizon a greener, calmer band.
    col = mix(col, vec3(0.13, 0.27, 0.28), exp(-max(d.y, 0.0) * 7.0) * 0.45);
    gStarMask = 0.3;
    // Star halos: a few big stars with rings of light around them, gently pulsing.
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        vec3 h = hash33(vec3(fi, 4.0, 9.0));
        vec3 sd = dirFrom(h.x * TAU, 0.18 + h.y * 0.9);
        float a = chordAngle(d, sd) / uScale;
        if (a > 0.35) {
            continue; // too far from this star for its halo to show
        }
        float size = 0.012 + 0.014 * h.z;
        float pulse = 0.85 + 0.15 * sin(uTime * (0.6 + h.z) + fi);
        float halo = exp(-a / (size * 2.2)) * 0.6 + 0.3 * smoothstep(0.35, 1.0, sin(a / size * 3.5 - uTime * 0.4)) * exp(-a / (size * 5.0));
        col += mix(uColB, vec3(1.0), 0.4) * halo * pulse;
        col += vec3(1.0, 0.97, 0.85) * exp(-a * a / (size * size * 0.15)) * pulse;
    }
    // A crescent moon with a warm halo.
    if (uSunOwn > 0.5) {
        vec3 moonDir = dirFrom(1.9, 0.55);
        float R = 0.05 * uScale;
        float moon = disk(d, moonDir, R);
        vec3 shadowDir = normalize(moonDir + vec3(-0.03, 0.04, 0.024));
        float bite = disk(d, shadowDir, R * 0.92);
        col += uColB * 0.45 * exp(-max(angleTo(d, moonDir) - R, 0.0) * 18.0);
        col = mix(col, mix(uColB, vec3(1.0, 0.85, 0.4), 0.5) * 1.3, moon * (1.0 - bite));
    }
    return col;
}
