#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform sampler2D Sampler2;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;
out float progress;
out float mode;
out float shade;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    shade = clamp(0.6 + 0.2 * dot(normalize(Normal), normalize(vec3(0.2,1.0,-0.7))), 0.0, 1.0);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
    progress = clamp(float(UV1.x) / 1000.0, 0.0, 1.0);
    mode = float(UV1.y);
}
