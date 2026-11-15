layout(std140) uniform AuraBorderParams {
    vec4 BorderScreenCenter;
    vec4 BorderUpThicknessId;
    vec4 BorderClockAlphaFootprint;
    vec4 BorderColorPass;
    vec4 BorderInnerDark;
    vec4 BorderDepthBloom;
    vec4 BorderBlurDepthRange;
};
#define ScreenSize BorderScreenCenter.xy
#define Center BorderScreenCenter.zw
#define Up BorderUpThicknessId.xy
#define Thickness BorderUpThicknessId.z
#define Id BorderUpThicknessId.w
#define Time BorderClockAlphaFootprint.x
#define Seed BorderClockAlphaFootprint.y
#define Alpha BorderClockAlphaFootprint.z
#define Footprint BorderClockAlphaFootprint.w
#define Color BorderColorPass.rgb
#define BloomPass BorderColorPass.a
#define InnerColor BorderInnerDark.rgb
#define Dark BorderInnerDark.a
#define DepthParams BorderDepthBloom.xyz
#define BloomIntensity BorderDepthBloom.a
#define Direction BorderBlurDepthRange.xy
#define Radius BorderBlurDepthRange.z
