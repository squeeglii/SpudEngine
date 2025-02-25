package me.cg360.spudengine.core.render.geometry.model;

import org.joml.Vector3f;

import java.util.Arrays;

public class Primitive {

    public static final Mesh[] CUBE;


    private static final int FACE_VERTICES = 4;
    private static final float[] CUBE_VERTICES = {
            -1, -1, -1, // BSW
            -1, -1,  1, // BNW
            1, -1,  1, // BNE
            1, -1, -1, // BSE
            -1,  1, -1, // TSW
            -1,  1,  1, // TNW
            1,  1,  1, // TNE
            1,  1, -1, // TSE
    };
    private static final int[] FACE_INDICES = {
            0, 1, 2, 3,   // bottom
            7, 6, 5, 4,   // top
            5, 6, 2, 1,   // north
            6, 7, 3, 2,   // east
            7, 4, 0, 3,   // south
            4, 5, 1, 0    // west
    };
    private static final float[] FACE_TEXTURE_COORDINATES = {
            0, 0,
            1, 0,
            1, 1,
            0, 1
    };

    private static Mesh[] buildCube() {
        Mesh[] meshes = new Mesh[6];

        for(int face = 0; face < 6; face++){
            int faceOffset = face * 4;

            int[] faceIndices = Arrays.copyOfRange(FACE_INDICES, faceOffset, faceOffset+4);
            Vector3f faceNormal = new Vector3f(0, 0, 0);

            float[] mPos = new float[FACE_VERTICES*3];
            float[] mTexCoord = new float[FACE_VERTICES*2];
            float[] mNormal = new float[FACE_VERTICES*3];
            int[] mTri = new int[3*2];

            // Get each vertex & process it.
            for(int v = 0; v < FACE_VERTICES; v++) {
                int index = faceIndices[v];
                float[] vertexComponents = Arrays.copyOfRange(CUBE_VERTICES, 3*index, 3*(index+1));
                Vector3f vertex = new Vector3f(vertexComponents);

                // If you add together the 4 face vertices and normalise
                // it, it points in the face's normal direction!
                faceNormal = faceNormal.add(vertex);

                // Textures just get the id of the current vertex and face, and
                // use odd/even numbers for U & V respectively.
                float tU = FACE_TEXTURE_COORDINATES[(2 * v)];
                float tV = FACE_TEXTURE_COORDINATES[(2 * v) + 1];

                int m2 = v * 2;
                int m3 = v * 3;
                mTexCoord[m2    ] = tU;
                mTexCoord[m2 + 1] = tV;

                // I can't read, so I use 1's for the constants and half them.
                mPos[m3  ] = vertex.x() * 0.5f;
                mPos[m3+1] = vertex.y() * 0.5f;
                mPos[m3+2] = vertex.z() * 0.5f;
            }

            // Normal calculations applied separately from vertices loop so
            // they could be totalled and then properly normalised.
            faceNormal.normalize();

            // Using a loop to assign normals so it's not a wall of text. Every vertex
            // has the same normal ("faceNormal") which points perpendicular from the current side.
            // Using modulo to fill the x,y,z into each vertex's normal.
            for(int i = 0; i < FACE_VERTICES*3; i++) {
                mNormal[i] = switch (i % 3) {
                    case 0 -> faceNormal.x();
                    case 1 -> faceNormal.y();
                    case 2 -> faceNormal.z();
                    default -> throw new IllegalStateException("Modulo operator did NOT work as expected.");
                };
            }

            // Faces points are already placed in the vertex buffer as an
            // anticlockwise quad, so the same index pattern can be used for every
            // face.
            // Manually add the indices for an anticlockwise quad
            int i = 0;
            mTri[i++] = faceOffset;
            mTri[i++] = faceOffset+1;
            mTri[i++] = faceOffset+2;
            mTri[i++] = faceOffset;
            mTri[i++] = faceOffset+2;
            mTri[i  ] = faceOffset+3;

            meshes[face] = new Mesh(mPos, mTexCoord, mTri, 0);
        }

        return meshes;
    }

    static {
        CUBE = buildCube();
    }



}
