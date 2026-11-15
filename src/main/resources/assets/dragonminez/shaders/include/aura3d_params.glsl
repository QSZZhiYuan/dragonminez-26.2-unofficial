layout(std140) uniform AuraParams {
    mat4 ModelViewMat;
    mat4 ProjMat;
    mat4 NormalTransform;
    vec4 SizeTime;
    vec4 PhaseGrowthPeaksFrequency;
    vec4 WaveNoiseBiasCoreAlpha;
    vec4 RimAlphaPowerThresholdNoise;
    vec4 AlphaBackBloomPass;
    vec4 SpikeParams;
    vec4 SpeedLayerHollowLayer;
    vec4 FacesSecondBorder;
    vec4 CoreColorValue;
    vec4 RimColorValue;
    vec4 NoiseColorValue;
    vec4 SecondBorderColorValue;
};
#define NormalMat mat3(NormalTransform)
#define Size SizeTime.xyz
#define Time SizeTime.w
#define Phase PhaseGrowthPeaksFrequency.x
#define Growth PhaseGrowthPeaksFrequency.y
#define Peaks PhaseGrowthPeaksFrequency.z
#define WaveFrequency PhaseGrowthPeaksFrequency.w
#define WaveAmplitude WaveNoiseBiasCoreAlpha.x
#define NoiseDetail WaveNoiseBiasCoreAlpha.y
#define UpwardBias WaveNoiseBiasCoreAlpha.z
#define CoreAlpha WaveNoiseBiasCoreAlpha.w
#define RimAlpha RimAlphaPowerThresholdNoise.x
#define RimPower RimAlphaPowerThresholdNoise.y
#define RimThreshold RimAlphaPowerThresholdNoise.z
#define NoiseFactor RimAlphaPowerThresholdNoise.w
#define Alpha AlphaBackBloomPass.x
#define BackFace AlphaBackBloomPass.y
#define BloomIntensity AlphaBackBloomPass.z
#define BloomPass AlphaBackBloomPass.w
#define SpikeDensity SpikeParams.x
#define SpikeBurstRate SpikeParams.y
#define SpikeRarity SpikeParams.z
#define BandStart SpikeParams.w
#define WaveSpeed SpeedLayerHollowLayer.x
#define LayerPass SpeedLayerHollowLayer.y
#define Hollow SpeedLayerHollowLayer.z
#define Layer SpeedLayerHollowLayer.w
#define Faces FacesSecondBorder.x
#define SecondBorderFactor FacesSecondBorder.y
#define CoreColor CoreColorValue.xyz
#define RimColor RimColorValue.xyz
#define NoiseColor NoiseColorValue.xyz
#define SecondBorderColor SecondBorderColorValue.xyz
