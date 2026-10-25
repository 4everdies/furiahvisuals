#version 120
// Furiah's Visuals, Weapons: the 3D weapons' fragment shader (and the first-person arms'). One material per draw
// (uMat): the blade finishes, the hardware, the handle materials, glowing parts, the arms' skin.
//
// Lighting, as for Minecraft's held items: the light at the player's position (uLight, from the lightmap: dark in
// caves, bright in the sun, reddish in the Nether) scales everything. A key light from the upper left, a dim fill from
// the right and a rim from behind give the shape; metal reflects the sky above, the fog color at the horizon and the
// ground below (so a weapon matches the sky, Dynamic Skies included), plus two soft studio lights, so it always has
// something to catch. Output is premultiplied (the anti-aliasing buffer is composited with ONE, ONE_MINUS_SRC_ALPHA).

uniform sampler2D uAlbedo;
uniform sampler2D uMaterial;
uniform sampler2D uNormalMap;
uniform float uHasMaps;
uniform float uHasNormalMap;

uniform float uMat;
uniform vec3 uColA;
uniform vec3 uColB;
uniform float uRough;
uniform float uHandle;
uniform vec4 uRect;
uniform float uWear;
uniform float uGlow;
uniform float uHeat;
// The heat the weapon has at rest (0: it doesn't heat), and its core (xyz, model units) with the reach of the fire
// from it (w; 0: no core): an Infernal blade catches from the core outward as it heats up to its resting heat.
uniform float uHeatIdle;
uniform vec4 uCore;
uniform float uTime;
uniform float uSheen;
uniform vec3 uSheenColor;
uniform float uGlint;
// Energy parts lit up to this height along the model (runes waking one by one; far above: all lit), and how much
// brighter they flare (a gem's spin, a crystal's flare).
uniform float uReveal;
uniform float uFlare;
uniform float uAlpha;
uniform float uFog;
uniform float uSeed;

uniform vec3 uLight;
uniform vec3 uUp;
uniform vec3 uSunDir;
uniform float uSun;
uniform vec3 uSky;
uniform vec3 uHorizon;
uniform vec3 uGround;

varying vec3 vPos;
varying vec3 vNormal;
varying vec2 vUv;
varying vec3 vObj;
varying vec3 vAxis;

// ---------------------------------------------------------------------------------------------------------------------
// Noise
// ---------------------------------------------------------------------------------------------------------------------

float hash3(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float a = hash3(i);
    float b = hash3(i + vec3(1.0, 0.0, 0.0));
    float c = hash3(i + vec3(0.0, 1.0, 0.0));
    float d = hash3(i + vec3(1.0, 1.0, 0.0));
    float e = hash3(i + vec3(0.0, 0.0, 1.0));
    float g = hash3(i + vec3(1.0, 0.0, 1.0));
    float h = hash3(i + vec3(0.0, 1.0, 1.0));
    float k = hash3(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(a, b, u.x), mix(c, d, u.x), u.y), mix(mix(e, g, u.x), mix(h, k, u.x), u.y), u.z);
}

float fbm3(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        s += noise3(p) * a;
        p = p * 2.03 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return s / 0.9375;
}

// ---------------------------------------------------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------------------------------------------------

// A height field's bumps on any surface, from screen-space derivatives (no tangents needed).
vec3 bump(vec3 n, float h, float amount) {
    vec3 dpdx = dFdx(vPos);
    vec3 dpdy = dFdy(vPos);
    float dhdx = dFdx(h) * amount;
    float dhdy = dFdy(h) * amount;
    vec3 r1 = cross(dpdy, n);
    vec3 r2 = cross(n, dpdx);
    float det = dot(dpdx, r1);
    vec3 grad = sign(det) * (dhdx * r1 + dhdy * r2);
    return normalize(abs(det) * n - grad);
}

// The normal map's frame, from the derivatives of the position and the texture coordinates.
vec3 mapNormal(vec3 n, vec3 t) {
    vec3 dp1 = dFdx(vPos);
    vec3 dp2 = dFdy(vPos);
    vec2 duv1 = dFdx(vUv);
    vec2 duv2 = dFdy(vUv);
    vec3 dp2perp = cross(dp2, n);
    vec3 dp1perp = cross(n, dp1);
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
    float m = max(dot(T, T), dot(B, B));
    if (m < 1e-20) {
        return n;
    }
    float inv = inversesqrt(m);
    return normalize(T * inv * t.x + B * inv * t.y + n * t.z);
}

// Where this point sits inside its part's texture rectangle: x around / across, y along (0..1).
vec2 partCoord() {
    vec2 size = max(uRect.zw - uRect.xy, vec2(1e-5));
    return clamp((vUv - uRect.xy) / size, 0.0, 1.0);
}

// Across a blade: 0 at the spine, 1 at the edge (both flats).
float acrossBlade(vec2 pc) {
    return pc.x < 0.5 ? pc.x * 2.0 : (1.0 - pc.x) * 2.0;
}

// ---------------------------------------------------------------------------------------------------------------------
// Lighting
// ---------------------------------------------------------------------------------------------------------------------

const vec3 KEY = vec3(-0.42, 0.74, 0.53);
const vec3 FILL = vec3(0.78, -0.12, 0.34);
const vec3 BACK = vec3(0.25, 0.35, -0.9);

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

