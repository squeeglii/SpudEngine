#version 450

layout(location = 0) out vec4 uFragColor;

layout(push_constant) uniform constants
{
    float time;
    int updateRate;
} pushConstants;

// https://gamedev.stackexchange.com/questions/59797/glsl-shader-change-hue-saturation-brightness
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}


void main()
{

    float updateFrac = pushConstants.time / pushConstants.updateRate;

    vec3 hsv = vec3(updateFrac / 10, 1, 1);

    uFragColor = vec4(hsv2rgb(hsv), 1);
}