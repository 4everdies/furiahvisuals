#version 120
// Distance field, step 1 of 2 (rows). For a pixel outside the silhouette: how far along its row the nearest
// covered pixel is (r = pixels / 32). For a pixel inside: how far the nearest uncovered one is (g = pixels / 8).
// Pixels with nothing covered anywhere near them (the flag level) are skipped: that is most of the screen.
uniform sampler2D uMask;
uniform sampler2D uNear;    // a small level whose g says "something is covered near here"
uniform vec2 uTexel;        // one mask pixel, uv
uniform float uRadius;      // how far to look, mask pixels (at most 32)
varying vec2 vUv;

void main() {
    // The small flag level first: where nothing is covered nearby (so this pixel is not covered either) one fetch
    // of a tiny texture is all it costs.
    if (texture2D(uNear, vUv).g <= 0.0) {
        gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        return;
    }
    bool inside = texture2D(uMask, vUv).r >= 0.5;
    float found = 32.0;
    for (int i = 1; i <= 32; i++) {
        float fi = float(i);
        if (fi > uRadius) {
            break;
        }
        float a = texture2D(uMask, vUv + vec2(uTexel.x * fi, 0.0)).r;
        float b = texture2D(uMask, vUv - vec2(uTexel.x * fi, 0.0)).r;
        if (inside ? min(a, b) < 0.5 : max(a, b) >= 0.5) {
            found = fi;
            break;
        }
    }
    gl_FragColor = inside ? vec4(0.0, min(found, 8.0) / 8.0, 0.0, 1.0) : vec4(found / 32.0, 0.0, 0.0, 1.0);
}
