// @colors C9962B FFF1B8
// Gilded Storm: heavy, dark storm clouds churn overhead, and golden light breaks through their gaps in broad rays that
// fan down from a light above them, shifting slowly as the clouds move. A radiant golden sigil, a star inside a ring of
// light, stands on the horizon in a chosen direction, slowly turning, with sparkles; golden motes drift in the light,
// and a warm gold haze lies along the horizon. The sigil rises out of the haze: there is no ground. Crisp (the sigil's
// lines), so it is drawn at full resolution.
// Options: uOptA.x cloud cover (0-1), uOptA.y ray strength (0-1), uOptA.z sigil (0/1), uOptA.w its direction (degrees,
// 0 north, 90 east); uOptB.x motes, uOptB.y haze.
// React to Music: the rays and the sigil brighten.

// Where cover puts the edge between cloud and gap in the clouds' noise: none of the sky (0) to all of it (1).
float gildedEdge(float cover) {
    return mix(0.45, -0.45, cover);
}

// noise2() with its filtering done here, at full precision: four fetches at texel centres instead of one filtered
// fetch, whose blend the graphics card rounds to 1/256 of a texel. Fine noise never shows that, but overhead the
// storm's broadest layers are so near that one texel spans most of the screen, and the clouds' lit margins would show
// the rounding as a grid of little squares.
vec2 gildedNoise2(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = (p.xy + vec2(37.0, 17.0) * p.z + 0.5) / 256.0;
    float k = 1.0 / 256.0;
    vec4 s = mix(mix(texture2D(uNoise, uv), texture2D(uNoise, uv + vec2(k, 0.0)), f.x),
            mix(texture2D(uNoise, uv + vec2(0.0, k)), texture2D(uNoise, uv + vec2(k, k)), f.x), f.y);
    return mix(s.xz, s.yw, f.z) * 2.0 - 1.0;
}

// fbmN() with its two broadest octaves from gildedNoise2() (the same noise; the finer octaves are small enough).
float gildedFbm(vec3 p, float octaves) {
    float s = 0.5 * gildedNoise2(p).x;
    p = OCT * p * 2.03;
    s += 0.25 * gildedNoise2(p).x;
    p = OCT * p * 2.03;
    float a = 0.125;
    for (int i = 2; i < 6; i++) {
        if (float(i) >= octaves) {
            break;
        }
        s += a * noise(p);
        p = OCT * p * 2.03;
        a *= 0.5;
    }
    return s;
}

// The storm clouds at a point of their deck: {thickness (0 a gap .. 1 thick), their lit margin along the gaps, the
// billows' light and shade (-1..1)}.
vec3 gildedClouds(vec2 uv, float cover, float t) {
    // (The warp is fbm2N's two octaves, at full precision too.)
    vec3 wp = vec3(uv * 0.5, t * 0.02);
    vec2 w = 0.5 * gildedNoise2(wp) + 0.25 * gildedNoise2(OCT * wp * 2.03);
    vec2 q = uv + w * 0.8 + vec2(t * 0.015, t * 0.006);
    float n = gildedFbm(vec3(q, t * 0.02), min(uDetail, 5.0));
    float edge = gildedEdge(cover);
    float thick = smoothstep(edge - 0.02, edge + 0.3, n);
    // The thin margin of each cloud, where the light behind it shines through.
    float rim = smoothstep(edge - 0.06, edge + 0.04, n) * (1.0 - smoothstep(edge + 0.04, edge + 0.24, n));
    float billow = noise(vec3(q * 3.1, t * 0.05 + 7.0));
    return vec3(thick, rim, billow);
}

// The clouds' thickness alone, roughly (two octaves, no warp): enough to tell where the light finds a gap.
float gildedCover(vec2 uv, float cover, float t) {
    float n = fbmN(vec3(uv + vec2(t * 0.015, t * 0.006), t * 0.02), 2.0);
    float edge = gildedEdge(cover);
    return smoothstep(edge - 0.02, edge + 0.3, n);
}

// lineAA with the pixel's size given: worked out once, before any branch (derivatives inside one can't be trusted).
float gildedLine(float dist, float halfWidth, float px) {
    float aa = px * 0.75 + 1e-6;
    return (1.0 - smoothstep(halfWidth - aa, halfWidth + aa, dist)) * min(1.0, halfWidth / aa);
}

