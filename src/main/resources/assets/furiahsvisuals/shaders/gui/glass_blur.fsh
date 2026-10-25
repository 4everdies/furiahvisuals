#version 120
uniform sampler2D uTex;
uniform vec2 uDir;
varying vec2 vUv;
void main() {
    vec3 c = texture2D(uTex, vUv).rgb * 0.2270270270;
    c += texture2D(uTex, vUv + uDir * 1.3846153846).rgb * 0.3162162162;
    c += texture2D(uTex, vUv - uDir * 1.3846153846).rgb * 0.3162162162;
    c += texture2D(uTex, vUv + uDir * 3.2307692308).rgb * 0.0702702703;
    c += texture2D(uTex, vUv - uDir * 3.2307692308).rgb * 0.0702702703;
    gl_FragColor = vec4(c, 1.0);
}
