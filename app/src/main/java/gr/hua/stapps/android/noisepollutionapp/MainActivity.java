package gr.hua.stapps.android.noisepollutionapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_TO_RECORD = 0;
    private static final boolean Version = true;
    private Handler handler;


    private View mLayout;
    HashMap decibels=null;
    Button rec;
    Button stop;
    TableLayout tableLayout;
    TableRow tableRow;
    TableRow average;
    TextView algorithm1;
    TextView algorithm2;
    TextView average1;
    TextView average2;
    double avg1 =0;
    double avg2 =0;
    int counter=0;
    Boolean check_run = false;
    private Boolean permission = false;

    public void setPermission(Boolean permission) {
        this.permission = permission;
    }

    /*
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                while(true)
                    showRecordPreview();
            }
        };
    */
    private Thread.UncaughtExceptionHandler handleAppCrash = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e("error", ex.toString());

            //Send email
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"it21549@hua.gr"});
            i.putExtra(Intent.EXTRA_SUBJECT, "Error Report");
            i.putExtra(Intent.EXTRA_TEXT, Log.getStackTraceString(ex));
            try {
                Toast.makeText(MainActivity.this, "Please choose an email client to send report of what went wrong!",Toast.LENGTH_LONG).show();
                startActivity(Intent.createChooser(i,"Send email"));
            } catch (android.content.ActivityNotFoundException x) {
                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);

        mLayout = findViewById(R.id.main_layout);
        rec = findViewById(R.id.record_button);
        stop = findViewById(R.id.stop_rec);
        tableLayout = findViewById(R.id.live_table);
        tableRow = findViewById(R.id.tableRow_avg);
        average = findViewById(R.id.averageTitle);
        algorithm1 = findViewById(R.id.live1);
        algorithm2 = findViewById(R.id.live2);
        average1 = findViewById(R.id.average1);
        average2 = findViewById(R.id.average2);

        stop.setVisibility(View.VISIBLE);
        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    setPermission(true);
                }  else {
                    setPermission(false);
                    requestMicPermission();
                }
            }
        }).start();


        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec.setClickable(false);
                check_run = false;
                if(permission) {
                    new Thread(new Runnable() {
                        long i = Calendar.getInstance().getTimeInMillis();


                        @Override
                        public void run() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    stop.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            check_run = true;
                                            //Toast.makeText(MainActivity.this, "Stopped Recording", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                            while (Calendar.getInstance().getTimeInMillis() - i <= 10000 && !check_run /*&& permission*/) {
                                try {
                                    Thread.sleep(300);

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //showRecordPreview();
                                            startLiveRecording();
                                        }
                                    });
                                    counter++;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    average1.setText(String.valueOf(avg1 / counter));
                                    average2.setText(String.valueOf(avg2 / counter));
                                    average.setVisibility(View.VISIBLE);
                                    tableRow.setVisibility(View.VISIBLE);
                                }
                            });
                            rec.setClickable(true);
                        }
                    }).start();
                } else {
                    requestMicPermission();
                    rec.setClickable(true);
                }
                /*if(Version)
                    showRecordPreview();
                else {
                    //Separate Activity Implementation
                    System.out.println("Button Clicked");
                    //requestAudioPermissions(table);
                    Intent intent = new Intent();
                    intent.setClassName(
                            "gr.hua.stapps.android.noisepollutionapp",
                            "gr.hua.stapps.android.noisepollutionapp.Decibel_Measurements");
                    startActivity(intent);
                }*/

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("PERMISSIONS", "onRequestPermissionsResult()");
        if (requestCode == PERMISSION_TO_RECORD) {
            //Request for Audio permission.
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission has been granted. Start Recording.
                Snackbar.make(mLayout, "Permission to record granted", Snackbar.LENGTH_SHORT).show();
                //startLiveRecording();
                setPermission(true);
            } else {
                //Permission request was denied.
                Snackbar.make(mLayout, "Permission to record denied", Snackbar.LENGTH_SHORT).show();
                setPermission(false);
            }
        }
    }

    private void showRecordPreview() {
        //Check if Record permission is granted.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            //Permission is already available, start recording.
            //Snackbar.make(mLayout, "Permission to record granted", Snackbar.LENGTH_SHORT).show();
            startLiveRecording();
        } else {
            //Permission is missing and must be requested.
            Log.d("PERMISSIONS", "requestPermission()");
            requestMicPermission();
        }
    }

    private void requestMicPermission() {
        //Permission has not been granted and must be requested.
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Log.d("PERMISSIONS", "requestMicPermission()/if");
            Snackbar.make(mLayout, "Permission to record audio is required", Snackbar.LENGTH_INDEFINITE).setAction("Audio Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.RECORD_AUDIO}, PERMISSION_TO_RECORD);
                }
            }).show();
        } else {
            Log.d("PERMISSIONS", "requestMicPermission()/else");
            Snackbar.make(mLayout, "Audio recording not available", Snackbar.LENGTH_SHORT).show();
            //Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_TO_RECORD);
        }
    }

    private void startLiveRecording() {
        liveRecording liveRecording = new liveRecording();

        NoiseRecorder noiseRecorder = new NoiseRecorder();
        
        try {
            //decibels = liveRecording.calculate().get();
            decibels = liveRecording.calculate(noiseRecorder).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (decibels.containsKey("Average")) {
            avg1 =  (avg1 + (Double) decibels.get("Average"));
            algorithm1.setText(decibels.get("Average").toString());
        } else
            algorithm1.setText("--");
        if (decibels.containsKey("Algorithm 1 Average")) {
            avg2 =  (avg2 + (Double) decibels.get("Algorithm 1 Average"));
            algorithm2.setText(decibels.get("Algorithm 1 Average").toString());
        }else
            algorithm2.setText("--");
        if (tableLayout.getVisibility() == View.INVISIBLE)
            tableLayout.setVisibility(View.VISIBLE);
    }



}