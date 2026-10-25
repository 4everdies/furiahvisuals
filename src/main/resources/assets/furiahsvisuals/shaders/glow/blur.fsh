#version 120
// One direction of a gaussian blur of the coverage (r): the halo. Each pair of neighbouring texels is read with a
// single bilinear fetch placed between them by their weights.
uniform sampler2D uTex;
uniform vec2 uStep;      // one texel along the blur direction, uv
uniform float uSigma;    // texels
uniform int uPairs;      // texel pairs on each side (at most 16)
varying vec2 vUv;

void main() {
    float k = -0.5 / (uSigma * uSigma);
    float sum = texture2D(uTex, vUv).r;
    float total = 1.0;
    for (int i = 0; i < 16; i++) {
        if (i >= uPairs) {
            break;
        }
        float x1 = float(2 * i + 1);
        float x2 = x1 + 1.0;
        float w1 = exp(k * x1 * x1);
        float w2 = exp(k * x2 * x2);
        float w = w1 + w2;
        if (w < 1.0e-5) {
            break;
        }
        vec2 o = uStep * ((x1 * w1 + x2 * w2) / w);
        sum += (texture2D(uTex, vUv + o).r + texture2D(uTex, vUv - o).r) * w;
        total += 2.0 * w;
    }
    gl_FragColor = vec4(sum / total, 0.0, 0.0, 1.0);
}
