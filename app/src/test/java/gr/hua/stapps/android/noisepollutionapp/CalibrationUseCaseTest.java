package gr.hua.stapps.android.noisepollutionapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalibrationUseCaseTest {

    private static CalibrationUseCase calibrationUseCase = new CalibrationUseCase();

    @Test
    public void calculateBoundary_local_more_than_remote_groupI() {
        double boundary = calibrationUseCase.calculateBoundary(56, 44);
        assertEquals(62, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_less_that_remote_groupI() {
        double boundary = calibrationUseCase.calculateBoundary(32, 44);
        assertEquals(38, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_equal_with_remote_groupI() {
        double boundary = calibrationUseCase.calculateBoundary(44, 44);
        assertEquals(50, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_more_than_remote_groupII() {
        double boundary = calibrationUseCase.calculateBoundary(56, 54);
        assertEquals(62, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_less_that_remote_groupII() {
        double boundary = calibrationUseCase.calculateBoundary(32, 54);
        assertEquals(38, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_equal_with_remote_groupII() {
        double boundary = calibrationUseCase.calculateBoundary(54, 54);
        assertEquals(60, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_more_than_remote_groupIII() {
        double boundary = calibrationUseCase.calculateBoundary(86, 64);
        assertEquals(92, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_less_that_remote_groupIII() {
        double boundary = calibrationUseCase.calculateBoundary(32, 64);
        assertEquals(38, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_equal_with_remote_groupIII() {
        double boundary = calibrationUseCase.calculateBoundary(64, 64);
        assertEquals(70, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_more_than_remote_groupIV() {
        double boundary = calibrationUseCase.calculateBoundary(106, 74);
        assertEquals(112, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_more_than_remote_more_than_group_groupIV() {
        double boundary = calibrationUseCase.calculateBoundary(106, 84);
        assertEquals(102, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_less_that_remote_groupIV() {
        double boundary = calibrationUseCase.calculateBoundary(32, 74);
        assertEquals(38, boundary, 1e-2);
    }

    @Test
    public void calculateBoundary_local_equal_with_remote_groupIV() {
        double boundary = calibrationUseCase.calculateBoundary(74, 74);
        assertEquals(80, boundary, 1e-2);
    }
}
