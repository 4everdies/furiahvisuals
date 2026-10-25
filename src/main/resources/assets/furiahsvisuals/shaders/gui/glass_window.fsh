#version 120
#include "gui/glass_shape_common.glsl"
void main() {
    float cover = clamp(0.5 - roundBox(vLocal, uHalf, uRadius), 0.0, 1.0);
    if (cover <= 0.0) discard;
    gl_FragColor = vec4(bg(screenUv()), cover * uAlpha);
}
