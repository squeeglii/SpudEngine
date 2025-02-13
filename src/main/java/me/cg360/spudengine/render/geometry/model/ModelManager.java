package me.cg360.spudengine.render.geometry.model;

import me.cg360.spudengine.render.buffer.BufferTransfer;
import me.cg360.spudengine.render.buffer.GeneralBuffer;
import me.cg360.spudengine.render.command.CommandBuffer;
import me.cg360.spudengine.render.command.CommandPool;
import me.cg360.spudengine.render.command.CommandQueue;
import me.cg360.spudengine.render.hardware.LogicalDevice;
import me.cg360.spudengine.render.sync.Fence;
import me.cg360.spudengine.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModelManager {

    public static List<BufferedModel> transformModels(CommandPool pool, CommandQueue queue, Model... models) {
        return ModelManager.transformModels(pool, queue, List.of(models));
    }

    public static List<BufferedModel> transformModels(CommandPool pool, CommandQueue queue, List<Model> models) {
        // todo: track buffered instances?

        List<BufferedModel> vulkanModelList = new ArrayList<>();
        List<GeneralBuffer> stagingBufferList = new ArrayList<>();
        LogicalDevice device = pool.getDevice();
        CommandBuffer cmd = new CommandBuffer(pool, true, true);

        // bulk action for all models. Efficiency!
        cmd.record(() -> {
            for (Model modelData : models) {
                BufferedModel vulkanModel = new BufferedModel(modelData.getId());
                vulkanModelList.add(vulkanModel);

                // Transform meshes loading their data into GPU buffers
                for (Mesh meshData : modelData.getSubMeshes()) {
                    BufferTransfer verticesBuffers = createVerticesBuffers(device, meshData);
                    BufferTransfer indicesBuffers = createIndicesBuffers(device, meshData);
                    stagingBufferList.add(verticesBuffers.src());
                    stagingBufferList.add(indicesBuffers.src());
                    ModelManager.recordTransferCommand(cmd, verticesBuffers);
                    ModelManager.recordTransferCommand(cmd, indicesBuffers);

                    BufferedMesh bufferedMesh = new BufferedMesh(verticesBuffers.dst(), indicesBuffers.dst(), meshData.indices().length);
                    vulkanModel.getSubMeshes().add(bufferedMesh);
                }
            }
        });

        // Wait for execution.
        Fence fence = new Fence(device, true);
        fence.reset();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            queue.submit(stack.pointers(cmd.asVk()), null, null, null, fence);
        }

        fence.fenceWait();
        fence.cleanup();
        cmd.cleanup();

        stagingBufferList.forEach(GeneralBuffer::cleanup);

        return vulkanModelList;
    }

    private static BufferTransfer createVerticesBuffers(LogicalDevice device, Mesh mesh) {
        float[] positions = mesh.positions();
        int numPositions = positions.length;
        int bufferSize = numPositions * VulkanUtil.FLOAT_BYTES;

        // use for transfer, set mappable, remove need for flushing
        int srcUse = VK11.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        int srcReq = VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK11.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;

        // use for transfer, mark as storing vertices, mark as used for GPU.
        int dstUse = VK11.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK11.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        int dstReq = VK11.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;

        GeneralBuffer srcBuffer = new GeneralBuffer(device, bufferSize, srcUse, srcReq);
        GeneralBuffer dstBuffer = new GeneralBuffer(device, bufferSize, dstUse, dstReq);

        long mappedMemory = srcBuffer.map();
        FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int) srcBuffer.getRequestedSize());
        data.put(positions);
        srcBuffer.unmap();

        return new BufferTransfer(srcBuffer, dstBuffer);
    }

    private static BufferTransfer createIndicesBuffers(LogicalDevice device, Mesh meshData) {
        int[] indices = meshData.indices();
        int numIndices = indices.length;
        int bufferSize = numIndices * VulkanUtil.INT_BYTES;

        // use for transfer, set mappable, remove need for flushing
        int srcUse = VK11.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        int srcReq = VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK11.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;

        // use for transfer, mark as storing vertices, mark as used for GPU.
        int dstUse = VK11.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK11.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
        int dstReq = VK11.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;

        GeneralBuffer srcBuffer = new GeneralBuffer(device, bufferSize, srcUse, srcReq);
        GeneralBuffer dstBuffer = new GeneralBuffer(device, bufferSize, dstUse, dstReq);

        long mappedMemory = srcBuffer.map();
        IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int) srcBuffer.getRequestedSize());
        data.put(indices);
        srcBuffer.unmap();

        return new BufferTransfer(srcBuffer, dstBuffer);
    }

    private static void recordTransferCommand(CommandBuffer cmd, BufferTransfer transfer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(transfer.src().getRequestedSize());
            VK11.vkCmdCopyBuffer(cmd.asVk(), transfer.src().getHandle(), transfer.dst().getHandle(), copyRegion);
        }
    }

}
