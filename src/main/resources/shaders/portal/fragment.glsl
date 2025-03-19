#version 450

#define IS_DEFINED_OVERLAY [0] > -512
#define BLACK 0.1
#define WHITE 0.9

layout(location = 0) in vec2 texCoords;
layout(location = 1) in vec2 overlayTexCoords;
layout(location = 2) in vec4 portalColour;
layout(location = 3) in vec4 debugColour;

layout(location = 0) out vec4 uFragColor;

layout(set = 4, binding = 0) uniform sampler2D textureSampler;
layout(set = 5, binding = 0) uniform sampler2D overlaySampler;

// https://gamedev.stackexchange.com/questions/59797/glsl-shader-change-hue-saturation-brightness
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}


void main()
{
    //uFragColor = vec4(gl_FragDepth, gl_FragDepth, gl_FragDepth, 1);

    vec4 col = texture(textureSampler, texCoords);

    if(overlayTexCoords IS_DEFINED_OVERLAY) {
        vec4 overlay = texture(overlaySampler, overlayTexCoords);

        //col = vec4(overlayTexCoords.x, overlayTexCoords.y, 0, 1);

        col.a = overlay.a;

        // if overlay is white, use this like a cutout mode?
        if (overlay.x < BLACK && overlay.y < BLACK && overlay.z < BLACK && col.a < 0.1) {
            discard;
        } else if (overlay.x > WHITE && overlay.y > WHITE && overlay.z > WHITE && col.a > 0.9) {
            col = portalColour;
        }
    }

    uFragColor = col; //* debugColour;
}