#version 120
varying vec2 vUv;
void main() {
    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);
    vUv = gl_MultiTexCoord0.xy;
}
