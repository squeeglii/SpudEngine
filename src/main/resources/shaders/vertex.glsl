#version 450

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;

layout(set = 0, binding = 0) uniform Projection {
    mat4 mat;
} projection;

layout(set = 1, binding = 0) uniform Camera {
    mat4 mat;
} view;

layout(push_constant) uniform PushConstants {
    mat4 modelTransform;
} pushConstants;

layout(location=0) out VS_OUT {
    vec3 pos;
    vec2 texCoords;
    mat4 projectionMatrix;
    mat4 viewMatrix;
    mat4 modelTransform;
} vs_out;

void main()
{
    vs_out.pos = pos;
    vs_out.texCoords = texCoords;
    vs_out.projectionMatrix = projection.mat;
    vs_out.viewMatrix = view.mat;
    vs_out.modelTransform = pushConstants.modelTransform;
}