// What a mirror-like surface sees in direction r: the sky, the horizon's fog, the ground, two soft studio lights and
// the sun, blurred by the roughness.
vec3 environment(vec3 r, float rough) {
    float up = dot(r, uUp);
    vec3 c;
    if (up > 0.0) {
        c = mix(uHorizon, uSky, smoothstep(0.0, 0.55 + 0.3 * rough, up));
    } else {
        c = mix(uHorizon, uGround, smoothstep(0.0, 0.25 + 0.4 * rough, -up));
    }
    float spread = 0.22 + 0.55 * rough;
    float box = smoothstep(1.0 - spread, 1.0 - spread * 0.25, dot(r, normalize(vec3(-0.5, 0.78, 0.38))));
    float strip = smoothstep(1.0 - spread * 0.6, 1.0 - spread * 0.1, dot(r, normalize(vec3(0.9, 0.2, 0.4))));
    c += vec3(1.0) * (box * 0.85 + strip * 0.45) * (1.0 - 0.5 * rough);
    float sun = pow(max(dot(r, uSunDir), 0.0), mix(900.0, 12.0, rough));
    c += vec3(1.0, 0.95, 0.85) * sun * uSun * (2.5 - 2.0 * rough);
    return c * uLight;
}

float ggx(float nh, float a) {
    float a2 = a * a;
    float d = nh * nh * (a2 - 1.0) + 1.0;
    return a2 / (3.14159 * d * d + 1e-5);
}

vec3 schlick(vec3 f0, float c) {
    float k = pow(1.0 - c, 5.0);
    return f0 + (1.0 - f0) * k;
}

// Everything a surface point is.
struct Surface {
    vec3 base;
    float metal;
    float rough;
    vec3 emission;
    float alpha;
    vec3 n;
    float aniso;
    float clearcoat;
};

vec3 shade(Surface s, vec3 v) {
    vec3 n = s.n;
    float nv = max(dot(n, v), 1e-3);
    vec3 f0 = mix(vec3(0.04), s.base, s.metal);
    vec3 diffuse = s.base * (1.0 - s.metal);
    vec3 key = normalize(KEY);
    vec3 fill = normalize(FILL);
    float kd = max(dot(n, key), 0.0);
    float fd = max(dot(n, fill), 0.0);
    float hemi = 0.5 + 0.5 * dot(n, uUp);
    // Like vanilla's item lighting: never brighter than the light itself.
    vec3 light = uLight * min(1.0, 0.36 + 0.1 * hemi + 0.58 * kd + 0.16 * fd);
    vec3 col = diffuse * light;

    // The key light's highlight (brushed metal: stretched across the brushing).
    vec3 h = normalize(key + v);
    float a = max(s.rough * s.rough, 0.002);
    float spec;
    if (s.aniso > 0.0) {
        vec3 t = normalize(vAxis - n * dot(vAxis, n));
        float th = dot(t, h);
        float sinTH = sqrt(max(1.0 - th * th, 0.0));
        spec = pow(sinTH, mix(18.0, 120.0, 1.0 - s.rough)) * (1.0 - s.rough) * 1.4 * s.aniso
                + ggx(max(dot(n, h), 0.0), a) * (1.0 - s.aniso) * 0.25;
    } else {
        spec = ggx(max(dot(n, h), 0.0), a) * 0.25;
    }
    vec3 fk = schlick(f0, max(dot(h, v), 0.0));
    col += fk * spec * kd * uLight * 1.6;

    // Reflections.
    vec3 r = reflect(-v, n);
    vec3 fe = f0 + (max(vec3(1.0 - s.rough), f0) - f0) * pow(1.0 - nv, 5.0);
    col += environment(r, s.rough) * fe * (0.55 + 0.45 * s.metal);

    // A glossy clear coat over paint and obsidian.
    if (s.clearcoat > 0.0) {
        float cf = 0.04 + 0.96 * pow(1.0 - nv, 5.0);
        col += environment(r, 0.08) * cf * s.clearcoat;
        col += ggx(max(dot(n, h), 0.0), 0.004) * 0.25 * cf * kd * uLight * s.clearcoat;
    }

    // Rim: a thin light along the far edges.
    float rim = pow(1.0 - nv, 3.0) * max(dot(n, normalize(BACK)) * 0.5 + 0.5, 0.0);
    col += rim * uLight * 0.22 * mix(vec3(1.0), s.base, 0.5 * s.metal);

    col += s.emission;
    return col;
}

// ---------------------------------------------------------------------------------------------------------------------
// Materials
// ---------------------------------------------------------------------------------------------------------------------

// Fine scratches along the blade and polished, worn edges; dirt in the grooves. Returns how much of a coating is worn
// through (for Blackened and Painted), changes the roughness and darkens grooves.
float applyWear(inout Surface s, float exposed, float detail) {
    if (uWear <= 0.0) {
        return 0.0;
    }
    float w = uWear;
    vec3 q = vObj * vec3(420.0, 16.0, 420.0) + uSeed;
    float lines = smoothstep(0.86, 0.97, noise3(q)) * smoothstep(0.35, 0.8, noise3(vObj * 22.0 + 3.0));
    float n = noise3(vObj * 60.0 + 11.0);
    float worn = smoothstep(0.62, 0.95, exposed + (n - 0.5) * 0.35 + w * 0.45) * w;
    float dirt = smoothstep(0.42, 0.12, exposed + (n - 0.5) * 0.2) * w;
    s.rough = clamp(s.rough + lines * 0.25 * w - worn * 0.12 + dirt * 0.3, 0.03, 1.0);
    s.base *= 1.0 - dirt * 0.55;
    s.base = mix(s.base, s.base * 1.12, lines * w * 0.6);
    return clamp(worn + lines * w * 0.5, 0.0, 1.0);
}

