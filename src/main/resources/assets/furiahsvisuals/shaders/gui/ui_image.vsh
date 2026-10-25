#version 120
uniform vec2 uCenter;  // the box's center, units
uniform float uPx;  // pixels per unit
varying vec2 vLocal;
varying vec2 vUv;
void main() {
    gl_Position = ftransform();
    gl_FrontColor = gl_Color;
    vUv = gl_MultiTexCoord0.xy;
    vLocal = (gl_Vertex.xy - uCenter) * uPx;
}
