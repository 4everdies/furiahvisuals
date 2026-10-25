#version 120
// Furiah's Visuals, Weapons: the Pixelated weapons' vertex shader. Each face is lit like vanilla's held items: its two
// fixed item lights (uL0, uL1, in view space, as vanilla sets them where the weapon is drawn) over an ambient 0.4, or
// flat (uFlat). The palette slot and flags come in the texture coordinates; the model's own coordinates give the pixel
// a fragment is in.

uniform vec3 uL0;
uniform vec3 uL1;
uniform float uFlat;
// A blade flexing (the Rapier's Flex) bends toward its edge as in weapon.vsh: x += uBend (y - uBendFrom)^2. The pixel a
// fragment is in still comes from the unbent model, so the pixels ride along the bend instead of sliding through it.
uniform float uBend;
uniform float uBendFrom;

varying vec3 vObj;
varying vec3 vNObj;
varying vec3 vNEye;
varying vec3 vPos;
varying float vShade;
varying vec2 vData;

void main() {
    vec4 vertex = gl_Vertex;
    vec3 normal = gl_Normal;
    if (uBend != 0.0) {
        float d = max(vertex.y - uBendFrom, 0.0);
        vertex.x += uBend * d * d;
        normal.y -= 2.0 * uBend * d * normal.x;
    }
    vec4 p = gl_ModelViewMatrix * vertex;
    vObj = gl_Vertex.xyz;
    vNObj = gl_Normal;
    vec3 n = normalize(gl_NormalMatrix * normalize(normal));
    vNEye = n;
    vPos = p.xyz;
    float lit = 0.4 + 0.6 * (max(0.0, dot(n, uL0)) + max(0.0, dot(n, uL1)));
    vShade = uFlat > 0.5 ? 1.0 : min(1.0, lit);
    vData = gl_MultiTexCoord0.xy;
    gl_FogFragCoord = length(p.xyz);
    gl_Position = gl_ProjectionMatrix * p;
}
