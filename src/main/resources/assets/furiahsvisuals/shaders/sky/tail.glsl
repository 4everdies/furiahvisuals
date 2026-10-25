// Furiah's Visuals, Dynamic Skies: the layers every style shares (stars, shooting stars, weather) and main().

vec3 stars(vec3 d) {
    if (uStars <= 0.0) {
        return vec3(0.0);
    }
    vec3 s = starLayer(d, 70.0, 0.55 * uStars, 0.09, 0.0);
    s += starLayer(d, 24.0, 0.28 * uStars, 0.07, 31.0) * 1.6;
    return s;
}

vec3 meteors(vec3 d) {
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        vec4 a = uMeteorA[i];
        vec4 b = uMeteorB[i];
        if (a.w <= 0.0 || dot(d, a.xyz) < 0.94) {
            continue;
        }
        vec3 rel = d - a.xyz;
        float along = -dot(rel, b.xyz);          // how far behind the head, along the trail
        float across = length(rel + b.xyz * along);
        float t = clamp(along / b.w, 0.0, 1.0);
        float trail = step(0.0, along) * (1.0 - t) * (1.0 - t);
        float w = 0.0009 + 0.0016 * (1.0 - t);
        float line = exp(-across * across / (w * w)) * trail;
        float head = exp(-dot(rel, rel) / 0.000012);
        acc += (line * 1.4 + head * 2.2) * a.w * mix(uStarCol, vec3(1.0), 0.5);
    }
    return acc;
}

void main() {
    vec3 v = skyDir();
    vec3 d = vec3(uSpin.x * v.x + uSpin.y * v.z, v.y, uSpin.x * v.z - uSpin.y * v.x);
    vec3 col = max(skyColor(d), 0.0);
    // Stars and shooting stars only show where the style itself is dark.
    float dark = (1.0 - smoothstep(0.06, 0.4, luma(col))) * gStarMask;
    col += (stars(d) + meteors(d)) * dark * (1.0 - uRain);
    // Weather: rain greys and dims the sky; lightning lights it up for a moment.
    float grey = luma(col);
    col = mix(col, vec3(grey) * 0.5 + vec3(0.035, 0.04, 0.05), uRain * 0.65);
    col += uFlash * vec3(0.55, 0.6, 0.85) * (0.25 + 0.75 * pow(max(dot(d, uFlashDir), 0.0), 3.0));
    // React to Music: brighter on the beat (a style with its own reactions has pulsed its own parts).
    col *= 1.0 + uBeat * 0.55 * (1.0 - gBeatOwn);
    // Grading.
    float l = luma(col);
    col = max(mix(vec3(l), col, uSat), 0.0) * uBright;
    // Contrast around a dark grey (these skies are mostly dark).
    col = max((col - 0.14) * uContrast + 0.14, 0.0);
    // A little noise against banding in smooth gradients.
    col += (hash12(gl_FragCoord.xy) - 0.5) / 255.0;
    gl_FragColor = vec4(col, uAlpha);
}