vec3 steel() {
    return vec3(0.78, 0.79, 0.81);
}

// How hot the weapon is against its resting heat (1 at rest; 2 white-hot; 0 cold).
float hotness() {
    return uHeatIdle > 0.0 ? uHeat / uHeatIdle : 1.0;
}

// Infernal: how far a point is past the fire's front (a share of the core's reach; above 0: not caught yet).
float pastFront() {
    if (uCore.w <= 0.0) {
        return -1.0;
    }
    return length(vObj - uCore.xyz) / uCore.w - hotness() * 1.08;
}

// Moonsilver's engraved stars: small four-pointed stars (the curve sqrt|x| + sqrt|y| = 1) scattered over the plane
// the surface faces most (picked by the largest axis of its normal in the model's own coordinates), about one in two
// 2.4 cm cells holding one, of different sizes; each twinkles slowly on its own. 0..1: how much this point is star.
float moonStars() {
    vec3 n = abs(cross(dFdx(vObj), dFdy(vObj)));
    vec2 q = n.z >= n.x && n.z >= n.y ? vObj.xy : (n.x >= n.y ? vObj.zy : vObj.xz);
    // (A pixel's size on that plane, for soft edges; derivatives taken here, outside the loop's branches.)
    float w = length(fwidth(q));
    vec2 cell = floor(q / 0.024);
    float star = 0.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            vec2 c = cell + vec2(float(i), float(j));
            float h = hash3(vec3(c, 41.0));
            if (h > 0.5) {
                vec2 ctr = (c + 0.15 + 0.7 * vec2(hash3(vec3(c, 43.0)), hash3(vec3(c, 47.0)))) * 0.024;
                float r = mix(0.0032, 0.0085, pow(hash3(vec3(c, 53.0)), 2.0));
                vec2 e = abs(q - ctr) / r;
                float a = sqrt(e.x) + sqrt(e.y);
                float soft = clamp(w / r, 0.03, 0.6);
                float twinkle = 0.75 + 0.25 * sin(uTime * (0.8 + 0.9 * h) + 6.2832 * hash3(vec3(c, 59.0)));
                star = max(star, (1.0 - smoothstep(1.0 - soft, 1.0 + soft, a)) * twinkle);
            }
        }
    }
    return star;
}

