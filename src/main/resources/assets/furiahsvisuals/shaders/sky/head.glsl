// Furiah's Visuals, Dynamic Skies: uniforms and helpers shared by every style.
// A program is: prelude (skyDir()) + head.glsl + styles/<style>.glsl (skyColor()) + tail.glsl (main()).
// Written for GLSL 1.20 and GLSL ES 1.00 alike: constant loop bounds, no integer math, no arrays but uniforms.

uniform sampler2D uNoise;   // 256x256 value-noise texture (see SkyShaders.noiseTexture), unit 0
uniform float uTime;        // animation clock in seconds, already multiplied by the speed setting
uniform float uDetail;      // fractal octaves for the quality setting, 3..6
uniform float uScale;       // pattern size multiplier (1 = the style's own size)
uniform vec3 uColA;         // main color of the style (or the user's)
uniform vec3 uColB;         // second color
uniform vec3 uParallax;     // near-layer offset from the player's movement
uniform float uClouds;      // 1 = the style may draw its own clouds
uniform float uSunOwn;      // 1 = the style may draw its own sun or moon
uniform float uRain;        // rain strength 0..1
uniform float uFlash;       // lightning flash 0..1
uniform vec3 uFlashDir;     // where the flash is
uniform float uStars;       // star density 0..1 (0 = no stars)
uniform float uTwinkle;     // 0..1
uniform vec3 uStarCol;
uniform vec4 uMeteorA[4];   // shooting stars: head direction, brightness
uniform vec4 uMeteorB[4];   //                 direction of travel, tail length (radians)
uniform float uBright;      // brightness
uniform float uSat;         // saturation
uniform float uAlpha;       // opacity (overlay mode)
uniform vec2 uSpin;         // rotation of the whole sky around the vertical: (cos, sin) of its angle
uniform float uCustom;      // 1 = the user picked the colors (styles with a fixed look can follow them)
uniform sampler2D uImage;   // Custom Image style: the panorama, unit 0
uniform float uHasImage;    // 1 = a panorama is loaded
uniform vec4 uOptA;         // a Dynamic Skies sky's own options (SkyModule.optionsOf): eight values in two vectors
uniform vec4 uOptB;
uniform float uBeat;        // React to Music: 0..1, up on the beat
uniform vec3 uLook;         // where the camera looks (world directions, y up, north -Z)
uniform float uContrast;    // contrast around a dark grey (1 = unchanged)

// Styles may lower this inside skyColor() where stars must not show (a black hole, the sun's corona, ...).
float gStarMask = 1.0;
// Styles that react to the music in their own way (some parts flare, others speed up) set this to 1 inside skyColor():
// the whole sky then doesn't pulse brighter on the beat as well.
float gBeatOwn = 0.0;

const float PI = 3.14159265;
const float TAU = 6.28318531;
// Rotation between fractal octaves, so their grids never line up.
const mat3 OCT = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);

// ---- Hashes ("hash without sine" style: stable on every GPU) ----

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

// ---- Noise ----

// Two independent smooth value noises (-1..1) from one texture fetch. The texture's green (alpha) channel holds
// red (blue) shifted by (37, 17) texels, so one bilinear fetch covers two neighbouring z slices.
vec2 noise2(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xy + vec2(37.0, 17.0) * p.z + f.xy;
    vec4 t = texture2D(uNoise, (uv + 0.5) / 256.0);
    return mix(t.xz, t.yw, f.z) * 2.0 - 1.0;
}

float noise(vec3 x) {
    return noise2(x).x;
}

// Fractal noise with as many octaves as the quality setting allows (about -1..1).
float fbm(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= uDetail) {
            break;
        }
        s += a * noise(p);
        p = OCT * p * 2.03;
        a *= 0.5;
    }
    return s;
}

vec2 fbm2(vec3 p) {
    vec2 s = vec2(0.0);
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= uDetail) {
            break;
        }
        s += a * noise2(p);
        p = OCT * p * 2.03;
        a *= 0.5;
    }
    return s;
}

// Fractal noise with a fixed octave count (for cheap secondary layers).
float fbmN(vec3 p, float octaves) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= octaves) {
            break;
        }
        s += a * noise(p);
        p = OCT * p * 2.03;
        a *= 0.5;
    }
    return s;
}

vec2 fbm2N(vec3 p, float octaves) {
    vec2 s = vec2(0.0);
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= octaves) {
            break;
        }
        s += a * noise2(p);
        p = OCT * p * 2.03;
        a *= 0.5;
    }
    return s;
}

