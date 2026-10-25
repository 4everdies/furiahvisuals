uniform sampler2D uBg;  // captured world (sharp or blurred)
uniform vec4 uViewport;  // captured area: x, y, w, h in window pixels
uniform vec2 uUvScale;  // part of the texture that holds it
uniform float uAlpha;
uniform vec2 uHalf;  // half size of the shape, pixels
uniform float uRadius;  // corner radius, pixels
varying vec2 vLocal;  // pixel offset from the shape's center (y down)
float roundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}
vec2 screenUv() { return (gl_FragCoord.xy - uViewport.xy) / uViewport.zw; }
vec3 bg(vec2 uv) { return texture2D(uBg, clamp(uv, vec2(0.0), vec2(1.0)) * uUvScale).rgb; }
float dither() { return (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0; }
