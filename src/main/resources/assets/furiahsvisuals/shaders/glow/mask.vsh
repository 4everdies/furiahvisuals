#version 120
// The mask pass: Minecraft's own first-person renderer draws the view model again, with its matrices, texture
// matrix (enchantment glint) and vertex colors; only where it lands matters.
varying vec2 vUv;
varying float vAlpha;

void main() {
    gl_Position = ftransform();
    vUv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    vAlpha = gl_Color.a;
}
