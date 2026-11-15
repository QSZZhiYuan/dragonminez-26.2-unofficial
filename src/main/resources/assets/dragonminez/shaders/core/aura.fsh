#version 330

#moj_import <minecraft:globals.glsl>

in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

uniform sampler2D Sampler0;

const float frameWidth = 1.0 / 4.0;

vec4 adjustColor(vec4 colorTex) {
    float intensity = colorTex.r;
    vec3 newColor = vec3(0.0);
    float alphaOut = 0.0;

    if (intensity >= 0.97) {
        newColor = vertexColor.rgb * 1.6;
        alphaOut = 1.0;
    } else if (intensity >= 0.7) {
        newColor = vertexColor.rgb * 1.3;
        alphaOut = 1.0;
    } else if (intensity > 0.5) {
        newColor = vertexColor.rgb;
        alphaOut = 0.85;
    } else if (intensity > 0.0) {
        newColor = vertexColor.rgb * 0.75;
        alphaOut = 0.65;
    }

    return vec4(newColor, alphaOut * intensity);
}

void main() {
    // Globals is bound by both the GUI and world passes. Ten sprite frames per second,
    // matching the original (ticks + partialTick) * 0.5, without an unbound material UBO.
    float speed = GameTime * 12000.0;
    // Spritesheet is 1 row x 4 columns; pick the current and next frame and blend between them.
    float currentFrameIdx = floor(mod(speed, 4.0));
    float nextFrameIdx = floor(mod(speed + 1.0, 4.0));

    vec2 currentUV = vec2((texCoord0.x * frameWidth) + (currentFrameIdx * frameWidth), texCoord0.y);
    vec2 nextUV = vec2((texCoord0.x * frameWidth) + (nextFrameIdx * frameWidth), texCoord0.y);

    vec4 currentFrameColor = adjustColor(texture(Sampler0, currentUV));
    vec4 nextFrameColor = adjustColor(texture(Sampler0, nextUV));

    vec4 finalColor = mix(currentFrameColor, nextFrameColor, fract(speed));

    if (finalColor.a * vertexColor.a < 0.01) {
        discard;
    }

    fragColor = vec4(finalColor.rgb, finalColor.a * vertexColor.a);
}
