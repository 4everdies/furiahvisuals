#version 120
// Furiah's Visuals, Weapons: an energy blade's glow and a shock baton's arcs where the weapon is drawn in the world's
// own matrices (third person, dropped items, frames), turned to the camera here, so the modelview is never read back.
// Each vertex is a point on the line (model space) with the line's direction there in its normal; its texture
// coordinates move it across the line (x, square to the line of sight from the eyes) and along it (y), in model units.

varying float vAcross;

void main() {
    vec4 e = gl_ModelViewMatrix * gl_Vertex;
    vec3 d = mat3(gl_ModelViewMatrix) * gl_Normal;
    float scale = length(d) / max(length(gl_Normal), 1e-6);
    d = scale > 1e-6 ? normalize(d) : vec3(0.0, 1.0, 0.0);
    vec3 side = cross(d, e.xyz);
    float l = length(side);
    side = l > 1e-6 ? side / l : vec3(1.0, 0.0, 0.0);
    e.xyz += (side * gl_MultiTexCoord0.x + d * gl_MultiTexCoord0.y) * scale;
    gl_Position = gl_ProjectionMatrix * e;
    gl_FrontColor = gl_Color;
    // Across a textured strip: 0 on one edge, 1 on the other (its texture's V).
    vAcross = gl_MultiTexCoord0.x < 0.0 ? 0.0 : 1.0;
}
