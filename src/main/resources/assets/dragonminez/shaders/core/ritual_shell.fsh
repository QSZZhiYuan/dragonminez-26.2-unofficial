#version 330
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 shellColor;
out vec4 fragColor;
void main() {
    float coverage = texture(Sampler0, texCoord0).a;
    if (coverage < 0.1) discard;
    fragColor = vec4(shellColor.rgb, shellColor.a * coverage);
}
