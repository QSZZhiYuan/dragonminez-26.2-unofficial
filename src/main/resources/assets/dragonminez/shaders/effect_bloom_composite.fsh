#version 330
uniform sampler2D DiffuseSampler;
layout(std140) uniform DmzPostParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec3 tonemap(vec3 color) {
    float luminance = dot(color, LUMA);
    vec3 compressed = color / (color + 1.0);
    return mix(color / (luminance + 1.0), compressed, compressed);
}

void main() {
    vec3 emission = texture(DiffuseSampler, texCoord).rgb;
    // The capture target contains emission, not scene coverage. Its alpha must never
    // attenuate the world, including where a translucent shell has a black interior.
    fragColor = vec4(tonemap(emission * Params.w), 0.0);
}
