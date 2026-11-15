#version 330
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    float intensity = texture(Sampler0, texCoord0).r;
    float alpha = intensity >= 0.7 ? 1.0 : (intensity > 0.5 ? 0.6 : 0.35);
    float opacity = intensity * alpha * vertexColor.a;
    if (opacity < 0.01) discard;
    fragColor = vec4(vertexColor.rgb, opacity);
}
