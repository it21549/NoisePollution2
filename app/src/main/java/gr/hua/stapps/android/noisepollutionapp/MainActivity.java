package gr.hua.stapps.android.noisepollutionapp;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    private static final int REC_PERM = 0;
    private static final int LOC_PERM = 1;
    private Handler handler;
    private Recording recording = new Recording(0.0,0.0,0.0, 2, 0, 0, 0, 0, 0);
    private View mLayout;
    double decibels = 0;
    Button rec;
    Button stop;
    Button upload;
    Switch advanced;
    TableLayout tableLayout;
    TableLayout userInfo;
    TableRow tableRow;
    TableRow average;
    TextView algorithm1;
    TextView average1;
    RadioGroup rg_gender;
    RadioButton rb_male;
    RadioButton rb_female;
    RadioButton rb_other;
    EditText age_text;
    EditText anthrop_text;
    EditText natural_text;
    EditText techno_text;
    Spinner perception;

    double avg1 = 0;
    int counter = 0;
    boolean alreadyUploaded;
    //Boolean check_run = false; // Value that determines if user asked to stop recording.
    private Boolean perm = false; // Permission to record

    private StorageReference mStorageRef;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private MyViewModel rec_model;
    private static DecimalFormat decimalFormat = new DecimalFormat("#.#");
    final static Integer NOT_REC = 0; //Not recording
    final static Integer REC = 1; // Recording
    final static Integer UP_REC = 2; //upload recording

    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    //private String url = "http://10.100.59.187:3000";
    private String url = "http://83.212.240.46";
    public void setPerm(Boolean perm) {
        this.perm = perm;
    }


    //Handling Crash Reports
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
        upload = findViewById(R.id.upload_button);
        advanced = findViewById(R.id.switch_advanced);
        tableLayout = findViewById(R.id.live_table);
        tableRow = findViewById(R.id.tableRow_avg);
        average = findViewById(R.id.averageTitle);
        algorithm1 = findViewById(R.id.live1);
        average1 = findViewById(R.id.average1);
        userInfo = findViewById(R.id.userInfoTable);
        rg_gender = findViewById(R.id.radioGroup_gender);
        rb_male = findViewById(R.id.radioMale);
        rb_female = findViewById(R.id.radioFem);
        rb_other = findViewById(R.id.radioOther);
        age_text = findViewById(R.id.editTextAge);
        anthrop_text = findViewById(R.id.editTextNoiseTypeAnth);
        natural_text = findViewById(R.id.editTextNoiseTypeNat);
        techno_text = findViewById(R.id.editTextNoiseTypeTec);
        perception = findViewById(R.id.spinner_perception);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.user_perception, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        perception.setAdapter(adapter);
        //Perception set
        perception.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (parent.getItemAtPosition(position).toString()) {
                    case ("absolute silence"):
                        recording.setPerception(0);
                        break;
                    case ("extremely quiet"):
                        recording.setPerception(1);
                        break;
                    case ("very quiet"):
                        recording.setPerception(2);
                        break;
                    case ("quiet"):
                        recording.setPerception(3);
                        break;
                    case ("moderate"):
                        recording.setPerception(4);
                        break;
                    case ("somewhat noisy"):
                        recording.setPerception(5);
                        break;
                    case ("noisy"):
                        recording.setPerception(6);
                        break;
                    case ("very noisy"):
                        recording.setPerception(7);
                        break;
                    case ("extremely noisy"):
                        recording.setPerception(8);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(MainActivity.this, "Perception cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        handler = new Handler();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        //Location Request Handling
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000); // 2 seconds
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if(locationResult ==  null) {
                    Log.wtf("locationCallback1", "locationResult = null");
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

        //Request permission to record if it is not already granted
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


        rec_model = new ViewModelProvider(this).get(MyViewModel.class);

        final Observer<Double> rec_observer = new Observer<Double>() {

            @Override
            public void onChanged(Double aDouble) {
                if(!aDouble.isNaN()) {
                    decibels = aDouble.doubleValue();
                    //Setting live recording value
                    algorithm1.setText(decimalFormat.format(decibels));
                    System.out.println("decibels " + decibels + "/" + aDouble.toString());

                    //Setting average live recording value
                    System.out.println("Counter = " + counter + " avg = " + avg1);
                    avg1 = (avg1 + decibels);
                    counter++;
                    average1.setText(decimalFormat.format(avg1/counter));
                    average.setVisibility(View.VISIBLE);
                    //tableRow.setVisibility(View.VISIBLE);
                    recording.setAverageDecibels(avg1/counter);
                    //recording.setCurrentDate(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
                    recording.setCurrentDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                    recording.setCurrentTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                } else {
                    System.out.println("double value is nan");
                }
            }
        };

        final Observer<Integer> up_observer= new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Log.i("OnLoopChange", "Checking to upload//loop= " + rec_model.getLoop().getValue());
                if(integer.equals(NOT_REC)) {
                    /*if (upload.isChecked() && !recording.getAverageDecibels().equals(0.0)) {
                        accessLocation();
                        Log.i("OnLoopChange", "Uploading..");
                    }*/
                    stop.setClickable(false);
                    rec.setClickable(true);
                }
            }
        };

        rec_model.getData().observe(this, rec_observer);
        rec_model.getLoop().observe(this, up_observer);
        

        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop.setClickable(true);
                alreadyUploaded = false;
                Log.i("MainActivity", "loop is " + rec_model.getLoop().getValue().toString());
                if (rec_model.getLoop().getValue().equals(NOT_REC)) { //If not recording
                    avg1 = 0;
                    counter = 0;

                    //If permission is granted to record
                    if (perm) {
                        Log.d("MainActivity", "running alt rec");
                        startBackgroundRecording();
                        rec.setClickable(false);
                    } else {
                        Log.d("DEBUG", perm.toString());
                        requestMicPermission();
                    }
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec_model.getLoop().setValue(NOT_REC);
                stop.setClickable(false);
                rec.setClickable(true);
               /* if(upload.isChecked())
                    accessLocation();*/
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!stop.isClickable() && !alreadyUploaded) { //while not recording
                    //Set User Info
                    boolean error = false;
                    //Gender info
                    int checked_gender = rg_gender.getCheckedRadioButtonId();
                    if (checked_gender == rb_male.getId()) {
                        //Gender is male
                        recording.setGender(0);
                    } else if (checked_gender == rb_female.getId()) {
                        //Gender is female
                        recording.setGender(1);
                    } else if (checked_gender == rb_other.getId()) {
                        //Gender is undefined
                        recording.setGender(2);
                    }
                    //Age info
                    if (!age_text.getText().toString().matches("")) {
                        int age = Integer.parseInt(age_text.getText().toString());
                        recording.setAge(age);
                        if (age > 100 || age < 0) {
                            Toast.makeText(MainActivity.this, "Age must be a reasonable number", Toast.LENGTH_SHORT).show();
                            error = true;
                        }
                    } else
                        Toast.makeText(MainActivity.this, "Age field is required", Toast.LENGTH_SHORT).show();

                    //Noise type info
                    //Type Anthropogenic
                    int anthrop;
                    if (anthrop_text.getText().toString().matches(""))
                        anthrop = 0;
                    else
                        anthrop = Integer.parseInt(anthrop_text.getText().toString());
                    recording.setAnthropogenic(anthrop);
                    //Type Natural
                    int natural;
                    if (natural_text.getText().toString().matches(""))
                        natural = 0;
                    else
                        natural = Integer.parseInt(natural_text.getText().toString());
                    recording.setNatural(natural);
                    //Type Technological
                    int techno;
                    if (techno_text.getText().toString().matches(""))
                        techno = 0;
                    else
                        techno = Integer.parseInt(techno_text.getText().toString());
                    recording.setTechnological(techno);

                    int total = anthrop + natural + techno;
                    if (total != 10) {
                        Toast.makeText(MainActivity.this, "Sum of Anthropogenic, Natural and Technological value must equal 10", Toast.LENGTH_SHORT).show();
                        error = true;
                    }

                    //If some error exist inform user to re-submit
                    if (error)
                        Toast.makeText(MainActivity.this, "Please re-submit with correct values", Toast.LENGTH_SHORT).show();
                    else {
                        accessLocation();
                    }
                } else if(alreadyUploaded)
                    Toast.makeText(MainActivity.this, "You have already uploaded, make a new recording to upload!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "You cannot upload while recording!", Toast.LENGTH_SHORT).show();
            }
        });

        advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    userInfo.setVisibility(View.VISIBLE);
                    upload.setVisibility(View.VISIBLE);
                } else {
                    userInfo.setVisibility(View.INVISIBLE);
                    upload.setVisibility(View.INVISIBLE);
                }
            }
        });

        //Record Button
        /*rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //User cannot click 'record' again unless 'stop' button is clicked
                rec.setClickable(false);
                //User has asked to start recording (not stop)
                check_run = false;
                avg1 = 0;
                //avg2 = 0;
                counter = 0;

                //If permission is granted to record
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

                            while (Calendar.getInstance().getTimeInMillis() - i <= 10000 && !check_run *//*&& permission*//*) {
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
        });*/

    }

    private void startBackgroundRecording() {
        Log.i("MainAct/startBackRec", "1st loop has value: " + rec_model.getLoop().getValue());
        rec_model.getLoop().setValue(REC);
        Log.i("MainAct/startBackRec", "2nd loop has value: " + rec_model.getLoop().getValue());
        rec_model.start();
        Log.i("MainAct/startBackRec","Started backgroundRecording");
        Log.i("MainAct/startBackRec","model hasObservers: " + rec_model.getData().hasObservers());
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
                        //Toast.makeText(MainActivity.this, "Lat:" + location.getLatitude() + " Lon:" + location.getLongitude(), Toast.LENGTH_LONG).show();
                        //FirebaseDatabase database = FirebaseDatabase.getInstance();
                        //DatabaseReference myRef = database.getReference().child("Recording");
                        recording.setLatitude(location.getLatitude());
                        recording.setLongitude(location.getLongitude());
                        //myRef.push().setValue(recording);

                        //Send data to database
                        sendAndRequestResponse();
                    } else
                        Toast.makeText(MainActivity.this, "GPS cannot be found", Toast.LENGTH_SHORT).show();
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            });
        }

    }

    private void sendAndRequestResponse() {
        alreadyUploaded = true;

        //Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("Recording");
        myRef.push().setValue(recording);

        //RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);


        //String Request initialized
        /*mStringRequest = new StringRequest(Request.Method.GET, url.concat("/users"), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //display the response on screen
                Toast.makeText(getApplicationContext(), "Response :" + response.toString(), Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("HTTP ERROR", "Error : " + error.toString());
            }
        });
        mRequestQueue.add(mStringRequest);*/

        //Json GET request
        /*final List<String> jsonResponses = new ArrayList<>();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url.concat("/users"), null, new Response.Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        String name = jsonObject.getString("name");
                        String email = jsonObject.getString("email");
                        String id = jsonObject.getString("id");
                        jsonResponses.add(name);
                        jsonResponses.add(email);
                        jsonResponses.add(id);
                    }
                    Toast.makeText(getApplicationContext(), "Response :" + jsonResponses.toString(), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mRequestQueue.add(jsonArrayRequest);*/

        //Json POST request
        JSONObject postData = new JSONObject();
        try {
            postData.put("lon", recording.getLongitude());
            postData.put("lat", recording.getLatitude());
            postData.put("dec", recording.getAverageDecibels());
            postData.put("tim", (recording.getCurrentDate() + " " + recording.getCurrentTime()) );
            postData.put("gen", recording.getGender());
            postData.put("age", recording.getAge());
            postData.put("ant", recording.getAnthropogenic());
            postData.put("nat", recording.getNatural());
            postData.put("tec", recording.getTechnological());
            postData.put("per", recording.getPerception());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url.concat("/noise"), postData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("Post Response", response.toString());
                        try {
                            Toast.makeText(MainActivity.this, response.get("Response").toString(), Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.wtf("Error posting", error);
                    }
                }
                )
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        mRequestQueue.add(jsonObjectRequest);
    }

}