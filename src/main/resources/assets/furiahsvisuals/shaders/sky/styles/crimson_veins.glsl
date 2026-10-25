// @colors FF2424 5C0008
// Crimson Veins: glowing red threads of liquid swirling fast over a dark red night, with hot white cores.
vec3 skyColor(vec3 d) {
    vec3 p = d * (1.35 / uScale) + uParallax * 0.25;
    p.x -= uTime * 0.04;
    p = stir(p, uTime * 0.18, 0.2, 0.065);
    float s = p.y * 4.2 + p.x * 0.6;
    float id = floor(s);
    float x = fract(s) - 0.5;
    float keep = step(0.32, hash12(vec2(id, 11.0)));
    float w = 0.02 + 0.02 * hash12(vec2(id, 5.0));
    float core = exp(-x * x / (w * w * 0.25));
    float body = exp(-x * x / (w * w));
    float halo = exp(-x * x / (w * w * 16.0));
    float pulse = 0.7 + 0.3 * sin(p.x * 2.5 + uTime * 0.9 + id * 2.3);
    vec3 col = (vec3(1.0, 0.85, 0.85) * core + uColA * body * 0.95 + uColA * halo * 0.55) * keep * pulse;
    // Deep red haze around the threads, so the dark between them is never flat.
    col += uColB * 0.35 * smoothstep(0.4, 0.0, abs(x)) * (0.35 + 0.65 * keep);
    return col;
}
