#version 120
// Distance field, step 2 of 2 (columns): combines the rows above and below into the true, round (Euclidean)
// distance to the silhouette. r = distance outside / 32, g = distance inside / 8 (mask pixels, pixel centers).
// This is what lets the rim follow every spike, notch and gap with a smooth, even width.
uniform sampler2D uRows;    // step 1
uniform sampler2D uNear;
uniform vec2 uTexel;
uniform float uRadius;
varying vec2 vUv;

void main() {
    if (texture2D(uNear, vUv).g <= 0.0) {
        gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        return;
    }
    vec2 own = texture2D(uRows, vUv).rg;
    bool inside = own.g > 0.0;
    float scale = inside ? 8.0 : 32.0;
    float g0 = (inside ? own.g : own.r) * scale;
    float best = g0 * g0;
    float reach = inside ? min(uRadius, 8.0) : uRadius;
    for (int i = 1; i <= 32; i++) {
        float fi = float(i);
        if (fi > reach || fi * fi >= best) {
            break;
        }
        vec2 up = texture2D(uRows, vUv + vec2(0.0, uTexel.y * fi)).rg;
        vec2 dn = texture2D(uRows, vUv - vec2(0.0, uTexel.y * fi)).rg;
        float gu = (inside ? up.g : up.r) * scale;
        float gd = (inside ? dn.g : dn.r) * scale;
        best = min(best, min(gu * gu, gd * gd) + fi * fi);
    }
    float d = sqrt(best);
    gl_FragColor = inside ? vec4(0.0, min(d, 8.0) / 8.0, 0.0, 1.0) : vec4(min(d, 32.0) / 32.0, 0.0, 0.0, 1.0);
}
