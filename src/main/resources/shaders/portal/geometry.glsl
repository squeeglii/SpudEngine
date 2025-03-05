#version 450

#define RECURSION_DEPTH 10

layout (triangles) in;
layout (triangle_strip, max_vertices = 3+(3*2*RECURSION_DEPTH)) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

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

// https://gamedev.stackexchange.com/questions/59797/glsl-shader-change-hue-saturation-brightness
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 hsv2rgb(float h, float s, float v)
{
    return hsv2rgb(vec3(h, s, v));
}

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

    mat4 blue = mat4(1);
    mat4 orange = mat4(1);

    for(int i = 0; i < RECURSION_DEPTH; i++) {
        blue *= portals.blueTransform;
        orange *= portals.orangeTransform;

        float r = RECURSION_DEPTH;
        float colFrac = 0.5 + 0.5 * ((i+1) / r);

        vec4 blueCol = vec4(hsv2rgb(0.5, colFrac, 1), 1);
        vec4 orangeCol = vec4(hsv2rgb(0.05, colFrac, 1), 1);

        emitOffsetRoom(blue, blueCol);
        emitOffsetRoom(orange, orangeCol);
    }
}