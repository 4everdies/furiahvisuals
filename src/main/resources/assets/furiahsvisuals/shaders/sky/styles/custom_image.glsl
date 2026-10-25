// @colors FFFFFF C8D2E6
// Custom Image: your own panorama (config/furiahsvisuals/sky.png, a 2:1 equirectangular image) wrapped around you.
vec3 skyColor(vec3 d) {
    if (uHasImage < 0.5) {
        // No image yet: a quiet grey-blue gradient.
        return gradient(d, vec3(0.2, 0.22, 0.27), vec3(0.05, 0.06, 0.09), 0.6);
    }
    // The image drifts slowly around you (the Rotation setting adds to it).
    float u = azimuth(d) / TAU + 0.5 + uTime * 0.002;
    float v = 0.5 - elevation(d) / PI;
    vec3 col = texture2D(uImage, vec2(u, v)).rgb;
    return col * mix(vec3(1.0), uColA, uCustom);
}
