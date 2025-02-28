#version 450

layout (triangles) in;
layout (triangle_strip, max_vertices = 9) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

layout(location = 0) in VS_OUT {
    vec3 pos;
    vec2 texCoords;
    mat4 projectionMatrix;
    mat4 viewMatrix;
    mat4 modelTransform;
} gs_in[];

layout(location = 0) out vec2 texCoords;

void emitOffsetRoom(mat4 transform) {
    mat4 mP = gs_in[0].projectionMatrix;
    mat4 mV = gs_in[0].viewMatrix;

    texCoords = gs_in[0].texCoords;
    gl_Position = mP * mV * transform * gs_in[0].modelTransform * vec4(gs_in[0].pos, 1);
    EmitVertex();

    texCoords = gs_in[1].texCoords;
    gl_Position = mP * mV * transform * gs_in[1].modelTransform * vec4(gs_in[1].pos, 1);
    EmitVertex();

    texCoords = gs_in[2].texCoords;
    gl_Position = mP * mV * transform * gs_in[2].modelTransform * vec4(gs_in[2].pos, 1);
    EmitVertex();

    EndPrimitive();
}

void main() {
    mat4 transform = mat4(1);
    emitOffsetRoom(transform); // no transform.
}