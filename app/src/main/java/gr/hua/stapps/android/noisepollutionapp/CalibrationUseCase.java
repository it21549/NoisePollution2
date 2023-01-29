package gr.hua.stapps.android.noisepollutionapp;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalibrationUseCase {
    public static final double GROUP_I = 50;
    public static final double GROUP_II = 60;
    public static final double GROUP_III = 70;
    public static final double GROUP_IV = 80;

    public static double calculateCalibrationGroup(double measurement) {
        if (measurement < GROUP_I) {
            return GROUP_I;
        } else if (measurement < GROUP_II) {
            return GROUP_II;
        } else if (measurement < GROUP_III) {
            return GROUP_III;
        } else {
            return GROUP_IV;
        }
    }

    public static double calculateBoundary(double localMeasurement, double remoteMeasurement) {
        double remoteFromBoundary = distanceFromBoundary(remoteMeasurement); //>=0
        return localMeasurement + remoteFromBoundary;
    }

    private static double distanceFromBoundary(double measurement) {
        double boundary = calculateCalibrationGroup(measurement);
        return boundary - measurement;
    }

    public static void detectOutliers(List<Double> data) {
        Collections.sort(data);

        int q1Index = (int) (data.size() * 0.25);
        double q1 = data.get(q1Index);
        int q3Index = (int) (data.size() * 0.75);
        double q3 = data.get(q3Index);

        double iqr = q3 - q1;

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        for (double value : data) {
            if (value < lowerBound || value > upperBound) {
                Logger.getGlobal().log(Level.INFO, "outlier: " + value);
            }
        }
    }
}
