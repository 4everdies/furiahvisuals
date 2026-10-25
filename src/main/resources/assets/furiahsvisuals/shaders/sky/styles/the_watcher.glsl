// @colors 141414 F2EEE4
// The Watcher: the sky is light paper printed with fine halftone dots, and a giant eye made of big halftone dots looks
// down from one direction. It blinks every few seconds, glances around now and then, and its pupil follows where you
// look. Deliberately dotted: every dot is one of a real halftone's, its size read from the picture at its middle.
// Options: uOptA.x direction (compass degrees), uOptA.y height (degrees), uOptA.z size, uOptA.w blink rate;
//          uOptB.x follow me (1/0), uOptB.y invert (1/0), uOptB.z dot size.

// How much ink the picture has at q, in the eye's own flat coordinates (1 = half the eye's width).
float eyeInk(vec2 q, vec2 look, float open) {
    float x = q.x;
    float w = max(0.0, 1.0 - x * x);
    float shape = pow(w, 0.72);
    // The lids: the upper one comes down to close, the lower one stays.
    float upper = mix(-0.06, 0.5, open) * shape;
    float lower = -0.4 * shape;
    // The paper's own fine dots, and a soft shadow around the eye.
    float ink = 0.055;
    float around = length(q * vec2(0.8, 1.55));
    ink += 0.2 * exp(-max(0.0, around - 0.95) * 4.0) * step(around, 1.6);
    if (abs(x) < 1.0 && q.y < upper && q.y > lower) {
        ink = 0.08;
        // The iris, where it looks; a darker rim, fine streaks; the pupil; a light in it.
        vec2 ic = look * vec2(0.42, 0.2);
        vec2 rq = q - ic;
        float r = length(rq);
        if (r < 0.4) {
            float ang = atan(rq.y, rq.x);
            ink = 0.58 + 0.16 * sin(ang * 23.0 + sin(ang * 7.0) * 2.0) * smoothstep(0.12, 0.34, r)
                    + 0.26 * smoothstep(0.33, 0.4, r);
            if (r < 0.16) {
                ink = 1.0;
            }
            if (length(rq - vec2(0.1, 0.11)) < 0.055) {
                ink = 0.0;
            }
        }
        // Shade under the upper lid.
        ink = max(ink, 0.5 * (1.0 - smoothstep(0.0, 0.14, upper - q.y)));
    }
    if (abs(x) < 1.03) {
        // The lids' lines, and a few lashes above the upper one.
        ink = max(ink, 1.0 - smoothstep(0.02, 0.05, abs(q.y - upper)));
        ink = max(ink, 0.75 * (1.0 - smoothstep(0.012, 0.035, abs(q.y - lower))));
        float lx = x * 7.0;
        float li = floor(lx + 0.5);
        if (abs(li) <= 5.0 && open > 0.3) {
            vec2 root = vec2(li / 7.0, mix(-0.06, 0.5, open) * pow(max(0.0, 1.0 - li * li / 49.0), 0.72));
            vec2 tip = root + vec2(li * 0.012, 0.13 - abs(li) * 0.012);
            ink = max(ink, 1.0 - smoothstep(0.012, 0.03, segDist(q, root, tip)));
        }
    }
    return clamp(ink, 0.0, 1.0);
}

vec3 skyColor(vec3 d) {
    gStarMask = 0.0;
    float t = uTime;
    vec3 center = dirFrom(radians(uOptA.x), radians(uOptA.y));
    vec3 right;
    vec3 up;
    frameOf(center, right, up);
    float size = tan(clamp(0.3 * uOptA.z * uScale, 0.08, 1.1));

    // Blinks: every few seconds the lid closes and opens again in about a third of a second.
    float period = mix(9.0, 2.2, uOptA.w);
    float slot = floor(t / period);
    float bt = t - slot * period - hash12(vec2(slot, 3.0)) * 0.6;
    float open = 1.0 - (smoothstep(0.0, 0.09, bt) - smoothstep(0.13, 0.32, bt));
    // Where it looks: where you look (Follow Me), and now and then somewhere else for a moment.
    vec2 follow = vec2(dot(uLook, right), dot(uLook, up));
    follow = uOptB.x > 0.5 ? follow / max(1.0, length(follow)) : vec2(0.0);
    float gs = floor(t / 5.5);
    float gl = t - gs * 5.5;
    vec2 away = vec2(hash12(vec2(gs, 1.0)), hash12(vec2(gs, 2.0))) * 2.0 - 1.0;
    float glance = smoothstep(0.0, 0.25, gl) * (1.0 - smoothstep(1.2, 1.5, gl)) * step(0.4, hash12(vec2(gs, 5.0)));
    vec2 look = mix(follow, away / max(1.0, length(away)), glance);

    // The halftone: a grid of dots on the faces of a cube turned so its front face is centered on the eye.
    vec3 local = vec3(dot(d, right), dot(d, up), dot(d, center));
    vec3 cf = cubeFace(local);
    float cells = floor(92.0 / uOptB.z);
    vec2 g = (cf.xy * 0.5 + 0.5) * cells;
    vec2 cellId = floor(g);
    // The picture is read at the dot's middle (a real halftone), on the face in front of the eye.
    vec2 cuv = (cellId + 0.5) / cells * 2.0 - 1.0;
    float ink = 0.055;
    if (cf.z > 3.5 && cf.z < 4.5) {
        ink = eyeInk(cuv / size, look, open);
    }
    float radius = sqrt(ink) * 0.64;
    float dist = length(g - cellId - 0.5);
    float aa = fwidth(dist) * 0.75 + 1e-4;
    float dotCover = 1.0 - smoothstep(radius - aa, radius + aa, dist);

    vec3 paper = uOptB.y > 0.5 ? uColA : uColB;
    vec3 inkColor = uOptB.y > 0.5 ? uColB : uColA;
    // A little grain in the paper.
    paper *= 0.96 + 0.04 * noise(d * 40.0);
    vec3 col = mix(paper, inkColor, dotCover);
    vec3 hz = paper * 0.86;
    return horizon(col, d, hz);
}
