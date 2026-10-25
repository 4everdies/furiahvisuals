#version 120
varying vec3 vDir;
void main() {
    gl_Position = ftransform();
    vDir = gl_Vertex.xyz;
}