// A blade finish at a point: pc its part's coordinates, exposed and detail from the maps, trim where the maps paint a
// finish's trim (their emission channel, which no blade finish uses for light: Dragonscale's gold).
void blade(inout Surface s, vec2 pc, float exposed, float detail, vec3 v, float trim) {
    float along = pc.y;
    float across = acrossBlade(pc);
    float nv = max(dot(s.n, v), 0.0);
    float m = uMat;
    if (m < 1.5) {
        // Polished steel.
        s.base = steel() * (0.96 + 0.08 * detail);
        s.metal = 1.0;
        s.rough = 0.12 + 0.06 * detail;
    } else if (m < 2.5) {
        // Brushed steel: fine lines along the blade.
        float lines = noise3(vObj * vec3(900.0, 6.0, 900.0));
        s.base = steel() * (0.9 + 0.12 * lines);
        s.metal = 1.0;
        s.rough = 0.3 + 0.08 * lines;
        s.aniso = 0.85;
    } else if (m < 3.5) {
        // Damascus: wavy layers of light and dark steel, folded and etched (a layer every centimetre or so).
        vec2 p = vObj.xy * vec2(110.0, 42.0);
        float warp = fbm3(vec3(vObj.xy * 26.0, 1.3)) * 3.2 + sin(vObj.y * 120.0 + vObj.x * 85.0) * 0.35;
        float layer = sin((p.x * 0.85 + p.y * 0.35 + warp * 2.4) * 3.14159);
        float bright = smoothstep(-0.2, 0.35, layer);
        s.base = mix(vec3(0.24, 0.24, 0.26), vec3(0.86, 0.86, 0.88), bright);
        s.metal = 1.0;
        s.rough = mix(0.42, 0.16, bright);
        s.n = bump(s.n, bright, 0.0006);
    } else if (m < 4.5) {
        // Blackened: a matte black coating, bright steel on the worn edge.
        s.base = vec3(0.045, 0.047, 0.05) * (0.9 + 0.2 * detail);
        s.metal = 0.25;
        s.rough = 0.62;
        float edge = smoothstep(0.93, 0.99, across) * 0.9;
        float worn = max(edge, applyWear(s, exposed, detail));
        s.base = mix(s.base, steel(), worn);
        s.metal = mix(s.metal, 1.0, worn);
        s.rough = mix(s.rough, 0.18, worn);
        return;
    } else if (m < 5.5) {
        // Gold.
        s.base = vec3(1.0, 0.76, 0.33) * (0.95 + 0.08 * detail);
        s.metal = 1.0;
        s.rough = 0.2 + 0.05 * detail;
    } else if (m < 6.5) {
        // Nebula: indigo at the tip, violet, bright magenta at the guard, with soft nebula clouds; a thin film over it
        // shifts the hue as the blade tilts.
        vec3 indigo = vec3(0.13, 0.09, 0.42);
        vec3 violet = vec3(0.44, 0.16, 0.86);
        vec3 magenta = vec3(1.0, 0.24, 0.78);
        vec3 g = along < 0.5 ? mix(magenta, violet, along / 0.5) : mix(violet, indigo, (along - 0.5) / 0.5);
        float big = fbm3(vObj * 7.0 + uSeed);
        float fine = fbm3(vObj * 26.0 + 4.0 + uSeed);
        g = mix(g, g * vec3(0.55, 0.62, 1.25), smoothstep(0.42, 0.78, big) * 0.55);
        g = mix(g, vec3(1.0, 0.62, 0.96), pow(max(fine - 0.5, 0.0) * 2.0, 2.0) * 0.55);
        g *= 0.82 + 0.3 * smoothstep(0.3, 0.75, big);
        float film = fine * 0.9 + (1.0 - nv) * 1.25 + along * 0.35;
        vec3 irid = 0.5 + 0.5 * cos(6.28318 * (film + vec3(0.0, 0.33, 0.67)));
        s.base = mix(g, g * (0.55 + 0.9 * irid), 0.42);
        s.metal = 0.8;
        s.rough = 0.2 + 0.08 * fine;
    } else if (m < 7.5) {
        // Prism: a holographic rainbow that moves with the viewing angle.
        float bands = along * 2.2 + across * 0.6 + (1.0 - nv) * 2.1 + fbm3(vObj * 12.0) * 0.6;
        vec3 rainbow = 0.55 + 0.45 * cos(6.28318 * (bands + vec3(0.0, 0.33, 0.67)));
        float grating = 0.85 + 0.15 * sin((vObj.x + vObj.y) * 900.0);
        s.base = rainbow * grating;
        s.metal = 1.0;
        s.rough = 0.1;
    } else if (m < 8.5) {
        // Obsidian: glossy black glass with a purple sheen and soft conchoidal ripples.
        float ripple = fbm3(vObj * 30.0 + uSeed);
        s.n = bump(s.n, ripple, 0.0009);
        float sheen = pow(1.0 - max(dot(s.n, v), 0.0), 2.0);
        s.base = vec3(0.012, 0.01, 0.018) + vec3(0.28, 0.08, 0.5) * sheen * 0.6;
        s.metal = 0.0;
        s.rough = 0.05;
        s.clearcoat = 0.6;
    } else if (m < 9.5) {
        // Painted: the color slot along the blade, a satin clear coat, steel where it is worn.
        s.base = mix(uColA, uColB, along) * (0.94 + 0.1 * detail);
        s.metal = 0.0;
        s.rough = 0.38;
        s.clearcoat = 0.35;
        float edge = smoothstep(0.95, 0.995, across) * 0.8;
        float worn = max(edge, applyWear(s, exposed, detail));
        s.base = mix(s.base, steel(), worn);
        s.metal = mix(s.metal, 1.0, worn);
        s.rough = mix(s.rough, 0.2, worn);
        s.clearcoat *= 1.0 - worn;
        return;
    } else if (m < 10.5) {
        // Void: dark energy with a glowing edge and slowly drifting motes.
        vec3 q = vObj * 9.0 + vec3(0.0, -uTime * 0.25, uTime * 0.1);
        float swirl = fbm3(q + fbm3(q * 1.7) * 1.5);
        s.base = vec3(0.02, 0.012, 0.035) + uColA * swirl * 0.08;
        s.metal = 0.0;
        s.rough = 0.3;
        s.clearcoat = 0.3;
        float edge = smoothstep(0.78, 1.0, across);
        float motes = smoothstep(0.82, 0.95, noise3(vObj * 70.0 + vec3(0.0, -uTime * 0.8, 0.0)));
        s.emission = uColA * (edge * 1.6 + swirl * 0.25 + motes * 1.2) * uGlow;
        return;
    } else if (m < 11.5) {
        // Hard light: a white-hot core, the color at the rims, a faint shimmer along the edge.
        float core = smoothstep(0.35, 0.0, abs(across - 0.5));
        float shimmer = 0.9 + 0.1 * noise3(vec3(vObj.y * 60.0 - uTime * 6.0, uTime * 2.0, 0.0));
        vec3 glow = mix(uColA, vec3(1.0), core * 0.85);
        s.base = vec3(0.0);
        s.metal = 0.0;
        s.rough = 1.0;
        s.emission = glow * (1.1 + 0.4 * core) * shimmer * uGlow;
        return;
    } else if (m < 12.5) {
        // Ice: translucent and faceted, cloudy deeper in, with thin white cracks inside (where two noises cross their
        // middle); a facet turned to the key light flashes.
        float crack = 1.0 - smoothstep(0.0, 0.035, abs(noise3(vObj * 38.0 + uSeed) - 0.5));
        crack = max(crack, 0.6 * (1.0 - smoothstep(0.0, 0.03, abs(noise3(vObj * 85.0 + 3.0 + uSeed) - 0.5))));
        float depth = fbm3(vObj * 12.0 + 2.0);
        s.base = mix(mix(vec3(0.42, 0.72, 0.98), vec3(0.88, 0.96, 1.0), depth), vec3(1.0), crack * 0.75);
        s.metal = 0.0;
        s.rough = 0.05;
        s.clearcoat = 0.8;
        s.alpha = clamp(mix(0.45, 0.85, max(pow(1.0 - nv, 2.0), depth * 0.6)) + crack * 0.35, 0.0, 1.0);
        float flash = pow(max(dot(s.n, normalize(normalize(KEY) + v)), 0.0), 180.0);
        s.emission = (uColA * 0.1 + vec3(0.6, 0.85, 1.0) * crack * 0.12) * uGlow + vec3(0.9, 0.97, 1.0) * flash * 0.6 * uLight;
        // Ice regrowing along the edges (Frost Breath): past the reveal's front the edges are not there (a ragged
        // line), and a frosty glint runs along the front.
        if (uReveal < 9000.0) {
            float edges = abs(acrossBlade(pc) - 0.5) * 2.0;
            float ragged = noise3(vObj * vec3(160.0, 60.0, 160.0));
            if (vObj.y > uReveal + (ragged - 0.5) * 0.012 && edges > 0.5 + 0.2 * ragged) {
                discard;
            }
            float front = exp(-pow((vObj.y - uReveal) / 0.012, 2.0)) * smoothstep(0.3, 0.8, edges);
            s.emission += vec3(0.85, 0.96, 1.0) * front * 1.6;
            s.alpha = max(s.alpha, front);
        }
        return;
    } else if (m < 13.5) {
        // Yew (bows): honey-gold sapwood and reddish-brown heartwood in a fine grain along the limb, wavering round
        // the odd knot; waxed, so a soft sheen.
        float warp = fbm3(vObj * vec3(26.0, 3.0, 26.0)) * 2.4;
        float grain = 0.5 + 0.5 * sin((vObj.x * 420.0 + vObj.z * 160.0) + warp * 6.0);
        float fine = noise3(vObj * vec3(600.0, 14.0, 600.0));
        float heart = smoothstep(0.3, 0.85, grain * 0.7 + fine * 0.3);
        s.base = mix(vec3(0.84, 0.62, 0.36), vec3(0.5, 0.26, 0.13), heart) * (0.92 + 0.12 * detail);
        s.metal = 0.0;
        s.rough = 0.46 - 0.1 * fine;
        s.clearcoat = 0.25;
    } else if (m < 14.5) {
        // Infernal: molten red-orange, hotter toward the edge, a darker cooling crust along the thick spine; thin lighter
        // heat lines flowing slowly down the blade (the flow is the noise drifting down the model). Caught from the core
        // outward as the weapon heats: past the fire's front it is cold blackened steel, and the front flashes as it
        // passes; above its resting heat the lines widen and the crust burns away.
        float past = pastFront();
        float lit = 1.0 - smoothstep(-0.03, 0.03, past);
        float front = exp(-past * past / 0.0008) * step(hotness(), 0.999);
        float flareUp = clamp(hotness() - 1.0, 0.0, 1.0);
        vec3 q = vObj * 14.0 + vec3(0.0, uTime * 0.25, 0.0);
        float flow = fbm3(q + vec3(fbm3(q * 0.6 + 3.1) * 1.2));
        float lines = 1.0 - smoothstep(0.0, 0.03 + 0.03 * flareUp, abs(flow - 0.5));
        float cool = (1.0 - smoothstep(0.0, 0.35, across)) * (1.0 - flareUp);
        float specks = smoothstep(0.62, 0.8, noise3(vObj * 60.0 + uSeed)) * cool;
        vec3 molten = mix(vec3(0.72, 0.1, 0.02), vec3(1.0, 0.42, 0.07), clamp(0.3 + 0.4 * smoothstep(0.3, 0.7, flow) + 0.3 * across, 0.0, 1.0));
        molten = mix(molten, vec3(1.0, 0.86, 0.46), lines);
        molten = mix(molten, vec3(1.0, 0.95, 0.8), flareUp * 0.4);
        s.base = mix(vec3(0.05, 0.047, 0.05), vec3(0.14, 0.045, 0.03), lit);
        s.metal = mix(0.3, 0.1, lit);
        s.rough = mix(0.6, 0.45, lit);
        s.n = bump(s.n, specks, 0.0006);
        float glow = lit * (1.0 - 0.7 * cool) * (1.0 - 0.8 * specks) * (0.9 + 0.5 * flareUp);
        s.emission = (molten * glow + vec3(1.0, 0.62, 0.22) * front * 1.6) * uGlow;
        return;
    } else if (m < 15.5) {
        // Bone: ivory, warmer and darker in the grooves (where the blade is least exposed), fine dark cracks (where a
        // stretched noise crosses its middle, here and there), a soft waxy sheen.
        float crack = 1.0 - smoothstep(0.0, 0.03, abs(noise3(vObj * vec3(60.0, 22.0, 60.0) + uSeed) - 0.5));
        crack *= smoothstep(0.35, 0.7, noise3(vObj * 9.0 + 5.0));
        float groove = 1.0 - smoothstep(0.15, 0.45, exposed);
        vec3 ivory = mix(vec3(0.93, 0.89, 0.78), vec3(0.78, 0.7, 0.55), groove * 0.8);
        ivory *= 0.94 + 0.08 * fbm3(vObj * 18.0 + uSeed);
        s.base = mix(ivory, vec3(0.42, 0.36, 0.28), crack * 0.85);
        s.metal = 0.0;
        s.rough = 0.42 + 0.2 * crack;
        s.clearcoat = 0.2;
        s.n = bump(s.n, -crack, 0.0006);
    } else if (m < 16.5) {
        // Crimson: deep red lacquered steel, darker toward the spine, bright red at the edges, under a glossy coat;
        // worn edges show the steel.
        vec3 deep = mix(vec3(0.24, 0.02, 0.03), vec3(0.55, 0.05, 0.06), smoothstep(0.0, 0.75, across));
        float edge = smoothstep(0.86, 0.99, across);
        s.base = mix(deep, vec3(0.95, 0.16, 0.13), edge) * (0.95 + 0.08 * detail);
        s.metal = 0.65;
        s.rough = 0.22;
        s.clearcoat = 0.5;
        float worn = applyWear(s, exposed, detail);
        s.base = mix(s.base, steel(), worn * 0.7);
        return;
    } else if (m < 17.5) {
        // Sapphire: deep blue steel, lighter toward the edge, the edge itself a frosty pale blue with a fine frost on it.
        float edge = smoothstep(0.8, 0.98, across);
        float frost = smoothstep(0.45, 0.8, noise3(vObj * 140.0 + uSeed)) * edge;
        vec3 blue = mix(vec3(0.05, 0.1, 0.32), vec3(0.12, 0.24, 0.62), smoothstep(0.0, 0.8, across));
        s.base = mix(blue, vec3(0.72, 0.86, 1.0), edge * 0.85);
        s.base = mix(s.base, vec3(0.95, 0.98, 1.0), frost * 0.6);
        s.metal = mix(0.9, 0.3, edge);
        s.rough = mix(0.18, 0.5, frost);
    } else if (m < 18.5) {
        // Dragonscale: overlapping deep red scales in staggered rows across the flats (each row's rounded rims lie over
        // the tops of the row below: the scale over a point is the highest row's whose disc holds it), domed, darker
        // where they tuck under the row above, a lighter rim and a dark seam round each, under a glossy lacquer; gold
        // trim where the maps paint it (the edges, a wing's bones).
        vec2 cell = vec2(0.021, 0.0125);
        vec2 q = vObj.xy / cell;
        float j0 = floor(q.y);
        vec2 d = vec2(0.0, 1.0);
        float id = 0.5;
        for (int k = 2; k >= -1; k--) {
            float j = j0 + float(k);
            float off = mod(j, 2.0) * 0.5;
            float i = floor(q.x - off + 0.5);
            vec2 e = (q - vec2(i + off, j)) * cell / 0.0128;
            if (dot(e, e) < 1.0) {
                d = e;
                id = hash3(vec3(i, j, 5.0));
                break;
            }
        }
        float r = length(d);
        float dome = sqrt(max(0.0, 1.0 - r * r));
        float tuck = smoothstep(-0.35, 0.55, d.y);
        float rim = smoothstep(0.6, 0.88, r) * (1.0 - smoothstep(0.9, 0.97, r));
        float seam = smoothstep(0.9, 1.0, r);
        float light = (0.55 + 0.45 * dome) * (1.0 - 0.55 * tuck) * (0.88 + 0.24 * id);
        vec3 red = mix(vec3(0.13, 0.008, 0.012), vec3(0.62, 0.065, 0.045), light);
        red = mix(red, vec3(0.86, 0.2, 0.12), rim * 0.35 * (1.0 - tuck));
        red *= 1.0 - 0.6 * seam;
        float gold = smoothstep(0.4, 0.6, trim);
        s.base = mix(red, vec3(1.0, 0.74, 0.3) * (0.92 + 0.12 * detail), gold);
        s.metal = mix(0.35, 1.0, gold);
        s.rough = mix(0.3 + 0.12 * seam, 0.22, gold);
        s.clearcoat = 0.45 * (1.0 - gold);
        s.n = bump(s.n, (dome - 0.5 * seam) * (1.0 - gold), 0.0011);
    } else if (m < 19.5) {
        // Moonsilver: a cool pale silver, with fine stars engraved in it (cut a little in, duller where cut) that glow
        // softly in the accent color, and a faint glow of it round the rims; both swell as the weapon flares (the
        // Crescent Bow's Moonrise).
        float st = moonStars();
        s.base = mix(vec3(0.8, 0.84, 0.9) * (0.95 + 0.08 * detail), vec3(0.5, 0.56, 0.66), st * 0.6);
        s.metal = 1.0;
        s.rough = mix(0.15 + 0.05 * detail, 0.45, st);
        s.n = bump(s.n, -st, 0.0004);
        float flare = clamp(uFlare, 0.0, 1.0);
        s.emission = uColA * (st * (0.5 + 1.6 * flare) + pow(1.0 - nv, 3.0) * (0.06 + 0.3 * flare)) * uGlow;
    } else if (m < 26.5) {
        // Living wood: dark bark in long fibres winding up it (as the Verdant Lance's shaft twists), split here and there
        // into cracks where a faint glow of the accent shows through; green moss in the grooves (where the bark is least
        // exposed) with tiny leaves in it here and there. The glow swells as the weapon flares (Bloom). Wood doesn't
        // wear as metal does.
        float wind = pc.x - vObj.y * 1.114;
        float fibre = noise3(vec3(wind * 48.0, vObj.y * 5.0, 3.0));
        float crack = 1.0 - smoothstep(0.0, 0.05, abs(noise3(vec3(wind * 20.0, vObj.y * 2.5, 9.0)) - 0.5));
        crack *= smoothstep(0.45, 0.7, noise3(vObj * 14.0 + 2.0));
        float moss = smoothstep(0.55, 0.85, (1.0 - exposed) * 0.7 + fbm3(vObj * 40.0 + uSeed) * 0.6);
        float leaf = smoothstep(0.86, 0.9, noise3(vObj * 160.0 + 7.0)) * moss;
        vec3 bark = mix(vec3(0.13, 0.085, 0.05), vec3(0.3, 0.2, 0.12), fibre);
        vec3 mossColor = mix(vec3(0.16, 0.3, 0.08), vec3(0.3, 0.52, 0.14), fbm3(vObj * 90.0));
        s.base = mix(mix(bark, mossColor, moss), vec3(0.36, 0.66, 0.2), leaf) * (1.0 - 0.6 * crack);
        s.metal = 0.0;
        s.rough = mix(0.78, 0.9, moss);
        s.n = bump(s.n, fibre * 0.6 - crack + moss * 0.5, 0.0012);
        s.emission = uColA * crack * (0.35 + 0.8 * clamp(uFlare, 0.0, 1.0)) * uGlow;
        return;
    }
    // Everything but the coated finishes wears as bare metal does.
    applyWear(s, exposed, detail);
}

