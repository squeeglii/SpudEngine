#version 450

layout (triangles) in;
layout (triangle_strip, max_vertices = 3+(3*2*5)) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

layout(location = 0) in VS_OUT {
    vec3 pos;
    vec2 texCoords;
    mat4 projectionMatrix;
    mat4 viewMatrix;
    mat4 modelTransform;
} gs_in[];

layout(set = 2, binding = 0) uniform PORTAL_SET {
    mat4 blueTransform;
    mat4 orangeTransform;      // do I need to send normals as well?
} portals;

layout(location = 0) out vec2 texCoords;
layout(location = 1) out vec4 debugColour;

void emitOffsetRoom(mat4 transform, vec4 debug) {
    mat4 mP = gs_in[0].projectionMatrix;
    mat4 mV = gs_in[0].viewMatrix;

    debugColour = debug;

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

    emitOffsetRoom(transform, vec4(1, 1, 1, 1));

    //todo: send culling through arrays to shader?
    //      ...or even send the full octree?

    //transform[3][0] = 10;

    emitOffsetRoom(portals.blueTransform, vec4(0.5, 0.5, 1.0, 1));
    emitOffsetRoom(portals.blueTransform*portals.blueTransform, vec4(0.3, 0.3, 1.0, 1));
    emitOffsetRoom(portals.blueTransform*portals.blueTransform*portals.blueTransform, vec4(0.1, 0.1, 1.0, 1));

    emitOffsetRoom(portals.orangeTransform, vec4(1.0, 0.6, 0.4, 1));
    emitOffsetRoom(portals.orangeTransform*portals.orangeTransform, vec4(1.0, 0.6, 0.2, 1));
    emitOffsetRoom(portals.orangeTransform*portals.orangeTransform*portals.orangeTransform, vec4(1.0, 0.6, 0.03, 1));

    //emitOffsetRoom(portals.orangeTransform);
}