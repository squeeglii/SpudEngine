#version 450

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;

layout(location=0) out VS_OUT {
    vec3 pos;
    vec2 texCoords;
} vs_out;

void main()
{
    vs_out.pos = pos;
    vs_out.texCoords = texCoords;
}