#version 450

layout (triangles) in;
layout (triangle_strip, max_vertices = 9) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

layout(location = 0) in VS_OUT {
    vec3 pos;
    vec2 texCoords;
    mat4 modelTransform;
    mat4 projectionMatrix;
    mat4 roomTransform;
} gs_in[];

layout(set = 0, binding = 0) uniform PORTAL_SET {
    mat4 blueTransform;
    mat4 redTransform;
    // do I need to send normals as well?
} portals;

layout(location = 0) out vec2 texCoords;

void emitOffsetRoom(mat4 transform) {
    mat4 mP = gs_in[0].projectionMatrix;

    texCoords = gs_in[0].texCoords;
    gl_Position = mP * transform * gs_in[0].modelTransform * vec4(gs_in[0].pos, 1);
    EmitVertex();

    texCoords = gs_in[1].texCoords;
    gl_Position = mP * transform * gs_in[1].modelTransform * vec4(gs_in[1].pos, 1);
    EmitVertex();

    texCoords = gs_in[2].texCoords;
    gl_Position = mP * transform * gs_in[2].modelTransform * vec4(gs_in[2].pos, 1);
    EmitVertex();

    EndPrimitive();
}

void main() {
    mat4 transform = mat4(1);

    emitOffsetRoom(transform);
    emitOffsetRoom(blueTransform);
    emitOffsetRoom(redTransform);
}