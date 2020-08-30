package gr.hua.stapps.android.noisepollutionapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
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

    //1-----------------------------------
    String[] appPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
    };

    private static final int PERMISSIONS_REQUEST_CODE = 1240;
    //1------------------------------------


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
    Boolean check_run = false; // Value that determines if user asked to stop recording.
    private Boolean permission = false;

    public void setPermission(Boolean permission) {
        this.permission = permission;
    }

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
                if (
                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                        //2----
                        /*&&
                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)*/
                        //2----
                {
                    System.out.println("Permission to record granted");
                    setPermission(true);
                }  else {
                    System.out.println("Permission to record not granted");
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
                        final NoiseRecorder noiseRecorder = new NoiseRecorder();

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
                                            if(noiseRecorder.recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                                                noiseRecorder.stopRec();
                                            }
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

                                            startLiveRecording(noiseRecorder);
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
                    Log.d("DEBUG", permission.toString());
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

        Log.d("DEBUG", "onRequestPermissionsResult");

        //4----------------
        /*if (requestCode == PERMISSIONS_REQUEST_CODE)
        {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            //Gather permission grant results
            for (int i=0; i<grantResults.length; i++)
            {
                // Add only permissions which are denied
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                {
                    Log.d("DEBUG", "denied permissions : " + permissions[i]);
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            //Check if all permissions are granted
            if (deniedCount == 0) {
                Log.d("DEBUG", "deniedCount = 0");
                setPermission(true);
            }
            else
            {
                for(Map.Entry<String, Integer> entry : permissionResults.entrySet())
                {
                    String permName = entry.getKey();
                    int permResult = entry.getValue();

                    if(permResult == PackageManager.PERMISSION_DENIED)
                    {
                        Snackbar.make(mLayout, permName + " not granted", Snackbar.LENGTH_SHORT).show();
                        setPermission(false);
                    }
                }
            }
        }*/
        //4----------------

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


    private void requestMicPermission() {

        //3---------------------------
        //Check which permissions are granted
        /*List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions)
        {
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        //Ask for non-granted permissions
        if(!listPermissionsNeeded.isEmpty())
        {
            Log.d("NEW_PERM", listPermissionsNeeded.get(0));
            ActivityCompat.requestPermissions
                    (this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE
                    );
            Log.d("DEBUG", "requested permissions");
        } else {
            Log.d("NEW_PERM", "request mic & location 2");
            ActivityCompat.requestPermissions(
                    this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE
            );
        }*/

        //3---------------------------
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

    private void startLiveRecording(NoiseRecorder noiseRecorder) {
        liveRecording liveRecording = new liveRecording();

        //NoiseRecorder noiseRecorder = new NoiseRecorder();
        
        try {
            //decibels = liveRecording.calculate().get();
            decibels = liveRecording.calculate(noiseRecorder).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (decibels.containsKey("Average")) {
            if(!Double.isNaN((Double) decibels.get("Average"))) {
                avg1 = (avg1 + (Double) decibels.get("Average"));
                algorithm1.setText(decibels.get("Average").toString());
            }
        } else
            algorithm1.setText("--");
        if (decibels.containsKey("Algorithm 1 Average")) {
            if(!Double.isNaN((Double) decibels.get("Algorithm 1 Average"))) {
                avg2 = (avg2 + (Double) decibels.get("Algorithm 1 Average"));
                algorithm2.setText(decibels.get("Algorithm 1 Average").toString());
            }
        }else
            algorithm2.setText("--");
        if (tableLayout.getVisibility() == View.INVISIBLE)
            tableLayout.setVisibility(View.VISIBLE);
    }



}