package gr.hua.stapps.android.noisepollutionapp;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class liveRecording {



    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public Future<HashMap> calculate() {
        return executor.submit(new Callable<HashMap>() {
            @Override
            public HashMap call() {
                NoiseRecorder noiseRecorder = new NoiseRecorder();
                noiseRecorder.setREC_TIME(0);
                noiseRecorder.setBUF(1);
                return noiseRecorder.getNoiseLevelAverage();
            }
        });
    }
    public Future<HashMap> calculate(final NoiseRecorder noiseRecorder) {
        return executor.submit(new Callable<HashMap>() {
            @Override
            public HashMap call()  {
                return noiseRecorder.startRec();
            }
        });
    }
}

