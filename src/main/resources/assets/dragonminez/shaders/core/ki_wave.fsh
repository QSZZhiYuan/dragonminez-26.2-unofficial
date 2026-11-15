#version 330

#moj_import <minecraft:globals.glsl>

in vec3 vNormal;
in vec3 vViewDir;
in vec3 vLocalPos;
flat in vec3 colorCore;
flat in vec3 colorBorder;
flat in vec3 colorOutline;
flat in float alphaMult;
out vec4 fragColor;

float flameField(vec3 p, float t, float freq, float speed) {
    float v = sin(p.x * 9.0 * freq + t * 4.0 * speed) * 0.50;
    v += sin(p.y * 11.0 * freq - t * 5.5 * speed + p.x * 3.0) * 0.32;
    v += sin(p.z * 13.0 * freq + t * 6.5 * speed + p.y * 4.0) * 0.24;
    v += sin((p.x + p.y + p.z) * 19.0 * freq - t * 8.0 * speed) * 0.16;
    return v;
}

void main() {
    float time = GameTime * 1200.0;
    float flameCore = flameField(vLocalPos, time, 1.35, 1.30);
    float flameBorder = flameField(vLocalPos, time, 1.00, 0.95);
    float flameOutline = flameField(vLocalPos, time, 0.65, 0.65);
#ifdef KI_DISK
    float g = 1.0 - clamp(length(vLocalPos.xy), 0.0, 1.0);
#else
    float f = clamp(abs(dot(normalize(vViewDir), normalize(vNormal))), 0.0, 1.0);
    float g = 1.0 - sqrt(max(0.0, 1.0 - f * f));
#endif
    vec3 outline = colorOutline;
    if (min(min(outline.r, outline.g), outline.b) > 0.86) {
        outline = clamp(colorBorder * 1.55 + 0.12, 0.0, 1.0);
    }
    float toBorder = smoothstep(0.10, 0.26, g + flameOutline * 0.07);
    float toCore = smoothstep(0.26, 0.58, g + flameCore * 0.17);
    vec3 color = mix(outline, colorBorder, toBorder);
    color = mix(color, colorCore, toCore);
    color *= mix(1.0 + flameBorder * 0.10, 1.0 + flameCore * 0.22, toCore);
#ifdef KI_DISK
    float alpha = alphaMult * smoothstep(0.0, 0.05, g);
#else
    float alpha = alphaMult * smoothstep(0.0, 0.05, g + flameOutline * 0.07);
#endif
    if (alpha < 0.01) discard;
    fragColor = vec4(color, alpha);
}
