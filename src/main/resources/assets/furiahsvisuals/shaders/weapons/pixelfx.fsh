#version 120
// Furiah's Visuals, Weapons: Pixel Effects. The first-person view model's effects (swing trails, sparks, smoke, motes,
// mist, glows) are drawn into a small buffer whose pixels are about the size of the weapon's pixels on screen; here
// that buffer goes onto the screen with nearest filtering (each of its texels a hard square) and every texel's
// brightness stepped to four levels, so the effects read as pixel art beside the weapon. Premultiplied, like the rest
// of the view model.

uniform sampler2D uTex;
// Brightness levels (4).
uniform float uLevels;

varying vec2 vUv;

void main() {
    vec4 c = texture2D(uTex, vUv);
    float m = max(max(c.r, c.g), max(c.b, c.a));
    if (m < 0.5 / uLevels) {
        discard;
    }
    // The texel's strength to the nearest of the levels (never down to nothing: it was over half a step).
    float q = max(1.0, floor(m * uLevels + 0.5)) / uLevels;
    gl_FragColor = c * (q / m);
}
