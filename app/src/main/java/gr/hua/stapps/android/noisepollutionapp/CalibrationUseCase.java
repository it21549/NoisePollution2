package gr.hua.stapps.android.noisepollutionapp;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalibrationUseCase {
    public static final double GROUP_I = 55;
    public static final double GROUP_II = 70;
    public static final double GROUP_III = 80;
    public static final double GROUP_IV = 90;

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

    public static double calculateAverage(List<Double> data) {
        removeOutliers(data);
        return Utils.calculateAverage(data);
    }

    public static void removeOutliers(List<Double> data) {
        Collections.sort(data);
        double multiplier = 1.5;

        int q1Index = (int) (data.size() * 0.25);
        double q1 = data.get(q1Index);
        int q3Index = (int) (data.size() * 0.75);
        double q3 = data.get(q3Index);

        double iqr = q3 - q1;

        double lowerBound = q1 - multiplier * iqr;
        double upperBound = q3 + multiplier * iqr;

        for (Iterator<Double> iterator = data.iterator(); iterator.hasNext(); ) {
            Double value = iterator.next();
            if (value < lowerBound || value > upperBound) {
                Logger.getGlobal().log(Level.INFO, "outlier: " + value);
                iterator.remove();
            }
        }
    }
}
