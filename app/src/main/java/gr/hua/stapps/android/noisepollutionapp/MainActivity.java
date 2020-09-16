package gr.hua.stapps.android.noisepollutionapp;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    private static final int REC_PERM = 0;
    private static final int LOC_PERM = 1;
    private Handler handler;
    private Recording recording = new Recording(0.0,0.0,0.0);

    private View mLayout;
    double decibels = 0;
    Button rec;
    Button stop;
    Switch upload;
    TableLayout tableLayout;
    TableRow tableRow;
    TableRow average;
    TextView algorithm1;
    TextView average1;
    double avg1 = 0;
    int counter = 0;
    Boolean check_run = false; // Value that determines if user asked to stop recording.
    private Boolean perm = false;

    private StorageReference mStorageRef;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public void setPerm(Boolean perm) {
        this.perm = perm;
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
                Toast.makeText(MainActivity.this, "Please choose an email client to send report of what went wrong!", Toast.LENGTH_LONG).show();
                startActivity(Intent.createChooser(i, "Send email"));
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

        System.out.println(Build.BRAND);
        if(Build.BRAND.contains("samsung"))
            System.out.println("sam is sam");
        else if(Build.BRAND.contains("Xiaomi"))
            System.out.println("xiam is xiam");


        mLayout = findViewById(R.id.main_layout);
        rec = findViewById(R.id.record_button);
        stop = findViewById(R.id.stop_rec);
        upload = findViewById(R.id.upload);
        tableLayout = findViewById(R.id.live_table);
        tableRow = findViewById(R.id.tableRow_avg);
        average = findViewById(R.id.averageTitle);
        algorithm1 = findViewById(R.id.live1);
        average1 = findViewById(R.id.average1);

        stop.setVisibility(View.VISIBLE);
        handler = new Handler();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000); // 2 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if(locationResult ==  null) {
                    System.out.println("locationResult = null");
                }
                for(Location location : locationResult.getLocations()) {
                    if(location !=null) {
                        recording.setLatitude(location.getLatitude());
                        recording.setLongitude(location.getLongitude());
                        //Toast.makeText(MainActivity.this, "Lat:" + recording.getLatitude() + " Lon:" + recording.getLongitude(), Toast.LENGTH_SHORT).show();
                        mFusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (
                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                {
                    System.out.println("Permission to record granted");
                    setPerm(true);
                } else {
                    System.out.println("Permission to record not granted");
                    setPerm(false);
                    requestMicPermission();
                }
            }
        }).start();


        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec.setClickable(false);
                check_run = false;
                avg1 = 0;
                //avg2 = 0;
                counter = 0;

                if (perm) {
                    Log.d("DEBUG", "running thread");
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
                                            if (noiseRecorder.recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                                                noiseRecorder.stopRec();
                                            }
                                            if(upload.isChecked())
                                                accessLocation();
                                        }
                                    });
                                }
                            });

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    upload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            if(isChecked) {

                                                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                                                locationRequest = LocationRequest.create();
                                                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                                locationRequest.setInterval(2 * 1000); // 2 seconds

                                                locationCallback = new LocationCallback() {
                                                    @Override
                                                    public void onLocationResult(LocationResult locationResult) {
                                                        super.onLocationResult(locationResult);
                                                        if(locationResult ==  null) {
                                                            System.out.println("locationResult = null");
                                                        }
                                                        for(Location location : locationResult.getLocations()) {
                                                            if(location !=null) {
                                                                recording.setLatitude(location.getLatitude());
                                                                recording.setLongitude(location.getLongitude());
                                                                //Toast.makeText(MainActivity.this, "Lat:" + recording.getLatitude() + " Lon:" + recording.getLongitude(), Toast.LENGTH_SHORT).show();
                                                                mFusedLocationClient.removeLocationUpdates(locationCallback);
                                                            }
                                                        }
                                                    }
                                                };
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
                                            startLiveRecording(noiseRecorder);
                                        }
                                    });
                                    //counter++;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(Calendar.getInstance().getTimeInMillis() - i > 10000) {
                                noiseRecorder.stopRec();
                                if (upload.isChecked())
                                    accessLocation();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DecimalFormat decimalFormat = new DecimalFormat("#.#");
                                    System.out.println("Counter = " + counter + " avg= " + (avg1) );
                                    average1.setText(decimalFormat.format(avg1 / counter));
                                    //average2.setText(decimalFormat.format(avg2 / counter));
                                    average.setVisibility(View.VISIBLE);
                                    tableRow.setVisibility(View.VISIBLE);
                                    //upload.setVisibility(View.VISIBLE);
                                    //upload.setClickable(true);
                                    //upload.setBackgroundColor(Color.TRANSPARENT);
                                    recording.setAverageDecibels(avg1/counter);
                                    recording.setCurrentDate(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
                                    recording.setCurrentTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                                }
                            });
                            rec.setClickable(true);
                        }
                    }).start();
                } else {
                    Log.d("DEBUG", perm.toString());
                    requestMicPermission();
                    rec.setClickable(true);
                }

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("PERMISSIONS", "onRequestPermissionsResult()");

        if (requestCode == REC_PERM) {
            //Request for permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission has been granted. Start Recording.
                Snackbar.make(mLayout, "Permission granted", Snackbar.LENGTH_SHORT).show();
                setPerm(true);
            } else {
                //Permission request was denied.
                Snackbar.make(mLayout, "Permission denied", Snackbar.LENGTH_SHORT).show();
                setPerm(false);
            }
        }
    }


    private void requestMicPermission() {
        //Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Log.d("PERMISSIONS", "requestMicPermission()/if");
            Snackbar.make(mLayout, "Permission to record audio is required", Snackbar.LENGTH_INDEFINITE).setAction("Audio Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REC_PERM);
                }
            }).show();
        } else {
            Log.d("PERMISSIONS", "requestMicPermission()/else");
            Snackbar.make(mLayout, "Audio recording not available", Snackbar.LENGTH_SHORT).show();
            //Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REC_PERM);
        }
    }

    private void startLiveRecording(NoiseRecorder noiseRecorder) {
        liveRecording liveRecording = new liveRecording();

        DecimalFormat decimalFormat = new DecimalFormat("#.#");

        try {
            //decibels = liveRecording.calculate().get();
            decibels = liveRecording.calculate(noiseRecorder).get();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!Double.isNaN(decibels)) {
            avg1 = (avg1 + decibels);
            algorithm1.setText(decimalFormat.format(decibels));
            counter++;
            System.out.println("decibels " + decibels);
        }

        if (tableLayout.getVisibility() == View.INVISIBLE)
            tableLayout.setVisibility(View.VISIBLE);
    }

    public void accessLocation() {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        System.out.println("in accessLocation()");
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED /*&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED*/) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(MainActivity.this, "Location permission is required", Toast.LENGTH_SHORT).show();
            Snackbar.make(mLayout, "Permission to access gps is required", Snackbar.LENGTH_INDEFINITE).setAction("Location Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_PERM);
                }
            }).show();
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Toast.makeText(MainActivity.this, "Lat:" + location.getLatitude() + " Lon:" + location.getLongitude(), Toast.LENGTH_LONG).show();
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference().child("Recording");
                        recording.setLatitude(location.getLatitude());
                        recording.setLongitude(location.getLongitude());
                        myRef.push().setValue(recording);
                    } else
                        Toast.makeText(MainActivity.this, "GPS cannot be found", Toast.LENGTH_SHORT).show();
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            });


        }

    }

}