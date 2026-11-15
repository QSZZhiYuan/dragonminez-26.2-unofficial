#version 330
uniform sampler2D Sampler0;
layout(std140) uniform MaskColor { vec4 PackedColors; };
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) discard;
    fragColor = PackedColors;
}
