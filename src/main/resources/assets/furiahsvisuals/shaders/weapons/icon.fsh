#version 120
// Furiah's Visuals, Weapons: a weapon's icon (premultiplied, from the atlas), and for an enchanted item the same
// smooth sheen (or vanilla-style stripes) the 3D weapon has, only where the icon is. A Pixelated weapon's icon (uPixels:
// its pixels across) gets them on whole pixels: the sheen a band four pixels wide stepping across, each of its pixels
// one step lighter; the stripes taken at each pixel's middle.

uniform sampler2D uTex;
uniform vec4 uCell;
uniform float uSheen;
uniform float uTime;
uniform vec3 uSheenColor;
uniform float uPixels;

varying vec2 vUv;

void main() {
    vec4 c = texture2D(uTex, vUv);
    if (uSheen > 0.5) {
        vec2 local = (vUv - uCell.xy) / max(uCell.zw - uCell.xy, vec2(1e-5));
        if (uPixels > 0.5) {
            // On the icon's own pixels; y up from its bottom.
            vec2 p = floor(vec2(local.x, 1.0 - local.y) * uPixels);
            if (uSheen < 1.5) {
                float band = mod(floor(uTime * 12.0), uPixels * 2.0 + 12.0) - 6.0;
                float d = p.x + p.y - band;
                if (d >= 0.0 && d < 4.0) {
                    c.rgb = min(c.rgb * 1.22 + vec3(0.06) * c.a, vec3(c.a));
                }
            } else {
                vec2 m = (p + 0.5) / uPixels;
                float a = fract((m.x + m.y) * 2.0 - uTime * 0.6);
                float band = smoothstep(0.0, 0.1, a) * smoothstep(0.35, 0.1, a) * 0.6;
                c.rgb += uSheenColor * band * c.a;
            }
        } else {
            float d = local.x + local.y;
            float band;
            if (uSheen < 1.5) {
                float x = d - fract(uTime * 0.28) * 3.2 + 0.6;
                band = exp(-x * x * 10.0) * 0.8 + 0.1;
            } else {
                float a = fract(d * 2.0 - uTime * 0.6);
                band = smoothstep(0.0, 0.1, a) * smoothstep(0.35, 0.1, a) * 0.6;
            }
            c.rgb += uSheenColor * band * c.a;
        }
    }
    gl_FragColor = c;
}
