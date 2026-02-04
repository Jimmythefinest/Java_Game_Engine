package com.njst.gaming.Math;

public class Quaternion {
    public float x, y, z, w;

    public Quaternion() {
        this(0, 0, 0, 1); // Identity rotation
    }

    public Quaternion(float x, float y, float z, float w) {
        this.x = x; this.y = y; this.z = z; this.w = w;
    }

    public static Quaternion fromAxisAngle(float axisX, float axisY, float axisZ, float angleRad) {
        float halfAngle = angleRad / 2f;
        float sin = (float) Math.sin(halfAngle);
        float cos = (float) Math.cos(halfAngle);
        float len = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        return new Quaternion(
                (axisX / len) * sin,
                (axisY / len) * sin,
                (axisZ / len) * sin,
                cos
        );
    }

    public Quaternion normalize() {
        float mag = (float) Math.sqrt(x*x + y*y + z*z + w*w);
        x /= mag; y /= mag; z /= mag; w /= mag;
        return this;
    }

    public Quaternion conjugate() {
        return new Quaternion(-x, -y, -z, w);
    }

    public Quaternion multiply(Quaternion q) {
        return new Quaternion(
                w*q.x + x*q.w + y*q.z - z*q.y,
                w*q.y - x*q.z + y*q.w + z*q.x,
                w*q.z + x*q.y - y*q.x + z*q.w,
                w*q.w - x*q.x - y*q.y - z*q.z
        );
    }

    public float[] rotateVector(float vx, float vy, float vz) {
        Quaternion v = new Quaternion(vx, vy, vz, 0);
        Quaternion result = this.multiply(v).multiply(this.conjugate());
        return new float[]{ result.x, result.y, result.z };
    }

    public float[] toMatrix4() {
        // Column-major 4x4 rotation matrix
        float[] m = new float[16];
        float xx = x*x, yy = y*y, zz = z*z;
        float xy = x*y, xz = x*z, yz = y*z;
        float wx = w*x, wy = w*y, wz = w*z;

        m[ 0] = 1 - 2 * (yy + zz);
        m[ 1] =     2 * (xy - wz);
        m[ 2] =     2 * (xz + wy);
        m[ 3] = 0;

        m[ 4] =     2 * (xy + wz);
        m[ 5] = 1 - 2 * (xx + zz);
        m[ 6] =     2 * (yz - wx);
        m[ 7] = 0;

        m[ 8] =     2 * (xz - wy);
        m[ 9] =     2 * (yz + wx);
        m[10] = 1 - 2 * (xx + yy);
        m[11] = 0;

        m[12] = m[13] = m[14] = 0;
        m[15] = 1;

        return m;
    }

    public static Quaternion lerp(Quaternion q1, Quaternion q2, float t) {
        Quaternion result = new Quaternion(
                q1.x + (q2.x - q1.x) * t,
                q1.y + (q2.y - q1.y) * t,
                q1.z + (q2.z - q1.z) * t,
                q1.w + (q2.w - q1.w) * t
        );
        return result.normalize();
    }

    public static Quaternion slerp(Quaternion q1, Quaternion q2, float t) {
        float dot = q1.x*q2.x + q1.y*q2.y + q1.z*q2.z + q1.w*q2.w;

        if (dot < 0.0f) {
            q2 = new Quaternion(-q2.x, -q2.y, -q2.z, -q2.w);
            dot = -dot;
        }

        if (dot > 0.9995f) {
            return lerp(q1, q2, t); // fallback to linear if very close
        }

        double theta0 = Math.acos(dot);
        double theta = theta0 * t;
        double sinTheta = Math.sin(theta);
        double sinTheta0 = Math.sin(theta0);

        float s1 = (float) (Math.cos(theta) - dot * sinTheta / sinTheta0);
        float s2 = (float) (sinTheta / sinTheta0);

        return new Quaternion(
                q1.x * s1 + q2.x * s2,
                q1.y * s1 + q2.y * s2,
                q1.z * s1 + q2.z * s2,
                q1.w * s1 + q2.w * s2
        ).normalize();
    }
    public static Quaternion fromEuler(float rollDeg, float pitchDeg, float yawDeg) {
        // Convert degrees to radians
        float roll = (float) Math.toRadians(rollDeg);
        float pitch = (float) Math.toRadians(pitchDeg);
        float yaw = (float) Math.toRadians(yawDeg);

        // Half angles
        float halfRoll = roll / 2f;
        float halfPitch = pitch / 2f;
        float halfYaw = yaw / 2f;

        // Compute trigonometric values for each axis
        float cosRoll = (float) Math.cos(halfRoll);
        float sinRoll = (float) Math.sin(halfRoll);
        float cosPitch = (float) Math.cos(halfPitch);
        float sinPitch = (float) Math.sin(halfPitch);
        float cosYaw = (float) Math.cos(halfYaw);
        float sinYaw = (float) Math.sin(halfYaw);

        // Quaternion components
        Quaternion qx = new Quaternion(sinRoll, 0, 0, cosRoll);
        Quaternion qy = new Quaternion(0, sinPitch, 0, cosPitch);
        Quaternion qz = new Quaternion(0, 0, sinYaw, cosYaw);

        // Combine quaternions (q = qz * qy * qx)
        return qz.multiply(qy).multiply(qx);
    }
    public static Quaternion fromEuler(float[] data){
      return fromEuler(data[0],data[1],data[2]);
    }
    public float[] toEuler() {
        float[] euler = new float[3];

        // Roll (x-axis)
        float sinr_cosp = 2 * (w * x + y * z);
        float cosr_cosp = 1 - 2 * (x * x + y * y);
        euler[0] = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (y-axis)
        float sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1)
            euler[1] =(float) Math.copySign(Math.PI / 2, sinp); // Use 90 degrees if out of range
        else
            euler[1] = (float) Math.asin(sinp);

        // Yaw (z-axis)
        float siny_cosp = 2 * (w * z + x * y);
        float cosy_cosp = 1 - 2 * (y * y + z * z);
        euler[2] = (float) Math.atan2(siny_cosp, cosy_cosp);

        // Convert from radians to degrees
        euler[0] = (float) Math.toDegrees(euler[0]);
        euler[1] = (float) Math.toDegrees(euler[1]);
        euler[2] = (float) Math.toDegrees(euler[2]);

        return euler; // euler[0] = roll, euler[1] = pitch, euler[2] = yaw
    }

    
}
