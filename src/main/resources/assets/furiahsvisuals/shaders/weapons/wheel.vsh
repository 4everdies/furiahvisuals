#version 120
varying vec2 vLocal;
void main() {
    gl_Position = ftransform();
    gl_FrontColor = gl_Color;
    vLocal = gl_MultiTexCoord0.xy;
}
