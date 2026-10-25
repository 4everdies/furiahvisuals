#version 120
uniform sampler2D uBg;
uniform vec4 uViewport;  // captured region: x, y (from the bottom), w, h in window pixels
uniform vec2 uUvScale;  // the part of the blurred texture that holds it
uniform vec2 uUvMax;
uniform vec2 uHalf;  // half size of the pill, pixels
uniform float uRadius;
uniform vec4 uTint;  // tint, alpha = strength
uniform vec4 uRim;  // rim light, alpha = intensity
uniform float uAlpha;
uniform float uBevel;
uniform float uRefract;
uniform float uHair;
varying vec2 vLocal;
float roundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}
void main() {
    float d = roundBox(vLocal, uHalf, uRadius);
    float cover = clamp(0.5 - d, 0.0, 1.0);
    if (cover <= 0.0) discard;
    float inside = max(-d, 0.0);
    vec2 q = abs(vLocal) - uHalf + vec2(uRadius);
    vec2 s = vec2(vLocal.x < 0.0 ? -1.0 : 1.0, vLocal.y < 0.0 ? -1.0 : 1.0);
    vec2 n = (q.x > 0.0 && q.y > 0.0) ? normalize(q) * s : (q.x > q.y ? vec2(s.x, 0.0) : vec2(0.0, s.y));
    // A slight lens at the rim: the rounded ends gather the world from just outside.
    float edge = 1.0 - clamp(inside / uBevel, 0.0, 1.0);
    vec2 uv = (gl_FragCoord.xy - uViewport.xy) / uViewport.zw;
    uv += vec2(n.x, -n.y) * uRefract * edge * edge / uViewport.zw;
    vec3 col = texture2D(uBg, clamp(uv * uUvScale, vec2(0.0), uUvMax)).rgb;
    // Vibrancy, then the tint (stronger over bright areas, so text stays readable).
    float l = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col = max(mix(vec3(l), col, 1.2), 0.0);
    float tint = uTint.a * (1.0 + 0.6 * smoothstep(0.35, 1.0, l));
    col = mix(col, uTint.rgb, clamp(tint, 0.0, 0.94));
    // A lit hairline, brightest along the top, and a faint glow inside the rim.
    float hair = 1.0 - smoothstep(0.0, uHair * 1.6, inside);
    float top = max(-n.y, 0.0);
    col += uRim.rgb * uRim.a * (hair * (0.1 + 0.5 * top) + edge * edge * edge * 0.05);
    gl_FragColor = vec4(col, cover * uAlpha);
}
