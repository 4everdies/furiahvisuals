#version 120
uniform sampler2D uCur;
uniform sampler2D uHist;
uniform vec2 uSize;
// x keep, y phosphor (0/1), z linear light (0/1), w start over (0/1)
uniform vec4 uParams;
// x the least step, y the least step relative to the value
uniform vec2 uStep;
void main() {
    vec2 uv = gl_FragCoord.xy / uSize;
    vec3 cur = texture2D(uCur, uv).rgb;
    if (uParams.z > 0.5) {
        cur = pow(cur, vec3(2.2));
    }
    vec3 outc = cur;
    if (uParams.w < 0.5) {
        vec3 hist = texture2D(uHist, uv).rgb;
        vec3 least = max(vec3(uStep.x), max(hist, cur) * uStep.y);
        if (uParams.y > 0.5) {
            vec3 faded = min(hist * uParams.x, max(hist - least, vec3(0.0)));
            outc = max(cur, faded);
        } else {
            vec3 d = cur - hist;
            vec3 ad = abs(d);
            vec3 move = min(ad, max(ad * (1.0 - uParams.x), least));
            outc = hist + sign(d) * move;
        }
    }
    gl_FragColor = vec4(outc, 1.0);
}
