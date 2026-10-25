#version 120
// Furiah's Visuals, Weapons: one direction of the glow buffer's gaussian blur (the bloom around glowing parts): nine
// texels' worth in five bilinear samples.

uniform sampler2D uTex;
uniform vec2 uStep;

varying vec2 vUv;

void main() {
    vec4 c = texture2D(uTex, vUv) * 0.2270270270;
    c += (texture2D(uTex, vUv + uStep * 1.3846153846) + texture2D(uTex, vUv - uStep * 1.3846153846)) * 0.3162162162;
    c += (texture2D(uTex, vUv + uStep * 3.2307692308) + texture2D(uTex, vUv - uStep * 3.2307692308)) * 0.0702702703;
    gl_FragColor = c;
}
