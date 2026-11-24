package pl.elytrapierscienie;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RingGeometryTest {
    @Test void crossesInEitherDirectionIncludingFastMovement() {
        assertTrue(RingGeometry.crosses(-20, 0, 0, 20, 0, 0, 0, 0, 0, 1, 0, 3));
        assertTrue(RingGeometry.crosses(20, 1, 1, -20, 1, 1, 0, 0, 0, 1, 0, 3));
    }
    @Test void rejectsMissesAndMotionAlongThePlane() {
        assertFalse(RingGeometry.crosses(-5, 3, 3, 5, 3, 3, 0, 0, 0, 1, 0, 3));
        assertFalse(RingGeometry.crosses(0, 0, -2, 0, 0, 2, 0, 0, 0, 1, 0, 3));
        assertFalse(RingGeometry.crosses(-2, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 3));
    }
    @Test void endpointOnPlaneIsCountedOnlyOnce() {
        assertTrue(RingGeometry.crosses(-1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3));
        assertFalse(RingGeometry.crosses(0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 3));
    }
    @Test void rotatedRingUsesCorrectCirclePlane() {
        double n = Math.sqrt(0.5);
        assertTrue(RingGeometry.crosses(-5, 2, -5, 5, 2, 5, 0, 0, 0, n, n, 3));
        assertFalse(RingGeometry.crosses(-7, 0, -3, 3, 0, 7, 0, 0, 0, n, n, 2));
    }
}
