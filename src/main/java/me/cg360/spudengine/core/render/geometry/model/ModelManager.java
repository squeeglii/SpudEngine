package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.data.buffer.BufferTransfer;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.ModelValidationException;
import me.cg360.spudengine.core.render.geometry.VertexFormatSummary;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureManager;
import me.cg360.spudengine.core.util.Registry;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferCopy;
import org.tinylog.Logger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class ModelManager implements Registry {

    public static String ID_MISSING_MODEL = "missing";

    public static final String BASE_PATH = "assets/models/";

    private static final VertexFormatSummary BUFFER_FORMAT = VertexFormats.POSITION_UV;

    private RenderSystem host;

    private final LogicalDevice graphicsDevice;
    private final TextureManager textureManager;

    private final Map<String, BufferedModel> bufferedModels;

    public final Material fallbackMaterial;

    public ModelManager(RenderSystem host, LogicalDevice graphicsDevice, TextureManager textureManager) {
        this.host = host;
        this.graphicsDevice = graphicsDevice;
        this.textureManager = textureManager;
        this.bufferedModels = new HashMap<>();

        this.fallbackMaterial = new Material(TextureManager.TEX_MISSING, Material.WHITE);
    }

    public void createMissingModel() {
        Model model = new Model(ID_MISSING_MODEL, Primitive.CUBE);
        this.processModels(model);
    }

    public void cleanup() {
        this.bufferedModels.values().forEach(BufferedModel::cleanup);
        this.bufferedModels.clear();
        this.host = null;
    }

    /** Registers and stages models & their materials in a batch call. */
    public void processModels(Model... models) {
        List<BufferedModel> processed = this.transformModels(this.host.getCommandPool(), this.host.getGraphicsQueue(), models);
        this.host.getRenderProcess().processModelBatch(processed);
    }

    /** Registers and stages models & their materials in a batch call. */
    public void processModels(List<Model> models) {
        List<BufferedModel> processed = this.transformModels(this.host.getCommandPool(), this.host.getGraphicsQueue(), models);
        this.host.getRenderProcess().processModelBatch(processed);
    }

    /** @see ModelManager#transformModels(CommandPool, CommandQueue, List) */
    protected List<BufferedModel> transformModels(CommandPool pool, CommandQueue queue, Model... models) {
        return this.transformModels(pool, queue, List.of(models));
    }

    /**
     * A batch call that loads models from their data arrays
     * into buffers.
     */
    protected List<BufferedModel> transformModels(CommandPool pool, CommandQueue queue, List<Model> models) {
        Logger.debug("Loading {} model(s)", models.size());
        List<BufferedModel> bufferedModels = new ArrayList<>();
        Set<Texture> stagedTextures = new LinkedHashSet<>();
        List<GeneralBuffer> stagingBufferList = new ArrayList<>();
        LogicalDevice device = pool.getDevice();
        CommandBuffer cmd = new CommandBuffer(pool, true, true);

        // bulk action for all models. Efficiency!
        cmd.record(() -> {
            for (Model modelData : models) {
                Logger.trace("Buffering model: {}", modelData.getId());
                BufferedModel model = new BufferedModel(modelData.getId(), BUFFER_FORMAT);
                bufferedModels.add(model);

                for(Material material : modelData.getMaterials()) {
                    BundledMaterial mat = this.transformMaterialInternal(cmd, material, this.graphicsDevice, stagedTextures);
                    model.getMaterials().add(mat);
                }

                boolean createdFallbackMaterial = false;
                int materialCount = modelData.getMaterials().size();

                // Transform meshes loading their data into GPU buffers
                for (Mesh meshData : modelData.getSubMeshes()) {
                    try {
                        meshData.validate();
                    } catch (ModelValidationException err) {
                        Logger.error(err, "Error transforming model '%s'".formatted(modelData.getId()));
                    }

                    BufferTransfer verticesBuffers = createVerticesBuffers(device, meshData);
                    BufferTransfer indicesBuffers = createIndicesBuffers(device, meshData);
                    stagingBufferList.add(verticesBuffers.src());
                    stagingBufferList.add(indicesBuffers.src());
                    ModelManager.recordTransferCommand(cmd, verticesBuffers);
                    ModelManager.recordTransferCommand(cmd, indicesBuffers);

                    BufferedMesh bufferedMesh = new BufferedMesh(verticesBuffers.dst(), indicesBuffers.dst(), meshData.indices().length);
                    int matIndex = meshData.materialIdx();
                    boolean isValidMaterial = matIndex >= 0 && matIndex < materialCount;

                    // Place a fallback material at the end if there are invalid entries. Create once and reuse.
                    if(!isValidMaterial) {
                        if(!createdFallbackMaterial) {
                            BundledMaterial fallback = this.getFallbackMaterial(cmd, device, stagedTextures);
                            model.getMaterials().add(fallback);
                        }

                        model.getMaterials().get(materialCount).meshes().add(bufferedMesh);
                        continue;
                    }

                    model.getMaterials().get(matIndex).meshes().add(bufferedMesh);
                }
            }
        }).submitAndWait(queue);

        stagingBufferList.forEach(GeneralBuffer::cleanup);
        stagedTextures.forEach(Texture::cleanupStagingBuffer);

        for(BufferedModel model: bufferedModels) {
            String modelId = model.getId();

            if(this.bufferedModels.containsKey(modelId)) {
                Logger.warn("Attempted to re-register model %s. Purging old model.", modelId);
                this.bufferedModels.remove(modelId).cleanup();
            }

            this.bufferedModels.put(modelId, model);
        }

        Logger.debug("Loaded {} model(s)", this.bufferedModels.size());
        return bufferedModels;
    }

    private BundledMaterial transformMaterialInternal(CommandBuffer cmd, Material material, LogicalDevice device, Set<Texture> uploadedTextures) {
        // If texture is null, it loads the default texture.
        Texture texture = this.textureManager.loadTexture(device, material.texture(), VK11.VK_FORMAT_R8G8B8A8_SRGB);
        texture.upload(cmd);
        uploadedTextures.add(texture);

        return new BundledMaterial(material.diffuse(), texture, new ArrayList<>());
    }

    private BundledMaterial getFallbackMaterial(CommandBuffer cmd, LogicalDevice device, Set<Texture> uploadedTextures) {
        return this.transformMaterialInternal(cmd, this.fallbackMaterial, device, uploadedTextures);
    }

    public BufferedModel getModel(String id) {
        if (!this.bufferedModels.containsKey(id)) {
            Logger.trace("Failed to find model {}", id);
            Logger.trace("Available model pool: {}", this.bufferedModels);
            BufferedModel fallback = this.bufferedModels.get(ID_MISSING_MODEL);
            if (fallback == null)
                throw new IllegalStateException("Fallback model required, but was not registered & buffered.");

            return fallback;
        }

        return this.bufferedModels.get(id);
    }


    public Collection<BufferedModel> getAllModels() {
        return this.bufferedModels.values();
    }


    private static BufferTransfer createVerticesBuffers(LogicalDevice device, Mesh mesh) {
        float[] positions = mesh.positions();
        float[] texCoords = mesh.textureCoordinates();

        int vertexSize = BUFFER_FORMAT.getVertexSize();
        int elementCount = mesh.vertexCount() * vertexSize;
        int bufferSize = elementCount * VulkanUtil.FLOAT_BYTES;

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
        for(int v = 0; v < mesh.vertexCount(); v++) {
            int vPos = v * 3;
            int tPos = v * 2;

            data.put(positions[vPos  ]);
            data.put(positions[vPos+1]);
            data.put(positions[vPos+2]);    // adjusting this? Make sure it follows the BUFFER_FORMAT.
            data.put(texCoords[tPos  ]);
            data.put(texCoords[tPos+1]);
        }

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

    @Override
    public String getRegistryIdentifier() {
        return "MODEL_MANAGER";
    }
}
