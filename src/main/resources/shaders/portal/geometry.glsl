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

layout(set = 2, binding = 0) uniform BLUE_PORTAL_DATA {
    vec3 worldPos;
    vec3 up;
    vec3 normal;
    mat4 stitchTransform;
} bluePortal;

layout(set = 3, binding = 0) uniform ORANGE_PORTAL_DATA {
    vec3 worldPos;
    vec3 up;
    vec3 normal;
    mat4 stitchTransform;
} orangePortal;

layout(set = 4, binding = 0) uniform PORTAL_LAYER {
    int num;
} portalLayer;

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

    bool missingBlue = bluePortal.stitchTransform == mat4(0);
    bool missingOrange = orangePortal.stitchTransform == mat4(0);

    // then this is just a basic shader. Just emit one room
    if(missingBlue && missingOrange) {
        emitOffsetRoom(worldPositions, generatedOverlayCoords, vec4(1, 1, 1, 1), mat4(1), 0, vec4(1, 1, 1, 1)); // room without portal transform.
        return;
    }

    // Portal overlays
    for(int i = 0; i < 3; i++) {
        vec3 pos = worldPositions[i].xyz;

        float dBlue = missingBlue ? PORTAL_CHECK_RANGE : distance(pos, bluePortal.worldPos);
        float dOrange = missingOrange ? PORTAL_CHECK_RANGE : distance(pos, orangePortal.worldPos);

        if(dBlue >= PORTAL_CHECK_RANGE && dOrange >= PORTAL_CHECK_RANGE) {
            portalColour = vec4(1, 1, 1, 1);
            continue;
        }

        vec3 closestPortal;
        vec3 closestUp;
        vec3 closestNormal;

        if(dBlue < dOrange) {
            closestPortal = bluePortal.worldPos;
            closestUp = bluePortal.up;
            closestNormal = bluePortal.normal;
            portalColour = BLUE_PORTAL;
        } else {
            closestPortal = orangePortal.worldPos;
            closestUp = orangePortal.up;
            closestNormal = orangePortal.normal;
            portalColour = ORANGE_PORTAL;
        }

        // check the vertex is actually on the same wall as the portal. Otherwise a portal
        // can gobble up a corridor.
        vec3 diffPortal = pos - closestPortal;
        if(length(diffPortal * closestNormal) > 0.1)
            continue;

       vec3 closestCross = cross(closestUp, closestNormal);

        // portal is 1u wide, 2u tall.
        vec3 diffWide = diffPortal * closestCross;
        float lenWide = length(diffWide);
        vec3 diffTall = (diffPortal * closestUp) / 2;
        float lenTall = length(diffTall);

        vec2 scaledDiff = vec2(lenWide, lenTall);
        vec2 rotatedScaledDiff = scaledDiff + vec2(0.5, 0.5);
        //todo: ^ maths here is broken somewhere. Portals snapped to 90 degrees look fine.
        //        portals at other rotations gain sharp edges.

        // if debugging uvs, clamp values to UV range of 0-1
        generatedOverlayCoords[i] = rotatedScaledDiff; // origin 0.5 0.5;

        //generatedOverlayCoords[i].x = clamp(0, generatedOverlayCoords[i].x, 1);
        //generatedOverlayCoords[i].y = clamp(0, generatedOverlayCoords[i].y, 1);
    }

    //todo: send culling through arrays to shader?
    //      ...or even send the full octree?

    if(portalLayer.num == 0) {
        emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, mat4(1), 0, vec4(1, 1, 1, 1));// room without portal transform.
        return;
    }

    // don't use portal transforms if there isn't even a link.
    if(missingBlue || missingOrange)
        return;

    // rooms with portal transforms.
    mat4 blue = mat4(1);
    mat4 orange = mat4(1);

    for(int i = 1; i <= MAX_RECURSION_DEPTH; i++) {
        blue *= bluePortal.stitchTransform;
        orange *= orangePortal.stitchTransform;

        if(i != portalLayer.num)
            continue;

        // calc debug colour tint.
        float r = MAX_RECURSION_DEPTH;
        float colFrac = 0.5 + 0.5 * ((i+1) / r);
        vec4 blueCol = vec4(hsv2rgb(0.5, colFrac, 1), 1);
        vec4 orangeCol = vec4(hsv2rgb(0.05, colFrac, 1), 1);

        emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, blue, i, blueCol);
        emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, orange, i, orangeCol);
    }
}