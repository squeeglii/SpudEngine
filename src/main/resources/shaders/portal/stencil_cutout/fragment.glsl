#version 450

#define IS_CUTOUT_THRESHOLD < 0.05

layout(location = 0) in vec2 texCoords;
layout(location = 1) in vec2 overlayTexCoords;
layout(location = 2) in vec4 portalColour;
layout(location = 3) in vec4 debugPos;

layout(location = 0) out vec4 uFragColor;

layout(set = 6, binding = 0) uniform sampler2D textureSampler;
layout(set = 7, binding = 0) uniform sampler2D overlaySampler;

// Cutout, but it doesn't utilise the overlays at all.
void main() {
    vec4 col = texture(textureSampler, texCoords);

    if (col.a IS_CUTOUT_THRESHOLD) {
        //gl_FragDepth = 0;
        discard;
    }

    uFragColor = col;
}