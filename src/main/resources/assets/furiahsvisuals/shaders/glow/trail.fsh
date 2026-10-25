#version 120
// The trail: an accumulation of glow only. Each frame: last frame's trail, faded by exp(-frameTime / tau) (and for
// Smoke spread and swollen a little), then max() with this frame's glow shape. Values are stored as square roots,
// so the faint end keeps enough precision in 8 bits to fade out smoothly instead of getting stuck.
// r = sqrt(strength); g = sqrt(freshness): 1 where the glow is now, fading like the trail (Color by age).
uniform sampler2D uPrev;
uniform sampler2D uCover;   // coverage (r) near the trail's resolution
uniform sampler2D uHalo;    // the halo's blurred coverage (r)
uniform vec2 uTexel;        // one trail texel, uv
uniform float uFade;        // how much of last frame's trail is left this frame
uniform float uCapture;     // 1: this frame's glow is written into the trail
uniform float uSpread;      // smoke: tap distance, trail texels (0: no smoke step this frame)
uniform float uGrow;        // smoke: how much it swells
uniform float uEcho;        // 1: Echo (crisp outline ghosts), 0: Smoke (soft filled smears)
uniform vec2 uMotion;       // Smoke: how far the hand moved on screen since the last frame (uv, pointing back)
varying vec2 vUv;

// The glow's shape as the trail keeps it. Smoke: a soft, even veil over the item and its halo, so the copies of
// consecutive frames overlap into one smear. Echo: the outline and its halo, with only a faint veil across the
// body, so every copy reads as a ghost of the item. (composite.fsh uses the same shape for the glow as it is now.)
float trailShape(float cover, float h) {
    if (uEcho > 0.5) {
        return max(cover * 0.3, (1.0 - abs(h * 2.0 - 1.0)) * 0.9);
    }
    return max(cover * 0.45, clamp(h * 2.0, 0.0, 1.0) * 0.5);
}

vec2 prev(vec2 uv) {
    vec2 v = texture2D(uPrev, uv).rg;
    return v * v;
}

void main() {
    vec2 c = prev(vUv);
    if (uSpread > 0.0) {
        vec2 o = uTexel * uSpread * 0.7071;
        vec2 a = prev(vUv + vec2(o.x, o.y));
        vec2 b = prev(vUv + vec2(-o.x, o.y));
        vec2 d = prev(vUv + vec2(o.x, -o.y));
        vec2 e = prev(vUv + vec2(-o.x, -o.y));
        vec2 soft = c * 0.36 + (a + b + d + e) * 0.16;
        vec2 swell = max(max(a, b), max(d, e)) * 0.92;
        c = mix(soft, max(soft, swell), uGrow);
    }
    c *= uFade;
    if (c.r < 0.003) {
        c = vec2(0.0);
    }
    if (uCapture > 0.0) {
        float now = trailShape(texture2D(uCover, vUv).r, texture2D(uHalo, vUv).r);
        // Smoke: copies of this frame's shape back along the way the hand came since the last frame, so a fast
        // swing leaves one continuous smear instead of separate copies (at any frame rate).
        if (dot(uMotion, uMotion) > 1.0e-8) {
            for (int k = 1; k <= 4; k++) {
                vec2 uv = vUv - uMotion * (float(k) * 0.25);
                now = max(now, trailShape(texture2D(uCover, uv).r, texture2D(uHalo, uv).r) * (1.0 - 0.08 * float(k)));
            }
        }
        c.r = max(c.r, now);
        c.g = max(c.g, smoothstep(0.02, 0.1, now));
    }
    gl_FragColor = vec4(sqrt(c), 0.0, 1.0);
}
