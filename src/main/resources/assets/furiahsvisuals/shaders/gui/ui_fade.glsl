uniform vec4 uFade;
float fadeMask() {
    float y = gl_FragCoord.y;
    return smoothstep(uFade.x, uFade.y, y) * (1.0 - smoothstep(uFade.z, uFade.w, y));
}