// Procedural handle materials, on the part's own coordinates (around, along).
void handle(inout Surface s, vec2 pc, float painted, float detail) {
    vec3 color = uColA;
    float h = uHandle;
    if (h < 0.5) {
        // As the maps paint it, in the chosen color.
        s.base = color * painted * 2.0;
        s.metal = 0.0;
        s.rough = clamp(uRough, 0.2, 1.0);
        return;
    }
    float along = pc.y;
    float around = pc.x;
    if (h < 1.5) {
        // Leather wrap: overlapping diagonal bands.
        float band = fract(along * 11.0 + around * 1.0);
        float ridge = smoothstep(0.0, 0.18, band) * smoothstep(1.0, 0.8, band);
        float grain = noise3(vObj * 300.0);
        s.n = bump(s.n, ridge + grain * 0.15, 0.0012);
        s.base = color * (0.62 + 0.38 * ridge) * (0.9 + 0.2 * grain);
        s.metal = 0.0;
        s.rough = 0.62 - 0.1 * ridge;
    } else if (h < 2.5) {
        // Wood: grain along the handle, darker late wood, a soft oiled sheen.
        float rings = sin((along * 9.0 + fbm3(vObj * vec3(30.0, 4.0, 30.0)) * 4.0) * 6.28318);
        float fine = noise3(vObj * vec3(600.0, 20.0, 600.0));
        float late = smoothstep(0.2, 0.9, rings);
        s.base = color * (1.0 - 0.35 * late) * (0.9 + 0.2 * fine);
        s.n = bump(s.n, late * 0.5 + fine * 0.2, 0.0006);
        s.metal = 0.0;
        s.rough = 0.5;
        s.clearcoat = 0.15;
    } else if (h < 3.5) {
        // Rubber grip: fine diamond texture.
        float u = around * 48.0;
        float v = along * 70.0;
        float d = min(abs(fract(u + v) - 0.5), abs(fract(u - v) - 0.5));
        s.n = bump(s.n, d, 0.0015);
        s.base = color * (0.85 + 0.3 * d);
        s.metal = 0.0;
        s.rough = 0.8 - 0.15 * d;
    } else if (h < 4.5) {
        // Cord wrap: two crossing cords with diamond gaps.
        float u = around * 6.0;
        float v = along * 9.0;
        float c1 = abs(fract(u + v) - 0.5);
        float c2 = abs(fract(u - v) - 0.5);
        float cord = smoothstep(0.5, 0.2, min(c1, c2));
        float twist = noise3(vObj * 500.0);
        s.n = bump(s.n, cord + twist * 0.1, 0.0015);
        s.base = color * (0.35 + 0.65 * cord) * (0.9 + 0.2 * twist);
        s.metal = 0.0;
        s.rough = 0.75;
    } else {
        // Metal: knurled.
        float u = around * 64.0;
        float v = along * 80.0;
        float d = min(abs(fract(u + v) - 0.5), abs(fract(u - v) - 0.5));
        s.n = bump(s.n, d, 0.001);
        s.base = mix(color, steel(), 0.5) * (0.85 + 0.25 * d);
        s.metal = 1.0;
        s.rough = 0.32 - 0.1 * d;
    }
}

