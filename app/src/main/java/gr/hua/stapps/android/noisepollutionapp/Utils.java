package gr.hua.stapps.android.noisepollutionapp;

import java.util.List;

public class Utils {

    //Equivalent Sound Exposure Level (Leq)
    public static Double calculateAverage(List<Double> decibels) {
        double dBSum = 0.0;
        for (int i=0; i<decibels.size(); i++) {
            dBSum += Math.pow(10,(decibels.get(i)*0.1));
        }
        return 10*Math.log10(dBSum/decibels.size());
    }

    //Calculate average amplitudes
    public static Double calculateAvg(short[] sample, int referenceValue, int dBRange) {
        double sum = 0.0;
        int count = 0;
        for (short s : sample) {
            if (s!=0) {
                sum += Math.abs(s);
                count++;
            }
        }
        double x = sum/count;
        //dB calculation
        return 20*Math.log10(x/referenceValue) + dBRange;
    }

    //Calculate dB by mean square of amplitudes
    public static Double calculateLeq(short[] sample, int referenceValue, int dBRange) {
        double dBSum = 0.0;
        for (short amplitude : sample) {
            if (amplitude!=0) {
                dBSum += Math.pow(amplitude, 2) / Math.pow(referenceValue, 2);
            }
        }
        return 10*Math.log10(dBSum/sample.length) + dBRange;
    }
}
