#version 450

layout (triangles) in;
layout (triangle_strip, max_vertices = 9) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

layout(location = 0) in VS_OUT {
    vec3 pos;
    vec2 texCoords;
    mat4 modelTransform;
    mat4 projectionMatrix;
} gs_in[];

layout(location = 0) out vec2 texCoords;

void emitOffsetRoom(vec3 offset) {
    mat4 mP = gs_in[0].projectionMatrix;

    vec3 combinedPos;  // TODO: Fix. Scaling causes this to fall apart.

    texCoords = gs_in[0].texCoords;
    combinedPos = gs_in[0].pos + offset;
    gl_Position = mP * gs_in[0].modelTransform * vec4(combinedPos, 1);
    EmitVertex();

    texCoords = gs_in[1].texCoords;
    combinedPos = gs_in[1].pos + offset;
    gl_Position = mP * gs_in[1].modelTransform * vec4(combinedPos, 1);
    EmitVertex();

    texCoords = gs_in[2].texCoords;
    combinedPos = gs_in[2].pos + offset;
    gl_Position = mP * gs_in[2].modelTransform * vec4(combinedPos, 1);
    EmitVertex();

    EndPrimitive();
}

void main() {
    emitOffsetRoom(vec3(0, 0, 0));
    emitOffsetRoom(vec3(12, 0, -1));
    emitOffsetRoom(vec3(24, 0, -2));
}