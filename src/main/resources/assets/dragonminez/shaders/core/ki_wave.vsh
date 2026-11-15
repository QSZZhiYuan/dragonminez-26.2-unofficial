#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 vNormal;
out vec3 vViewDir;
out vec3 vLocalPos;
flat out vec3 colorCore;
flat out vec3 colorBorder;
flat out vec3 colorOutline;
flat out float alphaMult;

vec3 unpackPalette(ivec2 encoded) {
    return vec3(encoded.y & 255, (encoded.x >> 8) & 255, encoded.x & 255) / 255.0;
}

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vNormal = normalize(mat3(ModelViewMat) * Normal);
    vViewDir = -viewPos.xyz;
    int localZ = ((UV1.y >> 8) & 255) | (((UV2.y >> 8) & 255) << 8);
    vLocalPos = vec3(UV0, float(localZ) / 65535.0 * 2.0 - 1.0);
    colorCore = Color.rgb;
    colorBorder = unpackPalette(UV1);
    colorOutline = unpackPalette(UV2);
    alphaMult = Color.a;
}
