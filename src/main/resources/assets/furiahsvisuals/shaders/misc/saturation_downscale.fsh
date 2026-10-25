#version 120
uniform sampler2D uScene;
uniform vec2 uSize;
// a quarter of a target pixel, in texture coordinates
uniform vec2 uTexel;
void main() {
    vec2 uv = gl_FragCoord.xy / uSize;
    vec3 c = texture2D(uScene, uv + vec2(-uTexel.x, -uTexel.y)).rgb + texture2D(uScene, uv + vec2(uTexel.x, -uTexel.y)).rgb
           + texture2D(uScene, uv + vec2(-uTexel.x, uTexel.y)).rgb + texture2D(uScene, uv + uTexel).rgb;
    gl_FragColor = vec4(c * 0.25, 1.0);
}
