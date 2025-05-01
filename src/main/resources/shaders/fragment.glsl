#version 450

layout(location = 0) in vec2 texCoords;

layout(location = 0) out vec4 uFragColor;

layout(set = 6, binding = 0) uniform sampler2D textureSampler;
layout(set = 7, binding = 0) uniform sampler2D overlaySampler;

void main()
{
    uFragColor = texture(textureSampler, texCoords);
}