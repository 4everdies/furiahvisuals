// The composite: one full-screen pass onto Minecraft's frame, before the HUD. color x (rim + halo + trail +
// animation). Halo and trail stay outside the silhouette unless Inner glow is above 0.
uniform sampler2D sMask;     // coverage of the view model (r)
uniform sampler2D sDist;     // r: distance outside / 32, g: distance inside / 8 (mask pixels)
uniform sampler2D sHalo;     // the blurred coverage (r)
uniform sampler2D sTrail;    // r: sqrt(strength), g: sqrt(freshness)
uniform sampler2D sNoise;    // value noise (r, g), 128 x 128, repeating
uniform float uHasMask;      // 0: no view model this frame (only an old trail fades out)
uniform float uMaskScale;    // mask pixels per screen pixel (1, or 2 for High)
uniform float uPx;           // screen pixels per 1080p pixel
uniform vec4 uRim;           // width (screen pixels), softness, intensity, hot core
uniform vec3 uHaloP;         // intensity, falloff (0 linear, 1 smooth, 2 exponential), curve
uniform vec2 uHaloSize;      // radius, and the sigma of its blur (screen pixels)
uniform float uHaloShape;    // 1: there is a halo (edge -> outward runs across it), 0: only the rim (uReach)
uniform float uHaloCovers;   // 1: the halo reaches past the rim, so where it is empty nothing else can show
uniform float uReach;        // no halo: pixels from the silhouette over which the colors run from A to B
uniform vec2 uInside;        // inner glow, item tint
uniform vec3 uTrailP;        // intensity, on (0 / 1), color by age (0 / 1)
uniform float uEcho;         // trail style: 1 Echo, 0 Smoke (the shape the trail keeps, see trail.fsh)
uniform vec4 uFlow;          // amount, noise cell size (pixels), cells travelled, direction (0 up, 1 outward)
uniform vec4 uShimmer;       // position along the item (0..1), half width (fraction of the item), strength, -
uniform vec3 uLook;          // gain (pulse, flicker, hit flash, target boost), flash (towards white), opacity
uniform int uBlend;          // 0 additive, 1 screen, 2 alpha
varying vec2 vUv;

float noise(vec2 p) {
    return texture2D(sNoise, p / 128.0).r;
}

float fbm(vec2 p) {
    return noise(p) * 0.55 + noise(p * 2.03 + vec2(17.0, 9.0)) * 0.3 + noise(p * 4.07 + vec2(41.0, 23.0)) * 0.15;
}

// Two noise values that drift up (like flames: stretched upwards into tongues) or away from the item's center.
vec2 flowField(vec2 px, vec2 center) {
    float cell = uFlow.y;
    float t = uFlow.z;
    if (uFlow.w < 0.5) {
        vec2 q = vec2(px.x / cell, px.y / (cell * 1.8) - t);
        return vec2(fbm(q), fbm(q * 1.9 + vec2(31.7, 11.3)));
    }
    // Carried outwards: two copies half a cycle apart, each restarting while the other is fully shown.
    vec2 dir = normalize(px - center + vec2(0.001, 0.0));
    float p1 = fract(t * 0.25);
    float p2 = fract(t * 0.25 + 0.5);
    vec2 q1 = px / cell - dir * p1 * 4.0;
    vec2 q2 = px / cell - dir * p2 * 4.0 + vec2(13.1, 7.7);
    vec2 a = vec2(fbm(q1), fbm(q1 + vec2(31.7, 11.3)));
    vec2 b = vec2(fbm(q2), fbm(q2 + vec2(31.7, 11.3)));
    return mix(a, b, abs(p1 * 2.0 - 1.0));
}

