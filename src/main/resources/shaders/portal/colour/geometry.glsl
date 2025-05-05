#version 450

// fragment shader uses threshold of -512 - overshoot it for sanity?
#define OVERLAY_UNDEFINED vec2(-1024, -1024)

#define IS_BLUE_MASK == 1
#define IS_ORANGE_MASK == 2

// portal is 1u wide, 2u tall by default.
#define PORTAL_SCALE_X 1
#define PORTAL_SCALE_Y 2
#define PORTAL_CHECK_RANGE 3

#define MAX_RECURSION_DEPTH 6

#define BLUE_PORTAL vec4(0.02, 0.25, 1, 1)
#define ORANGE_PORTAL vec4(1, 0.25, 0, 1)

layout (triangles) in;
layout (triangle_strip, max_vertices = 3+(3*2*MAX_RECURSION_DEPTH)) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

layout(location = 0) in VS_OUT {
    vec3 pos;
    vec2 texCoords;
} gs_in[];

layout(push_constant) uniform PushConstants {
    mat4 modelTransform;
} pushConstants;

layout(set = 0, binding = 0) uniform Projection {
    mat4 mat;
} projection;

layout(set = 1, binding = 0) uniform Camera {
    mat4 mat;
} view;

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

layout(set = 5, binding = 0) uniform PORTAL_TYPE_MASK {
    int val;
} portalTypeMask;

layout(location = 0) out vec2 texCoords;
layout(location = 1) out vec2 overlayTexCoords;
layout(location = 2) out vec4 portalBorderColour;
layout(location = 3) out vec4 debugPos;

// Creates a copy of the primitive and transforms (roomTransform) it.
// This is used to stitch rooms together at portal boundaries provided using the stitchTransform provided by both portals.
// vec4[3] worldPos       -  tri primitive vertex positions
// vec4[3] overlayCoords  -  overlay UVs, used by portal/colour/fragment to cutout the portal hole.
// vec4    portalColour   -  colour used to tint the portal edge. This is just passed through.
// mat4    roomTransform  -  transform applied on top of worldPos transform. Places the primitive in position for a room further down the portal scope.
void emitOffsetRoom(vec4[3] worldPos, vec2[3] overlayCoords, vec4 portalColour, mat4 roomTransform) {
    mat4 mP = projection.mat;
    mat4 mV = view.mat;

    // copy the points!
    for(int v = 0; v < 3; v++) {
        texCoords = gs_in[v].texCoords;
        overlayTexCoords = overlayCoords[v];
        portalBorderColour = portalColour;
        gl_Position = mP * mV * roomTransform * worldPos[v];
        debugPos = mP * mV * roomTransform * worldPos[v];
        EmitVertex();
    }

    EndPrimitive();
}

// Process per-primitive:
// - check both portals are provided
// - find the closest portal to a primitive and adopt it with that portal
// - calculate overlay UVs for how the portal would cover that primitive
//
void main() {
    vec4[3] worldPositions = vec4[3](
            pushConstants.modelTransform * vec4(gs_in[0].pos, 1),
            pushConstants.modelTransform * vec4(gs_in[1].pos, 1),
            pushConstants.modelTransform * vec4(gs_in[2].pos, 1)
    );

    vec2[3] generatedOverlayCoords = vec2[3]( OVERLAY_UNDEFINED, OVERLAY_UNDEFINED, OVERLAY_UNDEFINED );
    vec2[3] blankOverlayCoords = vec2[3]( OVERLAY_UNDEFINED, OVERLAY_UNDEFINED, OVERLAY_UNDEFINED );
    vec4 portalColour;

    bool missingBlue = bluePortal.stitchTransform == mat4(0);
    bool missingOrange = orangePortal.stitchTransform == mat4(0);

    // Then this is just a basic shader. Just emit one room provided that
    // we're in the right renderpass for it (as passed by portalLayer.num) - this fixes no portals being weirdly laggy.
    if(missingBlue && missingOrange) {
        if(portalLayer.num <= 0) {   // -1 for ignore all checks, 0 for ONLY this layer.
            emitOffsetRoom(worldPositions, blankOverlayCoords, vec4(1, 1, 1, 1), mat4(1));// room without portal transform.
        }
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

        vec3 diffWide = diffPortal * closestCross;
        float lenWide = length(diffWide)  / PORTAL_SCALE_X;
        vec3 diffTall = diffPortal * closestUp;
        float lenTall = length(diffTall) / PORTAL_SCALE_Y;

        vec2 scaledDiff = vec2(lenWide, lenTall);
        vec2 rotatedScaledDiff = scaledDiff + vec2(0.5, 0.5);
        //todo: ^ maths here is broken somewhere. Portals snapped to 90 degrees look fine.
        //        portals at other rotations gain sharp edges.

        generatedOverlayCoords[i] = rotatedScaledDiff; // origin 0.5 0.5;

        //generatedOverlayCoords[i].x = clamp(0, generatedOverlayCoords[i].x, 1);
        //generatedOverlayCoords[i].y = clamp(0, generatedOverlayCoords[i].y, 1);
    }

    //todo: send culling through arrays to shader?
    //      ...or even send the full octree?

    if(portalLayer.num <= 0) { // -1 for ignore all checks, 0 for ONLY the origin layer.
        emitOffsetRoom(worldPositions, generatedOverlayCoords, portalColour, mat4(1));// room without portal transform.

        if(portalLayer.num == 0)
            return;
    }

    // don't use portal transforms if there isn't even a link.
    if(missingBlue || missingOrange)
        return;

    // rooms with portal transforms.
    mat4 blue = mat4(1);
    mat4 orange = mat4(1);

    // Portal type is "unassigned" skip.
    if(portalTypeMask.val == 0) {
        return;
    }

    bool ignorePortalTypeMask = portalTypeMask.val < 0;

    for(int i = 1; i <= MAX_RECURSION_DEPTH; i++) {
        blue *= bluePortal.stitchTransform;
        orange *= orangePortal.stitchTransform;

        // -1 for ignore all checks, 0 for ONLY this layer.
        if((portalLayer.num >= 0) && (i != portalLayer.num))
            continue;

        // Final portal should not generate portal overlay.
        vec2[3] overlay = i == MAX_RECURSION_DEPTH
                            ? blankOverlayCoords
                            : generatedOverlayCoords;

        if(ignorePortalTypeMask || portalTypeMask.val IS_BLUE_MASK) {
            emitOffsetRoom(worldPositions, overlay, portalColour, blue);
        }

        if(ignorePortalTypeMask || portalTypeMask.val IS_ORANGE_MASK) {
            emitOffsetRoom(worldPositions, overlay, portalColour, orange);
        }
    }
}