#version 450

#define RECURSION_DEPTH 10
#define HOLEPUNCH_DIST 0.2

layout (triangles) in;
layout (triangle_strip, max_vertices = 3+(3*2*RECURSION_DEPTH)) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

layout(location = 0) in VS_OUT {
    vec3 pos;
    vec2 texCoords;
    mat4 projectionMatrix;
    mat4 viewMatrix;
    mat4 modelTransform;
} gs_in[];

layout(set = 2, binding = 0) uniform PORTAL_TRANSFORM_SET {
    mat4 blue;
    mat4 orange;
} portalTransforms;

layout(set = 3, binding = 0) uniform PORTAL_ORIGIN_SET {
    vec4 orange;
    vec4 blue;
} portalOrigins;

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

void emitOffsetRoom(mat4 roomTransform, int currentDepth, vec4 debugHighlight) {
    mat4 mP = gs_in[0].projectionMatrix;
    mat4 mV = gs_in[0].viewMatrix;

    debugColour = debugHighlight;

    vec4 worldPos_0 = gs_in[0].modelTransform * vec4(gs_in[0].pos, 1);
    vec4 worldPos_1 = gs_in[1].modelTransform * vec4(gs_in[1].pos, 1);
    vec4 worldPos_2 = gs_in[2].modelTransform * vec4(gs_in[2].pos, 1);

    //TODO: Transform points to check double holepunch.
    //      and add extra vertex to portal model so it actually cuts out.

    // Not yet at max depth. Punch holes through geometry.
    if(currentDepth < RECURSION_DEPTH) {
        bool p1Punch = false;
        bool p2Punch = false;
        bool p3Punch = false;

        if (distance(worldPos_0, portalOrigins.blue) <= HOLEPUNCH_DIST) p1Punch = true;
        if (distance(worldPos_0, portalOrigins.orange) <= HOLEPUNCH_DIST) p1Punch = true;

        if (distance(worldPos_1, portalOrigins.blue) <= HOLEPUNCH_DIST) p2Punch = true;
        if (distance(worldPos_1, portalOrigins.orange) <= HOLEPUNCH_DIST) p2Punch = true;

        if (distance(worldPos_2, portalOrigins.blue) <= HOLEPUNCH_DIST) p3Punch = true;
        if (distance(worldPos_2, portalOrigins.orange) <= HOLEPUNCH_DIST) p3Punch = true;

        if (p1Punch || p2Punch || p3Punch) {
            //debugColour = vec4(1, 0, 0, 0); // Entire triangle within range, cut.
            return;
        }
    }

    texCoords = gs_in[0].texCoords;
    gl_Position = mP * mV * roomTransform * worldPos_0;
    EmitVertex();

    texCoords = gs_in[1].texCoords;
    gl_Position = mP * mV * roomTransform * worldPos_1;
    EmitVertex();

    texCoords = gs_in[2].texCoords;
    gl_Position = mP * mV * roomTransform * worldPos_2;
    EmitVertex();

    EndPrimitive();
}

void main() {
    mat4 transform = mat4(1);

    emitOffsetRoom(transform, 0, vec4(1, 1, 1, 1));

    //todo: send culling through arrays to shader?
    //      ...or even send the full octree?

    mat4 blue = mat4(1);
    mat4 orange = mat4(1);

    for(int i = 1; i <= RECURSION_DEPTH; i++) {
        blue *= portalTransforms.blue;
        orange *= portalTransforms.orange;

        float r = RECURSION_DEPTH;
        float colFrac = 0.5 + 0.5 * ((i+1) / r);

        vec4 blueCol = vec4(hsv2rgb(0.5, colFrac, 1), 1);
        vec4 orangeCol = vec4(hsv2rgb(0.05, colFrac, 1), 1);

        emitOffsetRoom(blue, i, blueCol);
        emitOffsetRoom(orange, i, orangeCol);
    }
}