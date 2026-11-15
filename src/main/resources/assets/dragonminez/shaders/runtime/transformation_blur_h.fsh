#version 330
layout(std140) uniform OutlineParams { vec4 O0; vec4 O1; vec4 O2; };
#define AnimationTime O0.x
#define BloomRadius O0.y
#define BloomStrength O0.z
#define GlowStrength O0.w
#define OutlineBloomStrength O1.x
#define OutlineRadius O1.y
#define FireIntensity O1.z
#define ColorMode O1.w
#define BodyGlow O2.x
#define ColorChannel O2.y
#define InSize vec2(textureSize(DiffuseSampler, 0))
#define OutSize vec2(textureSize(DiffuseSampler, 0))


uniform sampler2D DiffuseSampler;


in vec2 texCoord;
out vec4 fragColor;

const int MAX_SAMPLES = 6;

void main() {
    vec2 texel = vec2(1.0 / max(InSize.x, 1.0), 0.0);
    float sampleRadius = max(0.0, BloomRadius);
    if (sampleRadius <= 0.001) {
        fragColor = texture(DiffuseSampler, clamp(texCoord, vec2(0.0), vec2(1.0)));
        return;
    }

    float sigma = max(0.001, sampleRadius * 0.5);
    float invSigma2 = 1.0 / (2.0 * sigma * sigma);

    vec4 sum = vec4(0.0);
    float weightSum = 0.0;

    float stride = max(1.0, sampleRadius / float(MAX_SAMPLES));

    for (int i = -MAX_SAMPLES; i <= MAX_SAMPLES; i++) {
        float fi = float(i) * stride;
        float weight = exp(-(fi * fi) * invSigma2);
        vec2 sampleUv = clamp(texCoord + texel * fi, vec2(0.0), vec2(1.0));
        sum += texture(DiffuseSampler, sampleUv) * weight;
        weightSum += weight;
    }

    fragColor = sum / max(weightSum, 0.0001);
}