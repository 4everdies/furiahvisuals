#version 120
varying vec3 vDir;
vec3 skyDir() {
    return normalize(vDir);
}
