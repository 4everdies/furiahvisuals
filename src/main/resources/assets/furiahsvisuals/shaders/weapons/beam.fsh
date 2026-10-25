#version 120
// Furiah's Visuals, Weapons: the glow's or arc's color, times the line texture's cross-section for a textured strip.

uniform sampler2D uTex;
uniform float uTextured;

varying float vAcross;

void main() {
    vec4 c = gl_Color;
    if (uTextured > 0.5) {
        c *= texture2D(uTex, vec2(0.5, vAcross));
    }
    gl_FragColor = c;
}
