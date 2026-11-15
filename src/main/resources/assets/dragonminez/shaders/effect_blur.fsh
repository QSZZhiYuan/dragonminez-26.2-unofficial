#version 330
uniform sampler2D DiffuseSampler;
layout(std140) uniform DmzPostParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 stepSize = Params.xy * Params.z / vec2(textureSize(DiffuseSampler, 0));
    vec4 sum = texture(DiffuseSampler, texCoord) * 0.227027;
    sum += texture(DiffuseSampler, texCoord + stepSize * 1.384615) * 0.316216;
    sum += texture(DiffuseSampler, texCoord - stepSize * 1.384615) * 0.316216;
    sum += texture(DiffuseSampler, texCoord + stepSize * 3.230769) * 0.070270;
    sum += texture(DiffuseSampler, texCoord - stepSize * 3.230769) * 0.070270;
    fragColor = sum;
}