// Ridged noise: sharp creases where the noise crosses zero (lightning, veins, cloud edges).
float ridge(vec3 p, float octaves) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= octaves) {
            break;
        }
        float n = 1.0 - abs(noise(p));
        s += a * n * n;
        p = OCT * p * 2.03;
        a *= 0.5;
    }
    return s;
}

// Marbling, like stirred paint: pushes p along a smooth flow in many small steps (long folded ribbons), then along
// a finer, faster flow (small curls on their edges). The quality setting changes the step count, not the look.
vec3 stir(vec3 p, float t, float big, float small) {
    float steps = 4.0 + uDetail;
    float kb = big * 8.0 / steps;
    float ks = small * 8.0 / steps;
    // The flows are looked up in a rotated frame, so the noise lattice never lines features up into straight cuts.
    for (int i = 0; i < 10; i++) {
        if (float(i) >= steps) {
            break;
        }
        vec2 n = noise2(OCT * p * 0.8 + vec3(0.0, 0.0, t + float(i) * 0.09));
        p += vec3(n.x, n.y * 0.6, -n.y) * kb;
    }
    for (int i = 0; i < 10; i++) {
        if (float(i) >= steps) {
            break;
        }
        vec2 n = noise2(OCT * p * 2.6 + vec3(4.0, 1.0, -t * 1.3));
        float a = n.x * 4.0;
        p += vec3(cos(a), sin(a) * 0.8, sin(a)) * ks;
    }
    return p;
}

// ---- Directions (y up; azimuth 0 = north (-Z), growing towards east (+X)) ----

float azimuth(vec3 d) {
    return atan(d.x, -d.z);
}

float elevation(vec3 d) {
    return asin(clamp(d.y, -1.0, 1.0));
}

vec3 dirFrom(float az, float el) {
    return vec3(sin(az) * cos(el), sin(el), -cos(az) * cos(el));
}

