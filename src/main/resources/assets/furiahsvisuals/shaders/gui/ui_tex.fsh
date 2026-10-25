#version 120
uniform sampler2D uTex;
varying vec2 vLocal;  // the texture coordinates (same vertex shader as the shapes)
#include "gui/ui_fade.glsl"
void main() {
    vec4 c = texture2D(uTex, vLocal) * gl_Color;
    gl_FragColor = vec4(c.rgb, c.a * fadeMask());
}