void main() {
    vec2 px = vUv * uScreen;
    float cover = 0.0;
    float dOut = 1.0e4;
    float dIn = 0.0;
    float h = 0.0;
    // Most of the screen is far from the hand: the two small textures decide that alone.
    if (uHaloCovers > 0.5 && uFlow.x <= 0.0) {
        float h0 = uHasMask > 0.5 ? texture2D(sHalo, vUv).r : 0.0;
        vec2 t0 = texture2D(sTrail, vUv).rg;
        if (h0 < 0.002 && t0.r * t0.r * uTrailP.y < 0.002) {
            discard;
        }
    }
    if (uHasMask > 0.5) {
        cover = texture2D(sMask, vUv).r;
        if (uRim.x > 0.0) {
            // The distance field is only made when there is a rim.
            vec2 dist = texture2D(sDist, vUv).rg;
            dOut = dist.r * 32.0 / uMaskScale;
            dIn = dist.g * 8.0 / uMaskScale;
        }
        h = texture2D(sHalo, vUv).r;
    }
    vec2 tr = texture2D(sTrail, vUv).rg;
    float trail = tr.r * tr.r * uTrailP.y;
    // Distance from the silhouette's outline (dOut is measured between pixel centers).
    float edge = max(dOut - 0.5 / uMaskScale, 0.0);
    // The glow as it is now, in the shape the trail keeps it (trail.fsh): only the part of the trail beyond it lags.
    float now = uEcho > 0.5 ? max(cover * 0.3, (1.0 - abs(h * 2.0 - 1.0)) * 0.9)
            : max(cover * 0.45, clamp(h * 2.0, 0.0, 1.0) * 0.5);

    float rimW = uRim.x;
    bool nearRim = rimW > 0.0 && edge < rimW * (1.0 + 1.2 * uRim.y) + 1.5;
    if (cover <= 0.0 && h < 0.002 && trail < 0.002 && !nearRim) {
        discard;
    }

    vec2 center;
    vec2 axis;
    float len;
    itemShape(center, axis, len);

    // Flow noise: the halo and trail are read from a place pushed along by drifting noise (so they stream up like
    // flames, or away from the item), and flicker with it.
    float flow = 1.0;
    if (uFlow.x > 0.0) {
        vec2 n = flowField(px, center);
        vec2 dir = uFlow.w < 0.5 ? vec2(0.0, 1.0) : normalize(px - center + vec2(0.001, 0.0));
        vec2 side = vec2(-dir.y, dir.x);
        // Tongues: pushed along the flow where the noise is high, wobbling sideways.
        vec2 shove = (dir * n.x * 24.0 + side * (n.y - 0.5) * 12.0) * uFlow.x * uPx;
        vec2 fuv = vUv - shove / uScreen;
        if (uHasMask > 0.5) {
            h = texture2D(sHalo, fuv).r;
        }
        tr = texture2D(sTrail, fuv).rg;
        trail = tr.r * tr.r * uTrailP.y;
        // Steady right at the item, flickering tongues further out.
        float outer = 1.0 - 0.65 * clamp(h * 2.0, 0.0, 1.0);
        flow = mix(1.0, 0.1 + 1.9 * smoothstep(0.28, 0.78, n.x), uFlow.x * outer);
    }
    float fresh = tr.g * tr.g;

    // Rim: a band hugging the outline, softened outwards; its core (next to the outline) runs hot.
    float rim = 0.0;
    float core = 0.0;
    if (rimW > 0.0) {
        float soft = uRim.y;
        rim = 1.0 - smoothstep(rimW - 0.5 - soft * rimW * 0.6, rimW + 0.5 + soft * rimW * 1.2, edge);
        core = exp(-edge / (0.3 * rimW + 0.5));
        // A touch of light on the item's outermost pixels hides their jagged edge.
        float bleed = (1.0 - smoothstep(0.4, 1.6, dIn)) * 0.45;
        rim = mix(rim, bleed, cover);
        core = mix(core, bleed, cover);
    }

    // Halo: the blurred coverage turned back into a distance from the outline (a straight edge blurred by a
    // gaussian of sigma gives h = 1 / (1 + exp(1.702 d / sigma)), very closely), faded over the halo's radius by
    // the falloff, then shaped by the curve. Thin parts blur fainter, so their halo is a little smaller.
    float t = clamp(h * 2.0, 0.0, 1.0);
    float prof = 0.0;
    if (h > 0.002 && uHaloSize.x > 0.0) {
        float d = h >= 0.5 ? 0.0 : uHaloSize.y * 0.5875 * log(1.0 / h - 1.0);
        float u = d / uHaloSize.x;
        if (uHaloP.y < 0.5) {
            prof = 1.0 - u;
        } else if (uHaloP.y < 1.5) {
            prof = 1.0 - smoothstep(0.0, 1.0, u);
        } else {
            prof = exp(-3.0 * u) * (1.0 - smoothstep(1.0, 1.6, u));
        }
        prof = pow(clamp(prof, 0.0, 1.0), uHaloP.z);
    }

    // Shimmer: a highlight sweeping along the item from one end to the other, lighting up the rim where it passes.
    float shine = 0.0;
    if (uShimmer.z > 0.0) {
        float along = dot(px - center, axis) / len + 0.5;
        float x = (along - uShimmer.x) / uShimmer.y;
        shine = exp(-x * x) * uShimmer.z;
    }

    // Only the part of the trail that lags behind the glow as it is now.
    float lag = max(trail - now, 0.0);

    float g = gradientAt(px, uHaloShape > 0.5 ? 1.0 - t : clamp(edge / uReach, 0.0, 1.0), center, len);
    vec4 glow4 = lut(g, 0.25);
    vec4 rim4 = lut(g, 0.75);
    vec4 trail4 = uTrailP.z > 0.5 ? lut(1.0 - fresh, 0.25) : glow4;
    vec3 glowC = glow4.rgb;
    vec3 rimC = mix(rim4.rgb, vec3(1.0), clamp(uRim.w * core * 0.9 + uLook.y * 0.7 + shine * 0.8, 0.0, 1.0));
    vec3 haloC = mix(glowC, vec3(1.0), uLook.y * 0.45);
    vec3 trailC = trail4.rgb;

    // How much of each part; a color's own alpha fades its part.
    float outside = 1.0 - cover;
    float gain = uLook.x;
    float rimI = rim * uRim.z * (1.0 + shine * 1.5) * rim4.a;
    float haloI = prof * uHaloP.x * flow * outside * glow4.a;
    float innerI = cover * (uInside.x * (0.3 + 0.7 * (1.0 - smoothstep(0.5, 1.0, h))) * flow + uInside.y * 0.35) * glow4.a;
    float trailI = lag * uTrailP.x * (outside + cover * uInside.x) * mix(1.0, flow, 0.6) * trail4.a;
    float opacity = uLook.z;

    if (uBlend == 2) {
        // Alpha: the glow's colors painted over the scene with its strength as opacity (dark colors work).
        float a1 = clamp(rimI * gain, 0.0, 1.0);
        float a2 = clamp(haloI * gain, 0.0, 1.0);
        float a3 = clamp(innerI * gain, 0.0, 1.0);
        float a4 = clamp(trailI * gain, 0.0, 1.0);
        float a = 1.0 - (1.0 - a1) * (1.0 - a2) * (1.0 - a3) * (1.0 - a4);
        vec3 col = (rimC * a1 + haloC * a2 + glowC * a3 + trailC * a4) / (a1 + a2 + a3 + a4 + 1.0e-5);
        a *= opacity;
        gl_FragColor = vec4(col * a, a);
        return;
    }
    vec3 rgb = (rimC * rimI + haloC * haloI + glowC * innerI + trailC * trailI) * gain * opacity;
    if (uBlend == 1) {
        // Screen: 1 - (1 - scene)(1 - glow), blended with ONE, ONE_MINUS_SRC_COLOR.
        gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 0.0);
        return;
    }
    // Additive light; the rim also covers a little of what is behind it, so it keeps its color over bright skies.
    float body = clamp(rimI * gain * 0.55, 0.0, 0.6) * outside * opacity;
    gl_FragColor = vec4(rgb, body);
}
