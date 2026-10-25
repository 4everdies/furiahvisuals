#version 120
uniform sampler2D uColor;
uniform sampler2D uDepth;
uniform vec2 uSize;
uniform vec4 uR0;
uniform vec4 uR1;
uniform vec4 uR2;
uniform vec4 uR3;
// x scale, y max length (screen heights), z aspect (width / height), w a number for this frame
uniform vec4 uBlur;
uniform vec2 uFallback;
uniform float uLinear;
void main() {
    vec2 uv = gl_FragCoord.xy / uSize;
    float d = texture2D(uDepth, uv).r;
    vec4 prev = mat4(uR0, uR1, uR2, uR3) * vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    vec2 b = uFallback;
    if (prev.w > 0.000001) {
        vec2 m = (uv - (prev.xy / prev.w * 0.5 + 0.5)) * uBlur.x;
        // NaN fails every comparison: then the fallback stays.
        if (abs(m.x) < 4.0 && abs(m.y) < 4.0) {
            b = m;
        }
    }
    float len = length(vec2(b.x * uBlur.z, b.y));
    if (len > uBlur.y) {
        b *= uBlur.y / len;
        len = uBlur.y;
    }
    vec3 c0 = texture2D(uColor, uv).rgb;
    if (len * uSize.y < 0.5) {
        gl_FragColor = vec4(c0, 1.0);
    } else {
        float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy + vec2(uBlur.w * 5.588238), vec2(0.06711056, 0.00583715))));
        vec3 sum = vec3(0.0);
        for (int i = 0; i < SAMPLES; i++) {
            float t = (float(i) + jitter) / float(SAMPLES) - 0.5;
            vec3 c = texture2D(uColor, uv + b * t).rgb;
            sum += uLinear > 0.5 ? c * c : c;
        }
        sum /= float(SAMPLES);
        gl_FragColor = vec4(uLinear > 0.5 ? sqrt(sum) : sum, 1.0);
    }
}