void main() {
    vec3 v = normalize(-vPos);
    vec3 n = normalize(vNormal);
    if (!gl_FrontFacing) {
        n = -n;
    }
    Surface s;
    s.base = vec3(0.5);
    s.metal = 0.0;
    s.rough = 0.6;
    s.emission = vec3(0.0);
    s.alpha = 1.0;
    s.n = n;
    s.aniso = 0.0;
    s.clearcoat = 0.0;

    if (uMat > 29.5) {
        // The arms: the skin as it is, with the sleeve's transparent parts cut out.
        vec4 skin = texture2D(uAlbedo, vUv);
        if (skin.a < 0.1) {
            discard;
        }
        s.base = skin.rgb;
        s.rough = 0.75;
        vec3 col = shade(s, v);
        gl_FragColor = vec4(col * uAlpha, uAlpha);
        return;
    }

    vec4 albedo = uHasMaps > 0.5 ? texture2D(uAlbedo, vUv) : vec4(0.5, 0.5, 0.5, 1.0);
    vec4 mat = uHasMaps > 0.5 ? texture2D(uMaterial, vUv) : vec4(0.0, 0.5, 0.0, 0.5);
    if (uHasNormalMap > 0.5) {
        vec3 t = texture2D(uNormalMap, vUv).xyz * 2.0 - 1.0;
        s.n = mapNormal(s.n, t);
    }
    float detail = luma(albedo.rgb) * 2.0 - 1.0;
    float exposed = mat.a;
    vec2 pc = partCoord();

    if (uMat < 0.5) {
        // As painted in the maps.
        s.base = albedo.rgb;
        s.metal = mat.r;
        s.rough = mat.g;
        s.emission = albedo.rgb * mat.b * uGlow * 1.5;
        applyWear(s, exposed, detail);
    } else if (uMat < 19.5 || uMat > 25.5 && uMat < 29.5) {
        // (Blade finishes are 1-19 and 26-29; 20-25 are the other parts' materials.)
        blade(s, pc, exposed, detail, v, mat.b);
    } else if (uMat < 20.5) {
        // Hardware: guard, pommel, fittings in the weapon's metal color.
        s.base = uColA * (albedo.rgb * 2.0);
        s.metal = mat.r;
        s.rough = clamp(uRough + (mat.g - 0.5) * 0.4, 0.05, 1.0);
        applyWear(s, exposed, detail);
    } else if (uMat < 21.5) {
        handle(s, pc, luma(albedo.rgb), detail);
        applyWear(s, exposed, detail);
    } else if (uMat < 22.5) {
        // Accent: anodized metal in the accent color.
        s.base = uColA * (0.85 + 0.3 * luma(albedo.rgb));
        s.metal = 0.85;
        s.rough = 0.26;
        s.emission = uColA * mat.b * uGlow;
    } else if (uMat < 23.5) {
        // Energy: glows in its color (runes, crystals, heated edges), dim beyond the reveal's front (which flashes
        // as it passes), brighter as it flares.
        float pulse = 0.85 + 0.15 * sin(uTime * 2.4 + vObj.y * 8.0);
        s.base = uColA * 0.2;
        s.metal = 0.0;
        s.rough = 0.25;
        float lit = vObj.y < uReveal ? 1.0 : 0.1;
        // (A core is dark while its weapon is cold, and catches as it starts to warm.)
        if (uCore.w > 0.0) {
            lit *= mix(0.08, 1.0, smoothstep(0.0, 0.08, hotness()));
        }
        float front = exp(-pow((vObj.y - uReveal) / 0.018, 2.0)) * 1.6;
        s.emission = mix(uColA, vec3(1.0), 0.25) * ((0.8 + 0.6 * mat.b) * pulse * lit + front) * (1.0 + 2.0 * uFlare) * uGlow;
    } else if (uMat < 24.5) {
        // Painted plastic in the accent color (the chainsaw's housing): a satin sheen, scuffed where exposed.
        s.base = uColA * (luma(albedo.rgb) * 2.0);
        s.metal = 0.0;
        s.rough = clamp(mat.g, 0.2, 0.9);
        s.clearcoat = 0.12;
        applyWear(s, exposed, detail);
    } else {
        // A saw chain: dark oiled steel with a fine grit (no maps: its texture coordinates hold its place along the
        // chain).
        float grit = noise3(vObj * 900.0);
        s.base = vec3(0.34, 0.35, 0.37) * (0.85 + 0.25 * grit);
        s.metal = 1.0;
        s.rough = 0.42 - 0.12 * grit;
        s.n = n;
    }

    // Heat (Thermal Machete): the blade's edge glows orange to white-hot (an Infernal blade burns as a whole instead).
    if (uHeat > 0.0 && uMat > 0.5 && uMat < 19.5 && abs(uMat - 14.0) > 0.5) {
        float across = acrossBlade(pc);
        float edge = smoothstep(0.6, 1.0, across) * uHeat;
        vec3 hot = mix(vec3(1.0, 0.25, 0.02), vec3(1.0, 0.95, 0.8), smoothstep(0.55, 1.0, edge));
        s.emission += hot * edge * 2.2;
        s.base *= 1.0 - edge * 0.7;
    }

    // Enchanted: a smooth sheen sweeping along the model (or vanilla's stripes).
    if (uSheen > 0.5) {
        float d = dot(vObj, vec3(0.55, 1.0, 0.25));
        float sweep;
        if (uSheen < 1.5) {
            float phase = fract(uTime * 0.28);
            float x = d * 1.6 - phase * 3.2 + 0.8;
            sweep = exp(-x * x * 18.0) * 0.9 + 0.12;
        } else {
            float a = fract(d * 5.0 - uTime * 0.6);
            float b = fract(dot(vObj, vec3(-0.8, 1.0, 0.3)) * 4.0 + uTime * 0.45);
            sweep = (smoothstep(0.0, 0.1, a) * smoothstep(0.35, 0.1, a) + smoothstep(0.0, 0.1, b) * smoothstep(0.3, 0.1, b)) * 0.5;
        }
        s.emission += uSheenColor * sweep * (0.35 + 0.65 * uLight.g);
    }

    // Edge check: a glint runs along the edge, from the guard to the tip.
    if (uGlint >= 0.0 && (uMat > 0.5 && uMat < 19.5 || uMat > 25.5 && uMat < 29.5)) {
        float along = pc.y;
        float across = acrossBlade(pc);
        float d = (along - uGlint) / 0.06;
        s.emission += vec3(1.0, 0.97, 0.92) * exp(-d * d) * smoothstep(0.7, 1.0, across) * 1.6;
    }

    vec3 col = shade(s, v);
    if (uFog > 0.5) {
        float f = clamp((gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale, 0.0, 1.0);
        col = mix(gl_Fog.color.rgb, col, f);
    }
    // A soft shoulder instead of a hard clip, so highlights keep their color.
    col = col / (1.0 + max(col - 0.85, vec3(0.0)) * 1.4);
    float alpha = s.alpha * uAlpha;
    gl_FragColor = vec4(col * alpha, alpha);
}
