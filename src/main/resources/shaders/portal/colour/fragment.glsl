#version 450

#define IS_DEFINED_OVERLAY [0] > -512
#define IS_CUTOUT_THRESHOLD < 0.05
#define BLACK 0.1
#define WHITE 0.9

layout(location = 0) in vec2 texCoords;
layout(location = 1) in vec2 overlayTexCoords;
layout(location = 2) in vec4 portalColour;
layout(location = 3) in vec4 debugPos;

layout(location = 0) out vec4 uFragColor;

layout(set = 5, binding = 0) uniform sampler2D textureSampler;
layout(set = 6, binding = 0) uniform sampler2D overlaySampler;

void main() {
    vec4 col = texture(textureSampler, texCoords);

    if (overlayTexCoords IS_DEFINED_OVERLAY) {
        vec4 overlay = texture(overlaySampler, overlayTexCoords);

        // if overlay irs black, use it like a cutout mask.
        if (overlay.x < BLACK && overlay.y < BLACK && overlay.z < BLACK && overlay.a IS_CUTOUT_THRESHOLD) {
            discard;

        // use overlay, but tint it.
        } else if (overlay.a > 0.9) {
            col.rgb = overlay.rgb * portalColour.rgb;
        }
    }

    // Traditional cutout
    if (col.a IS_CUTOUT_THRESHOLD) {
        //gl_FragDepth = 0;
        discard;
    }

    uFragColor = col;
}