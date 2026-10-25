#version 120
varying vec2 vLocal;
void main() {
    gl_Position = ftransform();
    vLocal = gl_MultiTexCoord0.xy;
}
