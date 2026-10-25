#version 120
// Shared by the composite and the tint pass: the view model's shape, where a pixel sits along the color gradient,
// and the colors.
uniform sampler2D sLut;      // 64 x 2: row 0 the glow color from t = 0 to 1, row 1 the rim color
uniform sampler2D sShape;    // 2 x 1: the view model's moments (moments.fsh, total.fsh) when uHasShape is 1
uniform float uHasShape;
uniform vec2 uScreen;        // screen size, pixels
uniform vec2 uCenter;        // without the moments: the view model's projected middle, pixels (y up)
uniform int uDirection;      // 0 edge -> outward, 1 vertical, 2 horizontal, 3 diagonal, 4 radial, 5 around
uniform float uMirror;       // around: 1 = A -> B -> A (gradients, no seam), 0 = once around (chroma)

// The view model as it is on screen now: its center (pixels), long axis (unit vector, pointing up) and length along
// that axis (pixels), from the moments of its mask (the center of mass and the covariance's main direction).
void itemShape(out vec2 center, out vec2 axis, out float len) {
    center = uCenter;
    axis = vec2(0.70710678, 0.70710678);
    len = 0.55 * uScreen.y;
    if (uHasShape < 0.5) {
        return;
    }
    vec4 a = texture2D(sShape, vec2(0.25, 0.5));
    vec4 b = texture2D(sShape, vec2(0.75, 0.5));
    if (a.x < 0.5) {
        return;
    }
    vec2 c = a.yz / a.x;
    float cxx = (a.w / a.x - c.x * c.x) * uScreen.x * uScreen.x;
    float cyy = (b.x / a.x - c.y * c.y) * uScreen.y * uScreen.y;
    float cxy = (b.y / a.x - c.x * c.y) * uScreen.x * uScreen.y;
    center = c * uScreen;
    float tr = cxx + cyy;
    float det = cxx * cyy - cxy * cxy;
    float l1 = 0.5 * tr + sqrt(max(0.25 * tr * tr - det, 0.0));
    vec2 v = abs(cxy) > 1.0e-3 ? vec2(l1 - cyy, cxy) : (cxx >= cyy ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    axis = normalize(v);
    if (axis.y < 0.0) {
        axis = -axis;
    }
    // A bar of length L has a variance of L^2 / 12 along it; 4 sigma reaches a little past both ends.
    len = max(4.0 * sqrt(max(l1, 1.0)), 0.1 * uScreen.y);
}

// Position along the gradient (0 = color A, 1 = color B) of the pixel at px; edgeT is how far out it is from the
// silhouette (0 on it, 1 where the glow ends), used by edge -> outward. The other directions run across the item.
float gradientAt(vec2 px, float edgeT, vec2 center, float len) {
    vec2 d = px - center;
    if (uDirection == 0) {
        return edgeT;
    }
    if (uDirection == 1) {
        return clamp(0.5 + d.y / len, 0.0, 1.0);                       // A at the bottom, B at the top
    }
    if (uDirection == 2) {
        return clamp(0.5 + d.x / len, 0.0, 1.0);                       // A on the left, B on the right
    }
    if (uDirection == 3) {
        return clamp(0.5 + (d.x + d.y) * 0.70710678 / len, 0.0, 1.0);   // A bottom left, B top right
    }
    if (uDirection == 4) {
        return clamp(length(d) / (0.65 * len), 0.0, 1.0);             // A at the center, B outwards
    }
    float a = fract(atan(d.y, d.x + 1.0e-4) / 6.2831853 + 1.0);         // around the item
    return uMirror > 0.5 ? 1.0 - abs(a * 2.0 - 1.0) : a;
}

vec4 lut(float t, float row) {
    return texture2D(sLut, vec2(clamp(t, 0.0, 1.0) * (63.0 / 64.0) + 0.5 / 64.0, row));
}
