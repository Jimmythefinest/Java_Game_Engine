package com.njst.gaming.Loaders;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AITexel;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import com.njst.gaming.Bone;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class FBXBoneLoader {

    /**
     * Loads the FBX file and returns the root Bone of the hierarchy.
     *
     * @param filePath the path to the FBX file
     * @return the root Bone loaded from the FBX file
     */
    public static Bone loadBones(String filePath, Map<String, KeyframeAnimation> anims, float scale) {
        // Configure some basic processing flags. Adjust these as needed.
        int flags = aiProcess_JoinIdenticalVertices | aiProcess_Triangulate | aiProcess_FixInfacingNormals;
        AIScene scene = aiImportFile(filePath, flags);
        if (scene == null || scene.mRootNode() == null) {
            throw new RuntimeException("Error loading FBX file: " + aiGetErrorString());
        }

        // This map can help track bones by name if you need to cross-reference bone
        // data.
        // Map<String, Bone> boneMap = new HashMap<>();

        // Process the scene graph starting at the root node.
        Bone rootBone = processNode(scene.mRootNode(), scene, null, anims, scale);

        // When finished, free the scene
        aiReleaseImport(scene);
        return rootBone;
    }

    public static ArrayList<Bone> get_array(Bone root) {
        ArrayList<Bone> bones = new ArrayList<>();
        addtoArrayList(bones, root);
        return bones;
    }

    public static void addtoArrayList(ArrayList<Bone> list, Bone root) {
        list.add(root);
        for (Bone b : root.Children) {
            addtoArrayList(list, b);
        }
    }

    public static WeightedGeometry loadModel(String filepath, List<Bone> bone_array, int id, float scale) {
        // Configure some basic processing flags. Adjust these as needed.
        int flags = aiProcess_Triangulate | aiProcess_FixInfacingNormals;
        AIScene scene = aiImportFile(filepath, flags);
        if (scene == null || scene.mRootNode() == null) {
            throw new RuntimeException("Error loading FBX file: " + aiGetErrorString());
        }
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Float> texture_coords = new ArrayList<>();
        ArrayList<Float> weights = new ArrayList<>();
        ArrayList<String> Bones = new ArrayList<>();
        ArrayList<Integer> Bones_int = new ArrayList<>();

        // int num_meshes = scene.mNumMeshes();
        // for(int i=0;i<num_meshes;i++){
        AIMesh mesh = AIMesh.create(scene.mMeshes().get(id));
        processVertices(mesh, vertices, scale, 1, 1, -1);
        proccessNormals(mesh, normals);
        proccessIndices(mesh, indices);
        proccessTextureCoordinates(mesh, texture_coords);
        proccessWeights(mesh, weights, Bones);
        // }
        HashMap<String, Integer> hash = new HashMap<>();
        for (int i = 0; i < bone_array.size(); i++) {
            hash.put(bone_array.get(i).name, i);
        }
        // HashMap<String,Object> map=new HashMap<>();
        // map.put("vertices", vertices);
        // map.put("tex", texture_coords);
        // map.put("normals", normals);
        // map.put("weights", weights);
        // map.put("indices", indices);
        // map.put("bones", Bones);
        // Gson g=new Gson();
        //  GeneralUtil.save_to_file("/jimmy/ch.json", g.toJson(map));
        for (String name : Bones) {
            Bones_int.add(hash.get(name));
            // for(int i=0;i<bone_array.size();i++){
            // if(bone_array.get(i).name.equals(name)){
            // Bones_int.add(i);
            // // System.out.printf("Found bone name"+name+" loaction:"+i);
            // break;
            // }
            // }
        }
        for (int i = 0; i < 10; i++) {
            System.out.println(Bones.get(i) + ":" + vertices.get(i));
        }
        System.err.println("Number of vertices:" + (vertices.size() / 3));
        aiReleaseImport(scene);
        return new WeightedGeometry(
                list_to_array(vertices),
                list_to_array(normals),
                list_to_array(texture_coords),
                list_to_array(weights),
                list_to_array_int(indices),
                list_to_array_int(Bones_int));

    }

    public static int[] load_Textures(String fpath) {
        AIScene scene = Assimp.aiImportFile(fpath,
                Assimp.aiProcess_Triangulate | Assimp.aiProcess_FlipUVs);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new RuntimeException("Failed to load model");
        }

        PointerBuffer textures = scene.mTextures();
        int numTextures = scene.mNumTextures();
        int[] texture_array=new int [numTextures];
        System.out.println("Num Textures"+numTextures);
        for (int i = 0; i < numTextures; i++) {
            AITexture texture = AITexture.create(textures.get(i));

            // String formatHint = texture.achFormatHintString(); // e.g., "png", "jpg"

            if (texture.mHeight() == 0) {
                // It's a compressed format (e.g., PNG, JPEG)
                ByteBuffer data = texture.pcDataCompressed();

                // You can decode it with STBImage
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    IntBuffer comp = stack.mallocInt(1);

                    ByteBuffer image = STBImage.stbi_load_from_memory(data, w, h, comp, 4);
                    // int width = w.get(), height = h.get();
                    if (image == null) {
                        System.err.println("STB failed: " + STBImage.stbi_failure_reason());
                        continue;
                    }
                    int textureID=glGenTextures();
                    glBindTexture(GL_TEXTURE_2D,textureID);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w.get(),h.get(),0,GL_RGBA,GL_UNSIGNED_BYTE,image);
                    texture_array[i]=textureID;
                    System.out.println("loaded Texture"+texture.mFilename().dataString());
                    

                    // Use `image`, `width`, and `height` with OpenGL texture upload
                    // e.g., glTexImage2D...

                    STBImage.stbi_image_free(image);
                }
            } else {
                // Uncompressed RGBA texels
                AITexel.Buffer texels = texture.pcData();
                int width = texture.mWidth();
                int height = texture.mHeight();
                texture_array[i]=uploadTexelsToOpenGL(texels, width, height);
                System.out.println("found texture");
                // Convert texels to OpenGL texture (as in previous message)
            }
        }
        aiReleaseImport(scene);

        return texture_array;
    }

    public static WeightedGeometry loadModel_invert_xz(String filepath, ArrayList<Bone> bone_array, int id,
            float scale) {
        // Configure some basic processing flags. Adjust these as needed.
        int flags = aiProcess_Triangulate | aiProcess_FixInfacingNormals;
        AIScene scene = aiImportFile(filepath, flags);
        if (scene == null || scene.mRootNode() == null) {
            throw new RuntimeException("Error loading FBX file: " + aiGetErrorString());
        }
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Float> texture_coords = new ArrayList<>();

        ArrayList<Float> weights = new ArrayList<>();
        ArrayList<String> Bones = new ArrayList<>();
        ArrayList<Integer> Bones_int = new ArrayList<>();

        // int num_meshes = scene.mNumMeshes();
        // for(int i=0;i<num_meshes;i++){
        AIMesh mesh = AIMesh.create(scene.mMeshes().get(id));
        processVertices_invert_xz(mesh, vertices, scale, 1, 1, 1);
        proccessNormals(mesh, normals);
        proccessIndices(mesh, indices);
        proccessTextureCoordinates(mesh, texture_coords);
        proccessWeights(mesh, weights, Bones);
        // }
        HashMap<String, Integer> hash = new HashMap<>();
        for (int i = 0; i < bone_array.size(); i++) {
            hash.put(bone_array.get(i).name, i);
        }
        for (String name : Bones) {
            Bones_int.add(hash.get(name));
            // for(int i=0;i<bone_array.size();i++){
            // if(bone_array.get(i).name.equals(name)){
            // Bones_int.add(i);
            // // System.out.printf("Found bone name"+name+" loaction:"+i);
            // break;
            // }
            // }
        }
        for (int i = 0; i < 10; i++) {
            System.out.println(Bones.get(i) + ":" + vertices.get(i));
        }
        System.err.println("Number of vertices:" + (vertices.size() / 3));

        return new WeightedGeometry(
                list_to_array(vertices),
                list_to_array(normals),
                list_to_array(texture_coords),
                list_to_array(weights),
                list_to_array_int(indices),
                list_to_array_int(Bones_int));

    }

    public static WeightedGeometry loadModel(String filepath, ArrayList<Bone> bone_array, int id, float scale, float x,
            float y, float z) {
        // Configure some basic processing flags. Adjust these as needed.
        int flags = aiProcess_Triangulate | aiProcess_FixInfacingNormals;
        AIScene scene = aiImportFile(filepath, flags);
        if (scene == null || scene.mRootNode() == null) {
            throw new RuntimeException("Error loading FBX file: " + aiGetErrorString());
        }
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Float> texture_coords = new ArrayList<>();

        ArrayList<Float> weights = new ArrayList<>();
        ArrayList<String> Bones = new ArrayList<>();
        ArrayList<Integer> Bones_int = new ArrayList<>();

        // int num_meshes = scene.mNumMeshes();
        // for(int i=0;i<num_meshes;i++){
        AIMesh mesh = AIMesh.create(scene.mMeshes().get(id));
        processVertices(mesh, vertices, scale, x, y, z);
        proccessNormals(mesh, normals);
        proccessIndices(mesh, indices);
        proccessTextureCoordinates(mesh, texture_coords);
        proccessWeights(mesh, weights, Bones);
        // }
        HashMap<String, Integer> hash = new HashMap<>();
        for (int i = 0; i < bone_array.size(); i++) {
            hash.put(bone_array.get(i).name, i);
        }
        for (String name : Bones) {
            Bones_int.add(hash.get(name));
            // for(int i=0;i<bone_array.size();i++){
            // if(bone_array.get(i).name.equals(name)){
            // Bones_int.add(i);
            // // System.out.printf("Found bone name"+name+" loaction:"+i);
            // break;
            // }
            // }
        }
        for (int i = 0; i < 10; i++) {
            System.out.println(Bones.get(i) + ":" + vertices.get(i));
        }
        System.err.println("Number of vertices:" + (vertices.size() / 3));

        return new WeightedGeometry(
                list_to_array(vertices),
                list_to_array(normals),
                list_to_array(texture_coords),
                list_to_array(weights),
                list_to_array_int(indices),
                list_to_array_int(Bones_int));

    }

    /**
     * Recursively process an AINode and create a Bone for it.
     *
     * @param node    the current node from Assimp
     * @param scene   the entire scene (may be used to access meshes)
     * @param parent  the parent Bone (null for root)
     * @param boneMap a mapping from bone name to Bone for reference
     * @return the Bone created from the current node
     */
    private static Bone processNode(AINode node, AIScene scene, Bone parent, Map<String, KeyframeAnimation> anims,
            float scale) {
        Bone bone = new Bone();
        bone.name = node.mName().dataString();
        if (anims.containsKey(bone.name.replace(":", "").replace("_", ""))) {
            anims.get(bone.name.replace(":", "").replace("_", "")).bone = bone;
        } else {
            // System.out.println("failed to map:"+bone.name);
        }

        // Extract the translation from the node's transformation matrix.
        // Assimp matrices are column-major. Here we extract the translation components.
        AIMatrix4x4 transform = node.mTransformation();
        float tx = transform.a4() / scale; // translation x
        float ty = transform.b4() / scale; // translation y
        float tz = transform.c4() / scale; // translation z

        bone.scale = new Vector3(new float[] { 0.01f, 0.01f, 0.01f });
        bone.position_to_parent = new Vector3(tx, ty, tz);
        // if(parent!=null)bone.position_to_parent=parent.global_position.sub(bone.global_position);
        new Matrix4();
        // Set the parent if available.
        if (parent != null) {
            parent.Children.add(bone);
            bone.set_Parent_position(parent.global_position);
        }

        // Add this bone to our map.
        // boneMap.put(bone.name, bone);

        // Process any mesh data attached to this node.
        

        // Recursively process all children nodes.
        int numChildren = node.mNumChildren();
        PointerBuffer children = node.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode childNode = AINode.create(children.get(i));
            processNode(childNode, scene, bone, anims, scale);
        }
        Vector3 rot=getEulerAnglesFromMatrix(transform);
        if(rot.y==-180){
            rot=new Vector3(180,0,0);
        }
        bone.rotate(rot);


        return bone;
    }

    private static void processVertices(AIMesh aiMesh, List<Float> vertices, float scale, float x, float y, float z) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(x * aiVertex.x() / scale);
            vertices.add(y * aiVertex.z() / scale);
            vertices.add(z * aiVertex.y() / scale);
            if (aiVertex.x() == 0) {
                System.out.println("zero found");
            }

        }
    }

    private static void processVertices_invert_xz(AIMesh aiMesh, List<Float> vertices, float scale, float x, float y,
            float z) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(x * aiVertex.x() / scale);
            vertices.add(z * aiVertex.y() / scale);
            vertices.add(y * aiVertex.z() / scale);
            if (aiVertex.x() == 0) {
                System.out.println("zero found");
            }

        }
    }

    private static void proccessNormals(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mNormals();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.z());
            vertices.add(0 - aiVertex.y());
        }
    }

    private static void proccessTextureCoordinates(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
        }
    }

    private static void proccessIndices(AIMesh mesh, List<Integer> indices) {
        int numfaces = mesh.mNumFaces();
        for (int i = 0; i < numfaces; i++) {
            AIFace face = mesh.mFaces().get(i);
            int numIndices = face.mNumIndices();
            for (int j = 0; j < numIndices; j++) {
                indices.add((int) face.mIndices().get(j));
            }
        }

    }

    private static void proccessWeights(AIMesh mesh, List<Float> weights, List<String> bones) {
        int numBones = mesh.mNumBones();
        PointerBuffer aiBones = mesh.mBones();
        ArrayList<ArrayList<Float>> temp_weights = new ArrayList<>();
        ArrayList<ArrayList<String>> temp_bones = new ArrayList<>();
        for (int i = 0; i < mesh.mNumVertices(); i++) {
            temp_weights.add(new ArrayList<Float>());
            temp_bones.add(new ArrayList<String>());
        }
        for (int i = 0; i < numBones; i++) {
            AIBone aiBone = AIBone.create(aiBones.get(i));
            int numWeights = aiBone.mNumWeights();
            for (int j = 0; j < numWeights; j++) {
                float weight = aiBone.mWeights().get(j).mWeight();
                int vertex = aiBone.mWeights().get(j).mVertexId();
                temp_weights.get(vertex).add(weight);
                temp_bones.get(vertex).add(aiBone.mName().dataString());
            }
        }
        for (int i = 0; i < temp_weights.size(); i++) {
            for (int j = 0; j < 4; j++) {
                if (j >= temp_weights.get(i).size()) {
                    weights.add(0f);
                    bones.add("temp_bones.get(i).get(0)");
                } else {
                    weights.add(temp_weights.get(i).get(j));
                    bones.add(temp_bones.get(i).get(j));
                }
            }
        }
    }

    public static float[] list_to_array(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static int[] list_to_array_int(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) != null) {
                array[i] = list.get(i);

            }
        }
        return array;
    }

    public static float[] quaternionToEuler(float w, float x, float y, float z) {
        float[] eulerAngles = new float[3];

        // Roll (x-axis rotation)
        float sinr_cosp = 2.0f * (w * x + y * z);
        float cosr_cosp = 1.0f - 2.0f * (x * x + y * y);
        eulerAngles[0] = (float) Math.atan2(sinr_cosp, cosr_cosp); // Roll

        // Pitch (y-axis rotation)
        float sinp = 2.0f * (w * y - z * x);
        if (Math.abs(sinp) >= 1) {
            eulerAngles[1] = Math.copySign((float) (Math.PI / 2), sinp); // Pitch
        } else {
            eulerAngles[1] = (float) Math.asin(sinp); // Pitch
        }

        // Yaw (z-axis rotation)
        float siny_cosp = 2.0f * (w * z + x * y);
        float cosy_cosp = 1.0f - 2.0f * (y * y + z * z);
        eulerAngles[2] = (float) Math.atan2(siny_cosp, cosy_cosp); // Yaw

        return eulerAngles; // Returns an array with Roll, Pitch, and Yaw (in radians)
    }

    public static Vector3 getEulerAnglesFromMatrix(AIMatrix4x4 mat) {

        // Extract the rotation matrix (upper-left 3x3 part of the 4x4 matrix)

        // Convert the rotation matrix to Euler angles

        float pitch, yaw, roll;

        pitch = (float) Math.toDegrees(Math.atan2(mat.c2(), mat.c3()));

        // Yaw (rotation around Y-axis)

        yaw = (float) Math.toDegrees(Math.atan2(-mat.c1(), Math.sqrt(mat.c2() * mat.c2() + mat.c3() * mat.c3())));

        // Roll (rotation around Z-axis)

        roll = (float) Math.toDegrees(Math.atan2(mat.b1(), mat.a1()));

        // Print Euler angles (in radians)

        return new Vector3(pitch,roll, yaw);
    }

    public static int uploadTexelsToOpenGL(AITexel.Buffer texels, int width, int height) {
        // Allocate a ByteBuffer to store RGBA data
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int i = 0; i < texels.remaining(); i++) {
            AITexel texel = texels.get(i);
            buffer.put(texel.r());
            buffer.put(texel.g());
            buffer.put(texel.b());
            buffer.put(texel.a());
        }

        buffer.flip();

        // Create OpenGL texture
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Set default texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        return textureID;
    }

}
