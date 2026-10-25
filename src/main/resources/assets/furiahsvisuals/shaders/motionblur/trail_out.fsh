#version 120
uniform sampler2D uHist;
uniform vec2 uSize;
uniform float uLinear;
void main() {
    vec3 c = texture2D(uHist, gl_FragCoord.xy / uSize).rgb;
    if (uLinear > 0.5) {
        c = pow(max(c, vec3(0.0)), vec3(1.0 / 2.2));
    }
    gl_FragColor = vec4(c, 1.0);
}
