#version 450

layout (triangles) in;
layout (triangle_strip, max_vertices = 9) out;

layout(location = 0) in VS_OUT {
    vec2 texCoords;
    mat4 modelTransform;
    mat4 projectionMatrix;
} gs_in[];

layout(location = 0) out vec2 texCoords;

void emitOffsetRoom(vec4 offset) {
    vec4 transformedOffset = offset * gs_in[0].modelTransform;// * gs_in[0].projectionMatrix;

    texCoords = gs_in[0].texCoords;
    gl_Position = gl_in[0].gl_Position + transformedOffset;
    EmitVertex();

    texCoords = gs_in[1].texCoords;
    gl_Position = gl_in[1].gl_Position + transformedOffset;
    EmitVertex();

    texCoords = gs_in[2].texCoords;
    gl_Position = gl_in[2].gl_Position + transformedOffset;
    EmitVertex();

    EndPrimitive();
}

void main() {
    emitOffsetRoom(vec4(0, 0, 0, 0));
    emitOffsetRoom(vec4(10, 4, 0, 0));
    emitOffsetRoom(vec4(-10, 0, 0, 0));
}