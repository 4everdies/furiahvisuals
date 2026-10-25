#version 120
uniform sampler2D uTex;
uniform vec2 uTexel;
uniform float uLevels;
uniform float uFill;
uniform float uInner;
varying vec2 vUv;
varying vec4 vColor;
float lum(vec4 c) {
    return dot(c.rgb, vec3(0.299, 0.587, 0.114)) * c.a;
}
void main() {
    vec2 d = max(uTexel * 1.6, fwidth(vUv) * 1.1);
    vec4 c = texture2D(uTex, vUv);
    vec4 a = texture2D(uTex, vUv + vec2(-d.x, d.y));
    vec4 b = texture2D(uTex, vUv + vec2(0.0, d.y));
    vec4 e = texture2D(uTex, vUv + vec2(d.x, d.y));
    vec4 l = texture2D(uTex, vUv + vec2(-d.x, 0.0));
    vec4 r = texture2D(uTex, vUv + vec2(d.x, 0.0));
    vec4 f = texture2D(uTex, vUv + vec2(-d.x, -d.y));
    vec4 g = texture2D(uTex, vUv + vec2(0.0, -d.y));
    vec4 h = texture2D(uTex, vUv + vec2(d.x, -d.y));
    // Sobel on the coverage (the outline) and on the brightness (the lines inside).
    float ax = (e.a + 2.0 * r.a + h.a) - (a.a + 2.0 * l.a + f.a);
    float ay = (a.a + 2.0 * b.a + e.a) - (f.a + 2.0 * g.a + h.a);
    float lx = (lum(e) + 2.0 * lum(r) + lum(h)) - (lum(a) + 2.0 * lum(l) + lum(f));
    float ly = (lum(a) + 2.0 * lum(b) + lum(e)) - (lum(f) + 2.0 * lum(g) + lum(h));
    float outline = clamp(length(vec2(ax, ay)) * 0.6, 0.0, 1.0);
    float inner = clamp((length(vec2(lx, ly)) - uInner) * 2.0, 0.0, 1.0);
    float line = max(outline, inner);
    // The tones: a few flat levels of the brightness, faint, inside the figure.
    float tone = floor(dot(c.rgb, vec3(0.299, 0.587, 0.114)) * uLevels + 0.5) / uLevels;
    float fill = tone * uFill * step(0.5, c.a);
    float k = max(line, fill);
    if (k < 0.004) discard;
    gl_FragColor = vec4(vColor.rgb, vColor.a * k);
}