// The distance from p to a star of `points` points, outer radius `r`, inner radius `ri` (inside negative), around the
// origin and turned by `turn`: the plane is folded into one point's half-slice, where the star's edge is one segment.
float gildedStar(vec2 p, float points, float r, float ri, float turn) {
    float slice = PI / points;
    float a = atan(p.y, p.x) - turn;
    float f = abs(mod(a + slice, 2.0 * slice) - slice);
    vec2 q = length(p) * vec2(cos(f), sin(f));
    vec2 tip = vec2(r, 0.0);
    vec2 notch = ri * vec2(cos(slice), sin(slice));
    float dist = segDist(q, tip, notch);
    // Inside when on the origin's side of the edge.
    vec2 e = notch - tip;
    float side = e.x * (q.y - tip.y) - e.y * (q.x - tip.x);
    return side > 0.0 ? -dist : dist;
}

vec3 skyColor(vec3 d) {
    gBeatOwn = 1.0;
    float t = uTime;
    float el = elevation(d);
    float az = azimuth(d);
    float cover = uOptA.x;
    float rays = uOptA.y;
    float sigilOn = uOptA.z;
    float sigilAz = radians(uOptA.w);
    float motes = uOptB.x;
    float hazeAmt = uOptB.y;
    vec3 gold = uColA;
    vec3 pale = uColB;
    float bright = 1.0 + uBeat * 0.9;
    // A pixel's size on the sky (radians).
    float px = length(fwidth(d));

    // Above the storm: the light, high over the sigil's side of the sky.
    vec3 light = dirFrom(sigilAz, 0.95);
    float toLight = max(dot(d, light), 0.0);

    // Behind the clouds: the sky the light fills, golden, brightest around the light and low down.
    float up = clamp(el, 0.0, 1.5);
    vec3 col = gold * (0.13 + 0.1 * (1.0 - smoothstep(0.0, 0.7, up)));
    col += mix(gold, pale, 0.6) * (pow(toLight, 5.0) * 0.3 + pow(toLight, 40.0) * 0.55);

    // The clouds: a deck overhead, dark olive-brown where thick, edged with gold where the light comes through a gap.
    float thick = 0.0;
    float deckScale = 0.7 / uScale;
    if (cover > 0.0) {
        vec3 c = gildedClouds(deck(d, 1.0) * deckScale, cover, t);
        // (Far away the deck's detail is finer than a pixel: it evens out there instead of shimmering.)
        float far = 1.0 - smoothstep(0.015, 0.16, d.y);
        float some = smoothstep(0.0, 0.15, cover);
        thick = mix(c.x, cover, far) * some;
        // Billowing: lighter and darker swells across each cloud, and its lit margin broken by them.
        float shade = smoothstep(0.2, 1.0, thick) * (0.65 + 0.35 * c.z);
        vec3 cloud = mix(gold * 0.1 + vec3(0.03, 0.026, 0.02), vec3(0.014, 0.012, 0.01), shade);
        float rim = c.y * (1.0 - far) * some * (0.65 + 0.35 * c.z);
        // The margins are brightest toward the light.
        float lightSide = 0.35 + 0.65 * smoothstep(-0.2, 0.9, dot(d, light));
        col = mix(col, cloud, thick) + mix(gold, pale, 0.55) * rim * lightSide * 0.8;
    }

    // The rays: broad shafts fanning out from the light, each lit all along when the clouds have a gap where it came
    // through (a fixed way out from the light along it), so they shift slowly as the clouds move.
    if (rays > 0.0 && dot(d, light) > 0.0) {
        vec2 lp = tangentCoords(d, light);
        float away = length(lp);
        vec2 along = lp / max(away, 1e-4);
        float gap = 1.0;
        if (cover > 0.0) {
            gap = 1.0 - gildedCover(deck(fromTangent(along * 0.32, light), 1.0) * deckScale, cover, t);
        }
        // Some broad, some narrow, drifting slowly round (round the circle: no seam where the angle wraps).
        float shafts = fbmN(vec3(along * 5.0, t * 0.04), 2.0) * 0.5 + 0.5;
        shafts = 0.35 + 0.65 * smoothstep(0.3, 0.8, shafts);
        float fade = exp(-away * 0.8) * smoothstep(0.15, 0.45, away);
        // Strongest in the air low over the horizon.
        float air = 0.35 + 0.65 * (1.0 - smoothstep(0.0, 0.9, el));
        col += mix(gold, pale, 0.35) * shafts * gap * fade * air * rays * 0.55 * bright;
    }

    // The haze: warm gold along the horizon.
    col += gold * hazeAmt * (0.42 * exp(-abs(el) * 9.0) + 0.06 * exp(-max(el, 0.0) * 2.5));

    // The sigil: a star inside a ring of light, standing on the horizon, slowly turning, with sparkles.
    if (sigilOn > 0.5 && el > -0.1 && el < 0.75) {
        vec3 center = dirFrom(sigilAz, 0.24);
        if (dot(d, center) > 0.9) {
            vec2 p = tangentCoords(d, center);
            float r = length(p);
            float turn = t * 0.06;
            float ang = atan(p.y, p.x);
            // The ring, a fainter inner ring, and the eight-pointed star between them.
            float ring = gildedLine(abs(r - 0.2), 0.0035, px);
            float inner = gildedLine(abs(r - 0.165), 0.0018, px);
            float sd = gildedStar(p, 8.0, 0.19, 0.075, turn);
            float star = gildedLine(abs(sd), 0.0026, px);
            float fill = (1.0 - smoothstep(-0.002, 0.002, sd)) * 0.18;
            // Its radiance: a soft glow round the ring, a bright heart, and light streaming out past the star's points.
            float halo = exp(-abs(r - 0.2) * 22.0) * 0.35 + exp(-r * 9.0) * 0.5;
            float tips = pow(max(cos((ang - turn) * 8.0), 0.0), 40.0);
            float beams = tips * smoothstep(0.17, 0.2, r) * exp(-max(r - 0.2, 0.0) * 16.0) * 0.6;
            // Sparkles: one in each of 24 slices round the ring, twinkling on its own.
            float cell = floor((ang + PI) / TAU * 24.0);
            vec3 h = hash33(vec3(cell, 7.0, 3.0));
            float sr = 0.2 + 0.03 * h.x + 0.02 * sin(t * (0.6 + h.y) + h.z * 6.0);
            float sa = (cell + 0.5) / 24.0 * TAU - PI + turn * 0.5;
            vec2 sp = sr * vec2(cos(sa), sin(sa));
            float sd2 = length(p - sp);
            float sparkle = exp(-sd2 * sd2 / 0.00004) * (0.5 + 0.5 * sin(t * (2.0 + 3.0 * h.y) + h.x * 20.0));
            vec3 sigil = hot(mix(gold, pale, 0.6)) * (ring + star * 0.9 + inner * 0.7) + pale * (fill + beams + sparkle * 1.4)
                    + gold * halo;
            col += sigil * bright;
        }
    }

    // Motes: golden specks drifting slowly in the light.
    if (motes > 0.0 && el > -0.04 && el < 0.9) {
        float cols = 80.0;
        // (They drift up and down only: a grid sliding round would cut a mote at the south, where the azimuth wraps.)
        vec2 cellUv = vec2(az / TAU * cols, el * 12.7 - t * 0.08);
        vec2 id = floor(cellUv);
        vec3 h = hash33(vec3(id, 51.0));
        if (h.x < motes * 0.3) {
            vec2 pos = id + vec2(0.3 + 0.4 * h.y + 0.08 * sin(t * (0.5 + h.z) + h.x * 30.0), 0.3 + 0.4 * h.z);
            vec2 dlt = cellUv - pos;
            dlt.x *= cos(el) * TAU * 12.7 / cols;
            // (A row of cells is 1/12.7 of a radian high.)
            float r = max(0.03, px * 12.7 * 0.8);
            float glowM = exp(-dot(dlt, dlt) / (r * r));
            float twinkle = 0.5 + 0.5 * sin(t * (1.5 + 2.0 * h.y) + h.z * 20.0);
            col += pale * glowM * twinkle * (1.0 - smoothstep(0.5, 0.9, el)) * 1.2;
        }
    }

    // (A few stars in the gaps only: the light behind the storm outshines the rest.)
    gStarMask = (1.0 - thick) * 0.1;
    col = col / (1.0 + luma(col) * 0.1);
    vec3 hz = mix(vec3(0.102, 0.0745, 0.0353), gold, 0.35);
    return horizon(col, d, hz);
}
