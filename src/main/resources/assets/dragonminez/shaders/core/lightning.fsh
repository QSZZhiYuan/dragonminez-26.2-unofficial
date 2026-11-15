#version 330

in vec3 vNormalWorld;
in vec3 v_viewDir;
in vec2 vLocalXZ;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float vDistFromCenter = length(vLocalXZ);
    float distNorm = clamp(vDistFromCenter / 0.15, 0.0, 1.0);

    float coreFactor = 1.0 - smoothstep(0.0, 0.3, distNorm);
    float glowFactor = 1.0 - smoothstep(0.2, 1.0, distNorm);

    vec3 coreColor = mix(vertexColor.rgb, vec3(1.0), 0.7);
    vec3 finalColor = mix(vertexColor.rgb, coreColor, coreFactor);

    float finalAlpha = mix(0.0, vertexColor.a, glowFactor) * 1.5;

    if (finalAlpha <= 0.01) discard;

    fragColor = vec4(finalColor, clamp(finalAlpha, 0.0, 1.0));
}
