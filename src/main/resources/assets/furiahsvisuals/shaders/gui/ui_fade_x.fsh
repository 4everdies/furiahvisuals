#version 120
uniform sampler2D uTex;
uniform vec4 uFadeX;
varying vec2 vLocal;
void main() {
    float x = gl_FragCoord.x;
    float m = smoothstep(uFadeX.x, uFadeX.y, x) * (1.0 - smoothstep(uFadeX.z, uFadeX.w, x));
    vec4 c = texture2D(uTex, vLocal) * gl_Color;
    gl_FragColor = vec4(c.rgb, c.a * m);
}
