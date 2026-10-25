// Item tint: multiplies the view model's own pixels towards the glow color (drawn before the composite, blended
// as scene * (color * a + 1 - a)), so the texture's detail stays.
uniform sampler2D sMask;
uniform float uTint;
uniform float uOpacity;
varying vec2 vUv;

void main() {
    float cover = texture2D(sMask, vUv).r;
    if (cover <= 0.0) {
        discard;
    }
    vec2 center;
    vec2 axis;
    float len;
    itemShape(center, axis, len);
    vec4 c = lut(gradientAt(vUv * uScreen, 0.0, center, len), 0.25);
    float a = uTint * cover * uOpacity * c.a;
    gl_FragColor = vec4(c.rgb * a, a);
}
