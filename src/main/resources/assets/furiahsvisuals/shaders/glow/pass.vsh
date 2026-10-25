#version 120
// Full-screen pass: the quad is given in 0..1 and mapped straight to clip space (no matrices involved).
varying vec2 vUv;

void main() {
    gl_Position = vec4(gl_Vertex.xy * 2.0 - 1.0, 0.0, 1.0);
    vUv = gl_MultiTexCoord0.xy;
}
