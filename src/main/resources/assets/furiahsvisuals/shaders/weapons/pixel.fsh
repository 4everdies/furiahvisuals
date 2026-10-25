#version 120
// Furiah's Visuals, Weapons: the Pixelated weapons' fragment shader. A face's color is its palette slot's, from the
// look's palette (uPalette: one column per slot; rows for the finish's hues and for colors running along the weapon),
// stepped by whole shades for the finish's pattern at the pixel it is in, then lit like a held item: the face's item
// lighting times the light where the weapon is (uLight), so it darkens in caves and at night. Glowing slots are full
// bright. Every pattern works on whole pixels, so pixels stay hard. Output is premultiplied (like weapon.fsh).
//
// Slots: 1 + material * 5 + shade (0 darkest .. 4 highlight); the palette's flags row says each slot's material, shade,
// alpha and whether it glows. Materials: 0 blade, 1 cutting edge, 2 metal, 3 handle, 4 accent, 5 energy, 6 paint,
// 7 chain, 8 and on the weapon's own colors.

uniform sampler2D uPalette;
uniform sampler2D uGlintTex;
// The finish's pattern: 0 plain, 1 brushed, 2 damascus, 3 nebula, 4 prism, 5 obsidian, 6 void, 7 hard light, 8 ice,
// 9 infernal, 10 bone, 11 dragonscale, 12 moonsilver, 13 living wood.
uniform float uFinish;
// The handle's material: 1 leather, 2 wood, 3 rubber, 4 cord, 5 metal.
uniform float uHandle;
// The weapon's extent along its length (pixels), for colors that run along it.
uniform vec2 uAlong;
uniform float uPixel;
uniform float uTime;
uniform float uWear;
uniform float uSeed;
// 0 no glint, 1 the pixel sheen, 2 vanilla's glint.
uniform float uSheen;
// Edge check's glint along the cutting edge (0..1; below 0: none).
uniform float uGlint;
// Glowing pixels are lit up to this height (model units); how much brighter they flare.
uniform float uReveal;
uniform float uFlare;
uniform float uGlow;
uniform float uAlpha;
uniform float uFog;
// 0 solid faces, 1 see-through faces, 2 glow halos.
uniform float uPass;
// Reduce Flashing: twinkles and flickers step more slowly.
uniform float uCalm;
uniform vec3 uLight;
// A saw chain (uChain): its pixels alternate dark and light along the chain's path round the bar (uChainPath: the
// bar's middle x, the drive end's and the nose's y, the path's radius; model units), uChainPhase further along, so the
// chain runs in whole-pixel steps (four times more slowly with Reduce Flashing).
uniform float uChain;
uniform vec4 uChainPath;
uniform float uChainPhase;
// Heat (the Thermal Machete, 0..1): the cutting edge's pixels glow in three steps, orange, yellow, then white, by how
// hot it is; its inner pixels (the edge's darker shades) a step behind, so the glow runs into the blade as a band; and
// now and then a pixel flickers down a step (whole steps, a few times a second; none with Reduce Flashing): in
// Pixelated that carries the heat in place of the shimmer.
uniform float uHeat;
// The heat the weapon has at rest (0: it doesn't heat), and its core (xyz, model units) with the reach of the fire
// from it (w; 0: no core): an Infernal blade catches from the core outward as it heats up to its resting heat.
uniform float uHeatIdle;
uniform vec4 uCore;
// A blade of light growing a pixel at a time: pixels past {axis, limit} (model units) aren't drawn.
uniform vec4 uClip;

varying vec3 vObj;
varying vec3 vNObj;
varying vec3 vNEye;
varying vec3 vPos;
varying float vShade;
varying vec2 vData;