// Flat 2D coordinates on a plane touching the sky at `center` (x to the right, y up), in radians near the center.
vec2 tangentCoords(vec3 d, vec3 center) {
    vec3 right = normalize(cross(center, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, center);
    float k = max(dot(d, center), 1e-4);
    return vec2(dot(d, right), dot(d, up)) / k;
}

// ---- Color helpers ----

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 hsv(float h, float s, float v) {
    vec3 k = clamp(abs(fract(h + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return v * mix(vec3(1.0), k, s);
}

// A bright, near-white core for a glowing color (hot light is always paler than its glow).
vec3 hot(vec3 c) {
    return mix(c, vec3(1.0), 0.65);
}

// Soft glow falloff for a distance d (in the same units as width).
float glow(float d, float width) {
    return width / (width + d * d * 30.0);
}

// ---- Sky objects ----

// Angle between the view direction and a direction s, in radians.
float angleTo(vec3 d, vec3 s) {
    return acos(clamp(dot(d, s), -1.0, 1.0));
}

// A round disk of `radius` radians around s, anti-aliased.
float disk(vec3 d, vec3 s, float radius) {
    float a = angleTo(d, s);
    float aa = max(fwidth(a), 1e-4);
    return 1.0 - smoothstep(radius - aa, radius + aa, a);
}

// Where the view ray meets a flat cloud deck `height` above the viewer (2D coordinates on the deck).
vec2 deck(vec3 d, float height) {
    return d.xz * (height / max(d.y, 0.015));
}

// Vertical sky gradient from the horizon color up to the zenith color.
vec3 gradient(vec3 d, vec3 horizon, vec3 zenith, float power) {
    return mix(horizon, zenith, pow(clamp(d.y, 0.0, 1.0), power));
}

// The inverse of tangentCoords(): the direction at 2D point uv on the plane touching the sky at `center`.
vec3 fromTangent(vec2 uv, vec3 center) {
    vec3 right = normalize(cross(center, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, center);
    return normalize(center + right * uv.x + up * uv.y);
}

// Two directions at right angles to d (a small screen-like frame for sprites drawn in the sky).
void frameOf(vec3 d, out vec3 right, out vec3 up) {
    vec3 ref = abs(d.y) > 0.95 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
    right = normalize(cross(ref, d));
    up = cross(d, right);
}

// ---- Dynamic Skies helpers ----

// Which face of a cube around the viewer a direction goes through (0..5: +X -X +Y -Y +Z -Z, in z) and where on it
// (x, y from -1 to 1). Flat patterns laid on the faces never pinch at the zenith (it is the middle of the top face).
vec3 cubeFace(vec3 d) {
    vec3 a = abs(d);
    if (a.x >= a.y && a.x >= a.z) {
        return d.x > 0.0 ? vec3(vec2(-d.z, d.y) / a.x, 0.0) : vec3(vec2(d.z, d.y) / a.x, 1.0);
    }
    if (a.y >= a.z) {
        return d.y > 0.0 ? vec3(vec2(d.x, -d.z) / a.y, 2.0) : vec3(vec2(d.x, d.z) / a.y, 3.0);
    }
    return d.z > 0.0 ? vec3(vec2(d.x, d.y) / a.z, 4.0) : vec3(vec2(-d.x, d.y) / a.z, 5.0);
}

// The direction through point uv of cube face `face` (the inverse of cubeFace).
vec3 cubeDir(vec2 uv, float face) {
    vec3 d;
    if (face < 0.5) {
        d = vec3(1.0, uv.y, -uv.x);
    } else if (face < 1.5) {
        d = vec3(-1.0, uv.y, uv.x);
    } else if (face < 2.5) {
        d = vec3(uv.x, 1.0, -uv.y);
    } else if (face < 3.5) {
        d = vec3(uv.x, -1.0, uv.y);
    } else if (face < 4.5) {
        d = vec3(uv.x, uv.y, 1.0);
    } else {
        d = vec3(-uv.x, uv.y, -1.0);
    }
    return normalize(d);
}

// Distance from p to the segment a..b.
float segDist(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-8), 0.0, 1.0);
    return length(pa - ba * h);
}

// Coverage of a line `halfWidth` wide at distance `dist` from it (same units), anti-aliased over a pixel: sharp at any
// resolution, and never thinner than a pixel's worth of light.
float lineAA(float dist, float halfWidth) {
    float aa = fwidth(dist) * 0.75 + 1e-6;
    return (1.0 - smoothstep(halfWidth - aa, halfWidth + aa, dist)) * min(1.0, halfWidth / aa);
}

// d turned by angle a around the unit axis (Rodrigues).
vec3 rotAxis(vec3 d, vec3 axis, float a) {
    float c = cos(a);
    float s = sin(a);
    return d * c + cross(axis, d) * s + axis * dot(axis, d) * (1.0 - c);
}

// The Dynamic Skies' horizon: the sky melts into `hz` (the fog takes the same color, see SkyStyle.horizonColor), and
// below the horizon it fades to a dark version of itself.
vec3 horizon(vec3 col, vec3 d, vec3 hz) {
    col = mix(col, hz, (1.0 - smoothstep(0.0, 0.14, abs(d.y))) * 0.6);
    return mix(col, hz * 0.4 + col * 0.12, smoothstep(0.0, -0.22, d.y));
}

// One layer of stars: a grid on each face of a cube around the viewer, at most one star per cell.
vec3 starLayer(vec3 d, float cells, float density, float size, float seed) {
    vec3 a = abs(d);
    vec2 uv;
    float face;
    if (a.x >= a.y && a.x >= a.z) {
        uv = d.zy / a.x;
        face = d.x > 0.0 ? 1.0 : 2.0;
    } else if (a.y >= a.z) {
        uv = d.xz / a.y;
        face = d.y > 0.0 ? 3.0 : 4.0;
    } else {
        uv = d.xy / a.z;
        face = d.z > 0.0 ? 5.0 : 6.0;
    }
    vec2 g = uv * cells;
    vec2 id = floor(g);
    vec3 h = hash33(vec3(id, face * 17.0 + seed));
    if (h.x > density) {
        return vec3(0.0);
    }
    // Keep stars away from cell borders, so none is ever cut in half.
    vec2 pos = id + 0.2 + 0.6 * h.yz;
    // Size in cells, but never below about a pixel (fwidth), so small stars do not flicker.
    float px = min(fwidth(g.x) + fwidth(g.y), 0.25);
    float r = max(size, px * 0.7);
    float dist = length(g - pos);
    float core = exp(-dist * dist / (r * r));
    float mag = pow(h.y, 3.0) * 0.8 + 0.2;
    float tw = 1.0 - uTwinkle * 0.55 * (0.5 + 0.5 * sin(uTime * (1.5 + 3.0 * h.z) + h.x * 40.0));
    // Slight color temperature per star.
    vec3 tint = mix(vec3(0.75, 0.85, 1.0), vec3(1.0, 0.9, 0.78), h.z);
    return uStarCol * tint * core * mag * tw * (size / r);
}
