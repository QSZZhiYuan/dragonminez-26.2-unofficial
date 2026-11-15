#version 330
#moj_import <minecraft:globals.glsl>
uniform sampler2D Sampler0;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;
in float progress;
flat in int bandPass;
out vec4 fragColor;
float hash(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) discard;
    float p = progress;
    float bandHeight = max(2.0, ScreenSize.y / 220.0);
    float row = floor(gl_FragCoord.y / bandHeight);
    float shuffle = floor(GameTime * 12000.0);
    float centerWeight = mix(0.72, 0.34, p);
    float pick = hash(row + shuffle * 17.0);
    float sideSplit = centerWeight + (1.0 - centerWeight) * 0.5;
    int band = pick < centerWeight ? 0 : (pick < sideSplit ? 1 : 2);
    if (band != bandPass) discard;
    float keep = hash(row * 3.7 + shuffle * 5.3 + 11.0);
    if (keep < p * p * 0.8) discard;
    color *= vertexColor * lightMapColor;
    color.rgb = mix(color.rgb, vec3(1.0), 0.12 + 0.38 * p);
    color.a *= 0.85 * (1.0 - smoothstep(0.45, 1.0, p));
    fragColor = color;
}
