package pl.elytrapierscienie;

final class RingGeometry {
    private RingGeometry() {}

    /** Tests a movement segment against the plane and interior of a vertical circle. */
    static boolean crosses(double fx, double fy, double fz, double tx, double ty, double tz,
                           double cx, double cy, double cz, double nx, double nz, double radius) {
        double from = (fx - cx) * nx + (fz - cz) * nz;
        double to = (tx - cx) * nx + (tz - cz) * nz;
        // Count an endpoint on the plane once, never a segment starting there.
        if (!((from < 0 && to >= 0) || (from > 0 && to <= 0))) return false;
        double t = from / (from - to);
        double x = fx + (tx - fx) * t - cx;
        double y = fy + (ty - fy) * t - cy;
        double z = fz + (tz - fz) * t - cz;
        double horizontal = x * -nz + z * nx;
        return horizontal * horizontal + y * y <= radius * radius;
    }
}
