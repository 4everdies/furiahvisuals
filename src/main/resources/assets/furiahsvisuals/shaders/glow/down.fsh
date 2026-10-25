#version 120
// Half resolution. Four bilinear taps average a 4x4 block of the source: r = coverage (0..1). g says whether any
// pixel under this one is covered at all; it keeps full strength on the way down, so even a one-pixel spike
// still counts several levels lower (the distance field uses it to skip pixels far from the view model).
uniform sampler2D uTex;
uniform vec2 uTexel;      // one source texel, uv
uniform float uFromMask;  // 1: the source is the mask itself, whose coverage is also its flag
varying vec2 vUv;

void main() {
    vec2 a = texture2D(uTex, vUv + uTexel * vec2(-1.0, -1.0)).rg;
    vec2 b = texture2D(uTex, vUv + uTexel * vec2(1.0, -1.0)).rg;
    vec2 c = texture2D(uTex, vUv + uTexel * vec2(-1.0, 1.0)).rg;
    vec2 d = texture2D(uTex, vUv + uTexel * vec2(1.0, 1.0)).rg;
    if (uFromMask > 0.5) {
        a.g = a.r;
        b.g = b.r;
        c.g = c.r;
        d.g = d.r;
    }
    float cover = (a.r + b.r + c.r + d.r) * 0.25;
    float any = min(1.0, max(max(a.g, b.g), max(c.g, d.g)) * 4.0);
    gl_FragColor = vec4(cover, any, 0.0, 1.0);
}
