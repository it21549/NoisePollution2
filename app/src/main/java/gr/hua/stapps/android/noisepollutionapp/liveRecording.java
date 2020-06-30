package gr.hua.stapps.android.noisepollutionapp;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class liveRecording {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public Future<HashMap<String, String>> calculate() {
        return executor.submit(new Callable<HashMap<String, String>>() {
            @Override
            public HashMap<String, String> call() throws Exception {
                NoiseRecorder noiseRecorder = new NoiseRecorder();
                noiseRecorder.setREC_TIME(0);
                noiseRecorder.setBUF(1);
                return noiseRecorder.getNoiseLevelAverage();
            }
        });
    }
}
