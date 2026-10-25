#version 120
// The view model's shape, step 1 of 2: sums over a block of a small level of the mask (coverage m at uv):
// uSecond 0 gives (m, x m, y m, x x m), 1 gives (y y m, x y m). From these sums the composite gets the item's real
// center and its long axis, whatever the item is (a 3D model, a map, a bow being drawn).
uniform sampler2D uTex;
uniform vec2 uLevelSize;     // texels of the level
uniform vec2 uBlock;         // level texels summed by one output texel (at most 16 x 16)
uniform float uSecond;

void main() {
    vec2 cell = floor(gl_FragCoord.xy);
    vec4 sum = vec4(0.0);
    for (int j = 0; j < 16; j++) {
        if (float(j) >= uBlock.y) {
            break;
        }
        for (int i = 0; i < 16; i++) {
            if (float(i) >= uBlock.x) {
                break;
            }
            vec2 t = cell * uBlock + vec2(float(i), float(j));
            if (t.x < uLevelSize.x && t.y < uLevelSize.y) {
                vec2 uv = (t + 0.5) / uLevelSize;
                float m = texture2D(uTex, uv).r;
                sum += uSecond > 0.5 ? vec4(uv.y * uv.y * m, uv.x * uv.y * m, 0.0, 0.0)
                        : vec4(m, uv.x * m, uv.y * m, uv.x * uv.x * m);
            }
        }
    }
    gl_FragColor = sum;
}
