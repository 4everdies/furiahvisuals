#version 120
// Furiah's Visuals, Weapons: the 3D weapons' (and the first-person arms') vertex shader. View space for the lighting,
// the model's own coordinates for its procedural patterns, and the direction of the model's +Y axis (along the blade)
// for brushed metal.
//
// A flexing blade (uBend) bends toward its edge, as x += uBend (y - uBendFrom)^2.
//
// A saw chain (uChain): each link is built in its own frame (x along the chain, y out from the bar, z across) and its
// place along the chain in its texture's u; here it is set on the chain's path round the bar (a straight run under the
// bar, round the nose, back along the top and round the drive sprocket), uChainPhase blocks further along, so the
// chain runs.

uniform float uChain;
uniform vec4 uChainPath;
uniform float uChainPhase;
// A blade flexing (the Rapier's Flex): it bends toward its edge, more the further along it is from uBendFrom.
uniform float uBend;
uniform float uBendFrom;
// A bow's limbs (uBowBend): both ways along y past uBowFrom from the grip, each point moves back toward the string
// (-x) by uBowBend d^2 and in toward the grip by about what keeps the limb's length; its patterns stay where they were.
uniform float uBowBend;
uniform float uBowFrom;

varying vec3 vPos;
varying vec3 vNormal;
varying vec2 vUv;
varying vec3 vObj;
varying vec3 vAxis;

// A point of the chain's path, s blocks along it, with its tangent and outward normal.
vec3 chainPoint(float s, out vec3 tang, out vec3 nrm) {
    float x0 = uChainPath.x;
    float y0 = uChainPath.y;
    float y1 = uChainPath.z;
    float r = uChainPath.w;
    float d = y1 - y0;
    float half = 3.14159265 * r;
    float len = 2.0 * d + 2.0 * half;
    s = mod(s, len);
    if (s < d) {
        tang = vec3(0.0, 1.0, 0.0);
        nrm = vec3(1.0, 0.0, 0.0);
        return vec3(x0 + r, y0 + s, 0.0);
    }
    s -= d;
    if (s < half) {
        float a = s / r;
        tang = vec3(-sin(a), cos(a), 0.0);
        nrm = vec3(cos(a), sin(a), 0.0);
        return vec3(x0 + r * cos(a), y1 + r * sin(a), 0.0);
    }
    s -= half;
    if (s < d) {
        tang = vec3(0.0, -1.0, 0.0);
        nrm = vec3(-1.0, 0.0, 0.0);
        return vec3(x0 - r, y1 - s, 0.0);
    }
    s -= d;
    float a = s / r;
    tang = vec3(sin(a), -cos(a), 0.0);
    nrm = vec3(-cos(a), -sin(a), 0.0);
    return vec3(x0 - r * cos(a), y0 - r * sin(a), 0.0);
}

void main() {
    vec4 vertex = gl_Vertex;
    vec3 normal = gl_Normal;
    if (uChain > 0.5) {
        float len = 2.0 * (uChainPath.z - uChainPath.y) + 6.28318531 * uChainPath.w;
        vec3 t;
        vec3 o;
        vec3 c = chainPoint(gl_MultiTexCoord0.x * len + uChainPhase, t, o);
        vertex.xyz = c + t * gl_Vertex.x + o * gl_Vertex.y + vec3(0.0, 0.0, gl_Vertex.z);
        normal = t * gl_Normal.x + o * gl_Normal.y + vec3(0.0, 0.0, gl_Normal.z);
    }
    if (uBend != 0.0) {
        // x += k d^2 along the blade; normals follow the slope (the inverse transpose of the bend).
        float d = max(vertex.y - uBendFrom, 0.0);
        vertex.x += uBend * d * d;
        normal.y -= 2.0 * uBend * d * normal.x;
    }
    vec3 obj = vertex.xyz;
    if (uBowBend != 0.0) {
        float s = vertex.y < 0.0 ? -1.0 : 1.0;
        float d = max(abs(vertex.y) - uBowFrom, 0.0);
        vertex.x -= uBowBend * d * d;
        vertex.y -= s * 0.6667 * uBowBend * uBowBend * d * d * d;
        normal.y += 2.0 * uBowBend * d * s * normal.x;
    }
    vec4 p = gl_ModelViewMatrix * vertex;
    vPos = p.xyz;
    vNormal = normalize(gl_NormalMatrix * normal);
    vUv = gl_MultiTexCoord0.xy;
    vObj = obj;
    vAxis = (gl_ModelViewMatrix * vec4(0.0, 1.0, 0.0, 0.0)).xyz;
    gl_FogFragCoord = length(p.xyz);
    gl_Position = gl_ProjectionMatrix * p;
}
