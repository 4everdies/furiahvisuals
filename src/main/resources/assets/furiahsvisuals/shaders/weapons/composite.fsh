#version 120
// Furiah's Visuals, Weapons: the supersampled view model, averaged down to the screen's pixels. At 2x a pixel covers
// 2x2 texels and one bilinear sample at their shared corner averages them; at 4x it covers 4x4, and four bilinear
// samples average four 2x2 blocks. The buffer is premultiplied, so its edges blend without dark fringes. Glowing parts
// add a soft bloom from the small blurred glow buffer (uBloom), which reaches past the model's edges. Icons get a dark
// outline around their shape (uOutline, in texels of the buffer), like vanilla's item sprites, so they read on any slot.

uniform sampler2D uTex;
uniform sampler2D uBloomTex;
uniform vec2 uTexel;
uniform float uTaps;
uniform float uBloom;
uniform vec4 uBloomRect;
uniform float uOutline;
// A weapon changing style (Pixelated's Switch Animation): blocks this many texels wide take their middle's color
// (0: none); uUsed: the part of the buffer in use (its texture coordinates' far corner).
uniform float uMosaic;
uniform vec2 uUsed;

varying vec2 vUv;

void main() {
    vec4 c;
    vec2 uv = vUv;
    if (uMosaic > 1.0) {
        vec2 cell = uTexel * uMosaic;
        uv = min((floor(uv / cell) + 0.5) * cell, uUsed - uTexel);
    }
    if (uTaps < 1.5) {
        c = texture2D(uTex, uv);
    } else {
        vec2 o = uTexel;
        c = 0.25 * (texture2D(uTex, uv + vec2(-o.x, -o.y)) + texture2D(uTex, uv + vec2(o.x, -o.y))
                + texture2D(uTex, uv + vec2(-o.x, o.y)) + texture2D(uTex, uv + vec2(o.x, o.y)));
    }
    if (uOutline > 0.0) {
        float a = 0.0;
        for (int i = 0; i < 12; i++) {
            float ang = float(i) * 0.5236;
            a = max(a, texture2D(uTex, vUv + vec2(cos(ang), sin(ang)) * uTexel * uOutline).a);
        }
        // Under the shape: a dark rim where the shape's neighbours are.
        vec4 rim = vec4(0.03, 0.03, 0.04, 1.0) * a * 0.9;
        c = c + rim * (1.0 - c.a);
    }
    if (uBloom > 0.0) {
        vec2 b = uBloomRect.xy + vUv * uBloomRect.zw;
        vec3 glow = texture2D(uBloomTex, b).rgb * uBloom;
        // Added light: it brightens what is under it without covering it.
        c.rgb += glow * (1.0 - c.a * 0.5);
    }
    gl_FragColor = c;
}
