#version 450

// fragment shader uses threshold of -512 - overshoot it for sanity?
#define OVERLAY_UNDEFINED vec2(-1024, -1024)

#define MAX_RECURSION_DEPTH 5
#define PORTAL_CHECK_RANGE 3

#define BLUE_PORTAL vec4(0.02, 0.25, 1, 1)
#define ORANGE_PORTAL vec4(1, 0.25, 0, 1)

layout (triangles) in;
layout (triangle_strip, max_vertices = 3+(3*2*MAX_RECURSION_DEPTH)) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

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
layout(location = 1) out vec2 overlayTexCoords;
layout(location = 2) out vec4 portalBorderColour;
layout(location = 3) out vec4 debugColour;

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

void emitOffsetRoom(vec4[3] worldPos, vec2[3] overlayCoords, vec4 portalColour, mat4 roomTransform, int currentDepth, vec4 debugHighlight) {
    mat4 mP = gs_in[0].projectionMatrix;
    mat4 mV = gs_in[0].viewMatrix;

    // debugging
    debugColour = debugHighlight;

    // copy the points!
    for(int v = 0; v < 3; v++) {
        texCoords = gs_in[v].texCoords;
        overlayTexCoords = overlayCoords[v];
        portalBorderColour = portalColour;
        gl_Position = mP * mV * roomTransform * worldPos[v];
        EmitVertex();
    }

    EndPrimitive();
}

void main() {
    vec4[3] worldPositions = vec4[3](
            gs_in[0].modelTransform * vec4(gs_in[0].pos, 1),
            gs_in[1].modelTransform * vec4(gs_in[1].pos, 1),
            gs_in[2].modelTransform * vec4(gs_in[2].pos, 1)
    );

    vec2[3] generatedOverlayCoords = vec2[3]( OVERLAY_UNDEFINED, OVERLAY_UNDEFINED, OVERLAY_UNDEFINED );
    vec4 portalColour;

    // Portal overlays
    for(int i = 0; i < 3; i++) {
        vec4 pos = worldPositions[i];

        float dBlue = distance(pos, portalOrigins.blue);
        float dOrange = distance(pos, portalOrigins.orange);

        if(dBlue > PORTAL_CHECK_RANGE && dOrange > PORTAL_CHECK_RANGE)
            continue;

        vec4 closestPortal;

        if(dBlue < dOrange) {
            closestPortal = portalOrigins.blue;
            portalColour = BLUE_PORTAL;
        } else {
            closestPortal = portalOrigins.orange;
            portalColour = ORANGE_PORTAL;
        }

        // portal is 1u wide, 2u tall.
        vec2 diffXZ = pos.xz - closestPortal.xz; // 0.5 in world space, is 0.5 in texture space;
        float lenXZ = length(diffXZ);
        float diffY = pos.y - closestPortal.y; // 1 in world space is 0.5 in texture space; squish.

        float u = 0.5 + lenXZ; //clamp(-0.5, lenXZ, 0.5);
        float v = 0.5 + (diffY / 2); //clamp(-0.5, diffY / 2, 0.5);   use clamps to test UVs.

        generatedOverlayCoords[i] = vec2(u, v); // origin 0.5 0.5;
    }

    //todo: send culling through arrays to shader?
    //      ...or even send the full octree?

    emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, mat4(1), 0, vec4(1, 1, 1, 1)); // room without portal transform.

    // rooms with portal transforms.
    mat4 blue = mat4(1);
    mat4 orange = mat4(1);

    for(int i = 1; i <= MAX_RECURSION_DEPTH; i++) {
        blue *= portalTransforms.blue;
        orange *= portalTransforms.orange;

        float r = MAX_RECURSION_DEPTH;
        float colFrac = 0.5 + 0.5 * ((i+1) / r);

        vec4 blueCol = vec4(hsv2rgb(0.5, colFrac, 1), 1);
        vec4 orangeCol = vec4(hsv2rgb(0.05, colFrac, 1), 1);

        emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, blue, i, blueCol);
        emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, orange, i, orangeCol);
    }
}