#version 120
uniform sampler2D uScene;
uniform vec2 uSize;
// x saturation, y contrast, z vibrance, w amount
uniform vec4 uGrade;
uniform float uDither;
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
void main() {
    vec4 src = texture2D(uScene, gl_FragCoord.xy / uSize);
    vec3 c0 = src.rgb;
    vec3 c = (c0 - 0.5) * uGrade.y + 0.5;
    float s = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
    c = mix(vec3(dot(c, LUMA)), c, 1.0 + uGrade.z * (1.0 - clamp(s, 0.0, 1.0)));
    c = mix(vec3(dot(c, LUMA)), c, uGrade.x);
    c = mix(c0, c, uGrade.w);
    float n = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    c += (n - 0.5) / 255.0 * uDither;
    // The frame's own alpha back (written whole: no masked write that would read the frame first).
    gl_FragColor = vec4(clamp(c, 0.0, 1.0), src.a);
}
