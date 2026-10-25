#version 120
// Furiah's Visuals, Weapons: puts the first-person view model's supersampled buffer back on the screen. The quad's
// corners come in normalized device coordinates.

varying vec2 vUv;

void main() {
    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);
    vUv = gl_MultiTexCoord0.xy;
}
