#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
out vec2 texCoord0;
out vec4 shellColor;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    int rgb = UV1.y & 65535;
    shellColor = vec4(float((rgb >> 11) & 31) / 31.0, float((rgb >> 5) & 63) / 63.0,
                      float(rgb & 31) / 31.0, clamp(float(UV1.x) / 1000.0, 0.0, 1.0));
}
