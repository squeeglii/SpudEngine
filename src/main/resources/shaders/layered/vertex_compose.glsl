#version 450

layout(location = 0) out vec2 outTexCoords;

// Vulkan Graphics Programming For in Java, Antonio Hernández Bejarano
// https://github.com/lwjglgamedev/vulkanbook
// :: this fills the screen without the need to send any vertex data to the shader.
void main()
{
    outTexCoords = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(outTexCoords.x * 2.0f - 1.0f, outTexCoords.y * -2.0f + 1.0f, 0.0f, 1.0f);
}