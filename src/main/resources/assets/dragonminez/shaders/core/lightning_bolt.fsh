#version 330
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    float across = abs(texCoord0.x);
    float soft = max(fwidth(across), 0.02);
    float core = 1.0 - smoothstep(0.34 - soft, 0.34 + soft, across);
    float outline = 1.0 - smoothstep(0.58 - soft, 0.58 + soft, across);
    float halo = 1.0 - smoothstep(0.58, 1.0, across);
    float alpha = max(outline, halo * halo * 0.55) * vertexColor.a;
    if (alpha <= 0.004) discard;
    vec3 coreColor = mix(vertexColor.rgb, vec3(1.0), 0.85);
    fragColor = vec4(mix(vertexColor.rgb, coreColor, core), clamp(alpha, 0.0, 1.0));
}
