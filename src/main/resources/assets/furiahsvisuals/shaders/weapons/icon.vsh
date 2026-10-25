#version 120
// Furiah's Visuals, Weapons: a hotbar or inventory icon drawn from the icon atlas (the GUI's own matrices).

varying vec2 vUv;

void main() {
    gl_Position = ftransform();
    vUv = gl_MultiTexCoord0.xy;
}
