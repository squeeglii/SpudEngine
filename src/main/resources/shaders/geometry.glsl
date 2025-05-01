#version 450

// fragment shader uses threshold of -512 - overshoot it for sanity?
#define OVERLAY_UNDEFINED vec2(-1024, -1024)

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out; // 3x triangles, 9 vertices. Allows for 2 copies of every model.

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

// vec4[3] worldPos       -  tri primitive vertex positions
// vec4[3] overlayCoords  -  overlay UVs, used by portal/colour/fragment to cutout the portal hole.
void emit(vec4[3] worldPos, vec2[3] overlayCoords) {
    mat4 mP = projection.mat;
    mat4 mV = view.mat;

    // copy the points!
    for(int v = 0; v < 3; v++) {
        texCoords = gs_in[v].texCoords;
        overlayTexCoords = overlayCoords[v];
        portalBorderColour = portalColour;
        gl_Position = mP * mV * worldPos[v];
        debugPos = mP * mV * worldPos[v];
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

    vec2[3] blankOverlayCoords = vec2[3]( OVERLAY_UNDEFINED, OVERLAY_UNDEFINED, OVERLAY_UNDEFINED );

    emit(worldPositions, blankOverlayCoords);
}