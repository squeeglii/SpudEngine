#version 450

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;

layout(location = 0) out vec2 texCoordsOut;

layout(set = 0, binding = 0) uniform Projection {
    mat4 mat;
} projection;

layout(push_constant) uniform PushConstants {
    mat4 modelMatrix;
} pushConstants;


void main()
{
    gl_Position = projection.mat * pushConstants.modelMatrix * vec4(pos, 1);
    texCoordsOut = texCoords;
}