// A 0..1 value for a whole-numbered pixel (large multipliers: whole numbers in a row don't repeat; two rounds, so short
// runs mix as well as long ones). PixelPalette.hash is its twin.
float hash(vec3 q) {
    vec3 p = fract(q * vec3(443.8975, 397.2973, 491.1871));
    p = fract(fract(p + dot(p, p.yzx + 19.19)) * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

vec4 pal(float slot, float row) {
    return texture2D(uPalette, vec2((slot + 0.5) / 128.0, (row + 0.5) / 48.0));
}

// An edge pixel's heat step (0 orange, 1 yellow, 2 white; below 0 not glowing) by its slot's shade; flicker: with the
// pixel's own flicker (its halo keeps the steady step).
float heatStep(float shade, vec3 px, bool flicker) {
    float s = floor(clamp(uHeat, 0.0, 0.999) * 3.0) - (shade < 2.5 ? 1.0 : 0.0);
    // (Only yellow and white flicker: an orange pixel never drops out of the glow.)
    if (flicker && s > 0.5 && uCalm < 0.5 && hash(vec3(px.y, px.x * 17.0 + px.z, floor(uTime * 6.0) + 61.0)) > 0.84) {
        s -= 1.0;
    }
    return s;
}

vec3 heatColor(float hs) {
    return hs < 0.5 ? vec3(1.0, 0.42, 0.06) : hs < 1.5 ? vec3(1.0, 0.78, 0.18) : vec3(1.0, 0.97, 0.86);
}

// How hot the weapon is against its resting heat (1 at rest; 2 white-hot; 0 cold).
float hotness() {
    return uHeatIdle > 0.0 ? uHeat / uHeatIdle : 1.0;
}

// Infernal: how far a pixel is past the fire's front (a share of the core's reach; above 0: not caught yet).
float pastFront(vec3 px) {
    if (uCore.w <= 0.0) {
        return -1.0;
    }
    return length((px + 0.5) * uPixel - uCore.xyz) / uCore.w - hotness() * 1.08;
}

// The handle materials' patterns, as a shade step at a face pixel (u across the handle, v along it).
float handleShade(vec2 uv) {
    if (uHandle < 1.5) {
        // Leather: diagonal wrap bands.
        return mod(uv.x + uv.y, 3.0) < 0.5 ? -1.0 : 0.0;
    }
    if (uHandle < 2.5) {
        // Wood: grain streaks along it.
        return hash(vec3(uv.x, floor(uv.y / 3.0), 5.0)) > 0.68 ? -1.0 : 0.0;
    }
    if (uHandle < 3.5) {
        // Rubber: grooves across it.
        return mod(uv.y, 2.0) < 0.5 ? -1.0 : 0.0;
    }
    if (uHandle < 4.5) {
        // Cord: two crossing cords make diamonds.
        return mod(uv.x + uv.y, 4.0) < 0.5 || mod(uv.x - uv.y, 4.0) < 0.5 ? -1.0 : 1.0;
    }
    // Metal: a fine knurl.
    return mod(uv.x + uv.y, 2.0) < 0.5 ? 0.0 : -1.0;
}

void main() {
    if (dot(vObj, uClip.xyz) > uClip.w + uPixel * 0.001) {
        discard;
    }
    float slot = floor(vData.x + 0.5);
    float flags = floor(vData.y + 0.5);
    float edge = mod(flags, 2.0);
    // A face along its sprite's own facing (not a side): stars and the like only show there.
    bool front = mod(floor(flags / 32.0), 2.0) > 0.5;
    vec3 halfOff = vec3(mod(floor(flags / 2.0), 2.0), mod(floor(flags / 4.0), 2.0), mod(floor(flags / 8.0), 2.0));
    // The pixel this fragment is in (on its layer's own grid), taken a little inside the face.
    vec3 px = floor(vObj / uPixel - vNObj * 0.25 - halfOff * 0.5);
    vec4 info = pal(slot, 1.0);
    float glowing = info.a;
    float alpha = info.r;
    float mat = floor(info.g * 255.0 + 0.5);
    float shade = floor(info.b * 255.0 + 0.5);
    float base = slot - shade;
    float k = shade;
    float span = max(1.0, uAlong.y - uAlong.x);
    float along = clamp((px.y + 0.5 - uAlong.x) / span, 0.0, 0.999);
    float row = 16.0 + floor(along * 32.0);
    // The face's own two axes.
    vec2 uv = abs(vNObj.z) > 0.5 ? px.xy : (abs(vNObj.x) > 0.5 ? px.zy : px.xz);
    // How squarely the face is turned to the eye, in whole steps (the shifting finishes step with it).
    float turn = floor(abs(dot(normalize(vNEye), normalize(-vPos))) * 2.99);
    float flash = 0.0;
    bool infernal = uFinish > 8.5 && uFinish < 9.5;

    if (uPass > 1.5) {
        // A glow halo: the glowing pixel's color at half strength, added (a hot edge's, its heat's: only round the outer
        // pixels, so the band stays two pixels wide, and none while it is too cold to glow; an Infernal blade's none
        // where it hasn't caught, nor a core before it does, nor energy the reveal hasn't lit yet).
        vec3 h = pal(slot, row).rgb;
        if (infernal && mat < 1.5) {
            h = pastFront(px) > 0.0 ? vec3(0.0) : h;
        } else if (uHeat > 0.0 && mat > 0.5 && mat < 1.5) {
            float hs = heatStep(shade, px, false);
            h = hs < 0.0 || shade < 2.5 ? vec3(0.0) : heatColor(hs);
        } else if (mat > 4.5 && mat < 5.5 && (uCore.w > 0.0 && hotness() < 0.02 || (px.y + 0.5) * uPixel > uReveal)) {
            h = vec3(0.0);
        }
        gl_FragColor = vec4(h * 0.5 * uGlow * uAlpha, 0.0);
        return;
    }

    if (mat < 1.5) {
        if (uFinish > 0.5 && uFinish < 1.5) {
            // Brushed: 1-pixel streaks along the blade.
            if (hash(vec3(px.x * 7.0 + px.z, floor(px.y / 4.0), 11.0)) > 0.72) {
                k += 1.0;
            }
        } else if (uFinish > 1.5 && uFinish < 2.5) {
            // Damascus: wavy bands in two alternating shades.
            float wave = px.y + floor(1.6 * sin(px.x * 0.9 + px.y * 0.25) + 0.5);
            if (mod(floor(wave / 2.0), 2.0) > 0.5) {
                k -= 1.0;
            }
        } else if (uFinish > 2.5 && uFinish < 3.5) {
            // Nebula: indigo to magenta in steps along the blade, the steps shifting as it turns; a few star pixels
            // twinkle (one step in four dimmer).
            row = 16.0 + mod(floor(along * 32.0) + turn * 8.0, 32.0);
            if (mat < 0.5 && front && hash(vec3(px.x, px.y * 31.0 + px.z, 23.0)) > 0.965) {
                row = 13.0;
                k = 4.0;
                glowing = 1.0;
                float tw = floor(uTime * (uCalm > 0.5 ? 1.5 : 3.0) + hash(px) * 7.0);
                if (mod(tw, 4.0) < 0.5) {
                    k = 2.0;
                }
            }
        } else if (uFinish > 3.5 && uFinish < 4.5) {
            // Prism: diagonal bands stepping through six hues, shifting a band as it turns.
            row = 2.0 + mod(floor((uv.x + uv.y) / 3.0) + turn, 6.0);
        } else if (uFinish > 4.5 && uFinish < 5.5) {
            // Obsidian: black glass with deep purple on its highlights and a few purple pixels.
            if (k > 2.5 || hash(vec3(px.x, px.y * 31.0 + px.z, 29.0)) > 0.93) {
                row = 12.0;
            }
        } else if (uFinish > 5.5 && uFinish < 6.5) {
            // Void: dark energy flickering in steps (the edge line glows, see the palette's flags).
            if (mat < 0.5 && hash(vec3(px.x, px.y, floor(uTime * (uCalm > 0.5 ? 2.0 : 5.0)))) > 0.88) {
                k += 1.0;
            }
        } else if (uFinish > 7.5 && uFinish < 8.5 && uReveal < 9000.0) {
            // Ice regrowing along its edges (Frost Breath): the edge's pixels past the reveal's front (ragged by a
            // pixel) aren't there yet, and the row at the front glints.
            float cy = (px.y + 0.5) * uPixel;
            float front = uReveal + (hash(vec3(px.x, px.z, 71.0)) - 0.5) * uPixel;
            if (mat > 0.5 && cy > front) {
                discard;
            }
            if (abs(cy - front) < uPixel) {
                k = 4.0;
                glowing = 1.0;
            }
        } else if (infernal) {
            // Infernal: molten pixels; lighter heat-line pixels step down the blade a pixel at a time (palette
            // cycling; more slowly with Reduce Flashing), up to two steps brighter as it flares past its resting heat.
            // Past the fire's front (moving out from the core as the weapon heats) it is blackened steel; the front's
            // own pixels are white-hot as it passes.
            float lines = px.y + floor(px.x * 0.5) + floor(hash(vec3(px.x, 0.0, 83.0)) * 3.0) + floor(uTime * (uCalm > 0.5 ? 2.0 : 4.0));
            if (mod(lines, 7.0) < 0.5) {
                k += 1.0;
            }
            float hot = hotness();
            k += floor(clamp(hot - 1.0, 0.0, 1.0) * 2.0 + 0.25);
            float past = pastFront(px);
            if (past > 0.0) {
                row = 14.0;
                k = shade;
                glowing = 0.0;
            } else if (hot < 0.999 && past > -1.5 * uPixel / max(uCore.w, 0.001)) {
                k = 4.0;
            }
        } else if (uFinish > 9.5 && uFinish < 10.5) {
            // Bone: fine cracks, short diagonal runs of pixels a shade darker (PixelPalette's twin).
            if (mat < 0.5 && hash(vec3(px.x - px.y, floor((px.x + px.y) / 4.0), 91.0)) > 0.93) {
                k -= 1.0;
            }
        } else if (uFinish > 10.5 && uFinish < 11.5 && mat < 0.5) {
            // Dragonscale: rows of scales two pixels tall, staggered two pixels, four across: each scale's rim lit (the
            // middle two pixels of its lower row), a dark seam pixel above where two scales meet (PixelPalette's twin).
            // The gold trim (the edge's pixels) stays plain.
            float srow = floor(px.y / 2.0);
            float lx = mod(px.x + mod(srow, 2.0) * 2.0, 4.0);
            if (mod(px.y, 2.0) < 0.5) {
                if (lx > 0.5 && lx < 2.5) {
                    k += 1.0;
                }
            } else if (lx < 0.5) {
                k -= 1.0;
            }
        } else if (uFinish > 11.5 && uFinish < 12.5 && mat < 0.5) {
            // Moonsilver: a star pixel here and there on the faces, in the accent's light (the palette's moon row),
            // each twinkling on its own (two steps dimmer now and then, more slowly with Reduce Flashing; the brightest
            // few flash white); more of them light up as the weapon flares (Moonrise). PixelPalette's twin.
            float h = hash(vec3(px.x, px.y * 31.0 + px.z, 37.0));
            if (front && h > 0.94 - 0.07 * clamp(uFlare, 0.0, 1.0)) {
                row = 15.0;
                k = h > 0.975 ? 4.0 : 3.0;
                glowing = 1.0;
                float tw = floor(uTime * (uCalm > 0.5 ? 1.0 : 2.5) + hash(px) * 9.0);
                if (mod(tw, 5.0) < 0.5) {
                    k -= 2.0;
                }
            }
        } else if (uFinish > 12.5 && uFinish < 13.5 && mat < 0.5) {
            // Living wood: grain streaks a shade lighter winding up the bark; moss in clumps (the "edge" ramp, the moss's
            // greens); now and then a crack pixel glowing in the accent's light (the palette's moon row), more of them as
            // the weapon flares (Bloom). PixelPalette's twin.
            if (hash(vec3(px.x + px.z - px.y, floor(px.y / 3.0), 53.0)) > 0.78) {
                k += 1.0;
            }
            float clump = hash(vec3(floor(px.x / 2.0) + px.z * 3.0, floor(px.y / 2.0), 61.0));
            if (clump > 0.86) {
                base = 6.0;
                k = clump > 0.93 ? 3.0 : 2.0;
            } else if (front && hash(vec3(px.x, px.y * 31.0 + px.z, 67.0)) > 0.965 - 0.06 * clamp(uFlare, 0.0, 1.0)) {
                row = 15.0;
                k = 2.0;
                glowing = 1.0;
            }
        }
        // Edge check: a glint steps along the cutting edge.
        if (uGlint >= 0.0 && (mat > 0.5 || edge > 0.5)) {
            float gy = mix(uAlong.x, uAlong.y, uGlint);
            if (abs(px.y + 0.5 - gy) < 1.5) {
                k = 4.0;
                flash = 0.35;
            }
        }
    } else if (mat > 2.5 && mat < 3.5) {
        k += handleShade(uv);
    } else if (mat > 6.5 && mat < 7.5 && uChain > 0.5) {
        // How far along the chain's path this pixel's middle is: up the cutting side, round the nose, back down the
        // top side, round the drive end.
        vec2 c = (px.xy + 0.5 + halfOff.xy * 0.5) * uPixel;
        float x0 = uChainPath.x, y0 = uChainPath.y, y1 = uChainPath.z, r = uChainPath.w;
        float run = y1 - y0;
        float s;
        if (c.y > y1) {
            s = run + atan(c.y - y1, c.x - x0) * r;
        } else if (c.y < y0) {
            s = 2.0 * run + 3.14159265 * r + atan(y0 - c.y, x0 - c.x) * r;
        } else if (c.x > x0) {
            s = c.y - y0;
        } else {
            s = run + 3.14159265 * r + (y1 - c.y);
        }
        float phase = uCalm > 0.5 ? floor(uChainPhase / uPixel * 0.25) * uPixel : uChainPhase;
        if (mod(floor((s - phase) / uPixel), 2.0) > 0.5) {
            k -= 1.0;
        }
    } else if (mat > 4.5 && mat < 5.5) {
        // Energy: dark above the reveal's front (runes waking one by one) and a core while its weapon is cold, brighter
        // as it flares.
        if ((px.y + 0.5) * uPixel > uReveal || uCore.w > 0.0 && hotness() < 0.02) {
            k = 0.0;
            glowing = 0.0;
        } else {
            k += floor(uFlare * 2.0 + 0.5);
        }
    }

    // Wear: a few silhouette pixels scuffed a shade lighter, a few nicked dark.
    if (edge > 0.5 && uWear > 0.0 && mat < 2.5) {
        float h = hash(vec3(px.x * 13.0 + px.z, px.y, 41.0 + uSeed));
        if (h < uWear * 0.35) {
            k += 1.0;
        } else if (h > 1.0 - uWear * 0.12) {
            k = 0.0;
        }
    }
    // Enchanted, the pixel sheen: a band four pixels wide steps across, one shade lighter.
    if (uSheen > 0.5 && uSheen < 1.5) {
        float band = mod(floor(uTime * (uCalm > 0.5 ? 6.0 : 12.0)), span + 12.0) - 6.0 + uAlong.x;
        float d = px.y + px.x * 0.5 - band;
        if (d >= 0.0 && d < 4.0) {
            k += 1.0;
        }
    }
    k = clamp(k, 0.0, 4.0);
    vec3 col = pal(base + k, row).rgb;
    if (uHeat > 0.0 && mat > 0.5 && mat < 1.5 && !infernal) {
        float hs = heatStep(shade, px, true);
        if (hs >= 0.0) {
            col = heatColor(hs);
            glowing = 1.0;
        }
    }
    vec3 lit = glowing > 0.5 ? col * max(1.0, uGlow) : col * vShade * uLight;
    lit += vec3(flash);
    if (uSheen > 1.5) {
        // Vanilla's glint over the pixels: its texture taken at each pixel's middle, in its two moving layers (turned
        // -50 and 10 degrees, sliding one way in 3 s and the other in 4.873 s), each tinted and added as vanilla adds
        // it (the tinted color times itself). One turn of the texture spans 128 pixels, about vanilla's on an item.
        vec2 g = (uv + 0.5) / 128.0;
        float t1 = mod(uTime, 3.0) / 3.0;
        float t2 = mod(uTime, 4.873) / 4.873;
        vec2 a = vec2(g.x * 0.6428 + g.y * 0.7660, -g.x * 0.7660 + g.y * 0.6428) + vec2(t1, 0.0);
        vec2 b = vec2(g.x * 0.9848 - g.y * 0.1736, g.x * 0.1736 + g.y * 0.9848) - vec2(t2, 0.0);
        vec3 ga = texture2D(uGlintTex, a).rgb * vec3(0.5, 0.25, 0.8);
        vec3 gb = texture2D(uGlintTex, b).rgb * vec3(0.5, 0.25, 0.8);
        lit += ga * ga + gb * gb;
    }
    if (uFog > 0.5) {
        float f = clamp((gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale, 0.0, 1.0);
        lit = mix(gl_Fog.color.rgb, lit, f);
    }
    float a = (uPass > 0.5 ? alpha : 1.0) * uAlpha;
    gl_FragColor = vec4(lit * a, a);
}
