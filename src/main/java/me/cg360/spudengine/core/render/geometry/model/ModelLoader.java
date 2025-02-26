package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.texture.TextureManager;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModelLoader {

    public static Model loadEnvironmentModel(String modelId, String modelResourcePath) {
        return loadModel(
                modelId, modelResourcePath, TextureManager.BASE_PATH,
                Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_Triangulate |
                Assimp.aiProcess_CalcTangentSpace | Assimp.aiProcess_PreTransformVertices
        );
    }

    public static Model loadModel(String modelId, String modelResourcePath) {
        return loadModel(
                modelId, modelResourcePath, TextureManager.BASE_PATH,
                Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_Triangulate |
                Assimp.aiProcess_CalcTangentSpace | Assimp.aiProcess_PreTransformVertices |
                Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_FixInfacingNormals
        );
    }

    private static Model loadModel(String modelId, String modelResourcePath, String texturesDir, int flags) {
        Logger.debug("Loading model data [{}]", modelResourcePath);

        String resPath = ClassLoader.getSystemResource(ModelManager.BASE_PATH + modelResourcePath).getPath();
        File modelPath = new File(resPath);

        if (!modelPath.exists())
            throw new RuntimeException("Model path does not exist [" + modelPath + "]");

        AIScene aiScene = Assimp.aiImportFile(modelPath.getPath(), flags);
        if (aiScene == null)
            throw new RuntimeException("Error loading model [modelPath: " + modelResourcePath + ", texturesDir: ]");

        int numMaterials = aiScene.mNumMaterials();
        List<Material> materialList = new ArrayList<>(numMaterials);
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiScene.mMaterials().get(i));
            Material material = ModelLoader.processMaterial(aiMaterial);
            materialList.add(material);
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        List<Mesh> meshDataList = new ArrayList<>(numMeshes);
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            Mesh meshData = ModelLoader.processMesh(aiMesh);
            meshDataList.add(meshData);
        }

        Model modelData = new Model(modelId, materialList, meshDataList);

        Assimp.aiReleaseImport(aiScene);
        Logger.debug("Loaded model [{}]", modelResourcePath);
        return modelData;
    }

    private static Material processMaterial(AIMaterial aiMaterial) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D colour = AIColor4D.create();

            Vector4f diffuse = Material.WHITE;
            int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, colour);
            if (result == Assimp.aiReturn_SUCCESS) {
                diffuse = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
            }

            AIString aiTexturePath = AIString.calloc(stack);
            Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, aiTexturePath,
                    (IntBuffer) null, null, null, null, null, null);

            String texturePath = aiTexturePath.dataString();
            if (!texturePath.isEmpty()) {
                diffuse = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
            }

            Logger.debug("built-in: {}", aiTexturePath.dataString());
            Logger.debug("processed: {}", texturePath);

            return new Material(texturePath, diffuse);
        }
    }

    private static Mesh processMesh(AIMesh imported) {
        float[] vertices = ModelLoader.processVertices(imported);
        float[] textCoords = ModelLoader.processTextCoords(imported);
        int[] indices = ModelLoader.processIndices(imported);
        int materialIdx = imported.mMaterialIndex();

        return textCoords != null
                ? new Mesh(vertices, textCoords, indices, materialIdx)
                : Mesh.withoutProvidedUVs(vertices, indices, materialIdx);
    }

    private static float[] processVertices(AIMesh aiMesh) {
        int componentCount = aiMesh.mNumVertices() * 3;
        float[] verticies = new float[componentCount];

        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        for(int vOffset = 0; vOffset < componentCount; vOffset+=3) {
            AIVector3D aiVertex = aiVertices.get();
            verticies[vOffset  ] = aiVertex.x();
            verticies[vOffset+1] = aiVertex.y();
            verticies[vOffset+2] = aiVertex.z();
        }

        return verticies;
    }

    private static float[] processTextCoords(AIMesh aiMesh) {
        AIVector3D.Buffer aiTextCoords = aiMesh.mTextureCoords(0);
        if(aiTextCoords == null)
            return null;

        int componentCount = aiMesh.mNumVertices() * 2;
        float[] texCoords = new float[componentCount];

        for (int vOffset = 0; vOffset < componentCount; vOffset+=2) {
            AIVector3D textCoord = aiTextCoords.get();
            texCoords[vOffset  ] = textCoord.x();
            texCoords[vOffset+1] = 1 - textCoord.y();
        }

        return texCoords;
    }


    protected static int[] processIndices(AIMesh aiMesh) {
        int numFaces = aiMesh.mNumFaces();
        List<Integer> indices = new ArrayList<>(3*numFaces);
        AIFace.Buffer aiFaces = aiMesh.mFaces();

        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = aiFaces.get(i);
            IntBuffer buffer = aiFace.mIndices();

            while (buffer.remaining() > 0)
                indices.add(buffer.get());
        }

        return indices.stream().mapToInt(i -> i).toArray();
    }
}
