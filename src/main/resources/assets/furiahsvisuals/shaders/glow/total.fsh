#version 120
// The view model's shape, step 2 of 2: adds up the block sums of step 1 into a 2 x 1 target: the left texel holds
// (m, x m, y m, x x m), the right one (y y m, x y m).
uniform sampler2D uA;
uniform sampler2D uB;
uniform vec2 uSize;          // texels of the step 1 targets (at most 16 x 8)

void main() {
    bool second = gl_FragCoord.x > 1.0;
    vec4 sum = vec4(0.0);
    for (int j = 0; j < 8; j++) {
        if (float(j) >= uSize.y) {
            break;
        }
        for (int i = 0; i < 16; i++) {
            if (float(i) >= uSize.x) {
                break;
            }
            vec2 uv = (vec2(float(i), float(j)) + 0.5) / uSize;
            sum += second ? texture2D(uB, uv) : texture2D(uA, uv);
        }
    }
    gl_FragColor = sum;
}
