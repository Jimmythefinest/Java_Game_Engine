package com.njst.gaming.Math;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Utils.Matrice_Math;

public class Tetrahedron extends Geometry {
    public Vector3 v1 = new Vector3(), v2 = new Vector3(), v3 = new Vector3(), v4 = new Vector3();
    public Vector3 ov1 = new Vector3(), ov2 = new Vector3(), ov3 = new Vector3(), ov4 = new Vector3();
    boolean modified=false;
    public Tetrahedron() {

    }

    @Override
    public float[] getVertices() {
        float[] v = new float[3 * 4];
        System.arraycopy(v1.toArray(), 0, v, 0, 3);
        System.arraycopy(v2.toArray(), 0, v, 3, 3);
        System.arraycopy(v3.toArray(), 0, v, 6, 3);
        System.arraycopy(v4.toArray(), 0, v, 9, 3);
        return v;

    }

    @Override
    public float[] getNormals() {
        float[] v = new float[3 * 4];
        Vector3 n1 = Normal(this.v1, this.v2, this.v3).normalize();
        Vector3 n2 = Normal(this.v1, this.v4, this.v2).normalize();
        Vector3 n3 = Normal(this.v1, this.v3, this.v4).normalize();
        Vector3 n4 = Normal(this.v4, this.v3, this.v2).normalize();
        Vector3 test = n1.clone().add(n2).add(n3).add(n4);
        System.out.println();
        System.out.println(test);

        Vector3 nv1 = n1.clone().add(n2)
                .add(n3)
                .mul(1.0f / 3.0f);
        Vector3 nv2 = n1.clone()
                .add(n2)
                .add(n4)
                .mul(1.0f / 3.0f);

        Vector3 nv3 = n1.clone()
                .add(n3)
                .add(n4)
                .mul(1.0f / 3.0f);

        Vector3 nv4 = n2.clone()
                .add(n3)
                .add(n4)
                .mul(1.0f / 3.0f);

        System.err.println(n2);

        System.arraycopy(nv1.toArray(), 0, v, 0, 3);
        System.arraycopy(nv2.toArray(), 0, v, 3, 3);
        System.arraycopy(nv3.toArray(), 0, v, 6, 3);
        System.arraycopy(nv4.toArray(), 0, v, 9, 3);
        return v;
    }

    @Override
    public float[] getTextureCoordinates() {

        return new float[] { 0, 0, 0, 1, 1, 1, 1, 0 };
    }

    @Override
    public int[] getIndices() {
        return new int[] { 0, 1, 2,
                0, 1, 3,
                0, 3, 2,
                1, 2, 3
        };
    }

    public Vector3 Normal(Vector3 v1, Vector3 v2, Vector3 v3) {
        Vector3 edge1 = v2.clone().sub(v1);
        Vector3 edge2 = v2.clone().sub(v3);

        return edge1.cross(edge2);
    }

    public static boolean intersectsTriangle(Vector3 origin, Vector3 direction,
        Vector3 v0, Vector3 v1, Vector3 v2) {
        final double EPSILON = 1e-8;
        Vector3 edge1 = v1.clone().sub(v0);
        Vector3 edge2 = v2.clone().sub(v0);

        Vector3 h = direction.cross(edge2);
        double a = edge1.dot(h);

        if (Math.abs(a) < EPSILON)
            return false; // Line is parallel to triangle

        double f = 1.0 / a;
        Vector3 s = origin.clone().sub(v0);
        double u = f * s.dot(h);

        if (u < 0.0 || u > 1.0)
            return false;

        Vector3 q = s.cross(edge1);
        double v = f * direction.dot(q);

        if (v < 0.0 || u + v > 1.0)
            return false;

        double t = f * edge2.dot(q);
        return t >= 0; // Intersection occurs at t units along the ray
    }
    public boolean ray_intersects(Vector3 origin,Vector3 direction){
        return intersectsTriangle(origin, direction, v1, v2, v3)||
        intersectsTriangle(origin, direction, v1, v2, v4)||
        intersectsTriangle(origin, direction, v1, v4, v3)||
        intersectsTriangle(origin, direction, v4, v2, v3);

    }

    public float Volume() {
        return Volume(this.v1, this.v2, this.v3, this.v4);
    }

    public float Volume(Vector3 v1, Vector3 v2, Vector3 v3, Vector3 v4) {
        Vector3 a = v1.clone().sub(v4);
        Vector3 b = v2.clone().sub(v4);
        Vector3 c = v3.clone().sub(v4);

        float Volume = Math.abs(a.dot(b.cross(c))) / 6.0f;
        return Volume;
    }

    public boolean point_is_inside(Vector3 point) {
        float volume = this.Volume();
        float point_volume = Volume(point, v1, v2, v3);
        // System.out.println();
        // System.out.println(point_volume);

        point_volume += Volume(point, v1, v2, v4);
        // System.out.println(point_volume);
        point_volume += Volume(point, v1, v3, v4);
        // System.out.println(point_volume);

        point_volume += Volume(point, v2, v3, v4);
        // System.out.println(point_volume);

        if (point_volume + 0.01f > volume && point_volume - 0.01f < volume) {
            return true;
        }
        // System.out.println(point_volume-volume);
        return false;
    }

    public boolean intersects(Tetrahedron t1) {
        return point_is_inside(t1.v1) || point_is_inside(t1.v2) || point_is_inside(t1.v3) || point_is_inside(t1.v4);
    }

    public void modelMatrix(Matrix4 matrix) {
        if(!modified){
            ov1=v1;
            ov2=v2;
            ov3=v3;
            ov4=v4;
            modified=true;

        }
        v1 = new Vector3(
                Matrice_Math.MatrixMultiplication(new float[] { ov1.x, ov1.y, ov1.z, 1 }, matrix.get(new float[16]), 4));
        v2 = new Vector3(
                Matrice_Math.MatrixMultiplication(new float[] { ov2.x, ov2.y, ov2.z, 1 }, matrix.get(new float[16]), 4));
        v3 = new Vector3(
                Matrice_Math.MatrixMultiplication(new float[] { ov3.x, ov3.y, ov3.z, 1 }, matrix.get(new float[16]), 4));
        v4 = new Vector3(
                Matrice_Math.MatrixMultiplication(new float[] { ov4.x, ov4.y, ov4.z, 1 }, matrix.get(new float[16]), 4));
        // System.out.println(v1);

    }

    public static void main(String[] args) {
        Tetrahedron t = new Tetrahedron();
        t.v1 = new Vector3(0, 0, 0);
        t.v2 = new Vector3(0, 1, 1);
        t.v3 = new Vector3(1, 1, 0);
        t.v4 = new Vector3(0, 1, 0);
       
        // System.out.println(t.ray_intersects(new Vector3(0.5f, 1.5f, 1),(new Vector3(0, 0, -1))));
    }

}
