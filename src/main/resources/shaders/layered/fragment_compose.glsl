#version 450

layout(location = 0) in vec2 texCoords;

layout(location = 0) out vec4 uFragColor;

layout(set = 0, binding = 0) uniform sampler2D frameBufferInSampler;


void main()
{
    uFragColor = texture(textureSampler, texCoords);
}