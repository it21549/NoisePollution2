package gr.hua.stapps.android.noisepollutionapp;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import gr.hua.stapps.android.noisepollutionapp.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NoisePollutionViewModel rec_model;
    //Handler in case of app crash
    private Thread.UncaughtExceptionHandler appCrashHandler = new AppCrashHandler(this);
    private  Recording recording = new Recording();

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    // Permission to record
    private Boolean permissionToRecordGranted = false;
    public void setPermissionToRecordGranted(Boolean permissionToRecordGranted) {
        this.permissionToRecordGranted = permissionToRecordGranted;
    }

    private static final int REC_PERM = 0;
    private static final int LOC_PERM = 1;
    double decibels = 0;

    double avg1 = 0;
    int counter = 0;
    boolean alreadyUploaded;

    private static DecimalFormat decimalFormat = new DecimalFormat("#.#");
    final static Integer NOT_REC = 0; //Not recording
    final static Integer REC = 1; // Recording



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Provide ViewModel
        rec_model = new ViewModelProvider(this).get(NoisePollutionViewModel.class);

        //Handle app crashes with email
        Thread.setDefaultUncaughtExceptionHandler(appCrashHandler);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.user_perception, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        binding.spinnerPerception.setAdapter(adapter);
        //Perception set
        binding.spinnerPerception.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                    setPermissionToRecordGranted(true);
                } else {
                    System.out.println("Permission to record not granted");
                    setPermissionToRecordGranted(false);
                    requestAudioPermission();
                }
            }
        }).start();


        final Observer<Double> rec_observer = aDouble -> {
            if(!aDouble.isNaN()) {
                decibels = aDouble.doubleValue();
                //Setting live recording value
                binding.live1.setText(decimalFormat.format(decibels));
                System.out.println("decibels " + decibels + "/" + aDouble);

                //Setting average live recording value
                System.out.println("Counter = " + counter + " avg = " + avg1);
                avg1 = (avg1 + decibels);
                counter++;
                binding.average1.setText(decimalFormat.format(avg1/counter));
                binding.averageTitle.setVisibility(View.VISIBLE);
                recording.setAverageDecibels(avg1/counter);
                recording.setCurrentDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                recording.setCurrentTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            } else {
                System.out.println("double value is nan");
            }
        };

        final Observer<Integer> up_observer= integer -> {
            Log.i("OnLoopChange", "Checking to upload//loop= " + rec_model.getLoop().getValue());
            if(integer.equals(NOT_REC)) {
                binding.stopRec.setClickable(false);
                binding.recordButton.setClickable(true);
            }
        };

        rec_model.getData().observe(this, rec_observer);
        rec_model.getLoop().observe(this, up_observer);
        

        binding.recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.stopRec.setClickable(true);
                alreadyUploaded = false;
                Log.i("MainActivity", "loop is " + rec_model.getLoop().getValue().toString());
                if (rec_model.getLoop().getValue().equals(NOT_REC)) { //If not recording
                    avg1 = 0;
                    counter = 0;

                    //If permission is granted to record
                    if (permissionToRecordGranted) {
                        Log.d("MainActivity", "running alt rec");
                        startBackgroundRecording();
                        binding.recordButton.setClickable(false);
                    } else {
                        Log.d("DEBUG", permissionToRecordGranted.toString());
                        requestAudioPermission();
                    }
                }
            }
        });

        binding.stopRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec_model.getLoop().setValue(NOT_REC);
                binding.stopRec.setClickable(false);
                binding.recordButton.setClickable(true);
            }
        });

        binding.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!binding.stopRec.isClickable() && !alreadyUploaded) { //while not recording
                    //Set User Info
                    boolean error = false;
                    //Gender info
                    int checked_gender = binding.radioGroupGender.getCheckedRadioButtonId();
                    if (checked_gender == binding.radioMale.getId()) {
                        //Gender is male
                        recording.setGender(0);
                    } else if (checked_gender == binding.radioFem.getId()) {
                        //Gender is female
                        recording.setGender(1);
                    } else if (checked_gender == binding.radioOther.getId()) {
                        //Gender is undefined
                        recording.setGender(2);
                    }
                    //Age info
                    if (!binding.editTextAge.getText().toString().matches("")) {
                        int age = Integer.parseInt(binding.editTextAge.getText().toString());
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
                    if (binding.editTextNoiseTypeAnth.getText().toString().matches(""))
                        anthrop = 0;
                    else
                        anthrop = Integer.parseInt(binding.editTextNoiseTypeAnth.getText().toString());
                    recording.setAnthropogenic(anthrop);
                    //Type Natural
                    int natural;
                    if (binding.editTextNoiseTypeNat.getText().toString().matches(""))
                        natural = 0;
                    else
                        natural = Integer.parseInt(binding.editTextNoiseTypeNat.getText().toString());
                    recording.setNatural(natural);
                    //Type Technological
                    int techno;
                    if (binding.editTextNoiseTypeTec.getText().toString().matches(""))
                        techno = 0;
                    else
                        techno = Integer.parseInt(binding.editTextNoiseTypeTec.getText().toString());
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

        binding.switchAdvanced.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                binding.userInfoTable.setVisibility(View.VISIBLE);
                binding.uploadButton.setVisibility(View.VISIBLE);
            } else {
                binding.userInfoTable.setVisibility(View.INVISIBLE);
                binding.uploadButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void requestAudioPermission() {
        //Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Log.d("PERMISSIONS", "requestMicPermission()/if");
            Snackbar.make(binding.getRoot(), "Permission to record audio is required", Snackbar.LENGTH_INDEFINITE).setAction("Audio Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REC_PERM);
                }
            }).show();
        } else {
            Snackbar.make(binding.getRoot(), "Audio recording not available", Snackbar.LENGTH_SHORT).show();
            //Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REC_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("PERMISSIONS", "onRequestPermissionsResult()");

        if (requestCode == REC_PERM) {
            //Request for permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission has been granted. Start Recording.
                Snackbar.make(binding.getRoot(), "Permission granted", Snackbar.LENGTH_SHORT).show();
                setPermissionToRecordGranted(true);
            } else {
                //Permission request was denied.
                Snackbar.make(binding.getRoot(), "Permission denied", Snackbar.LENGTH_SHORT).show();
                setPermissionToRecordGranted(false);
            }
        }
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
            Snackbar.make(binding.getRoot(), "Permission to access gps is required", Snackbar.LENGTH_INDEFINITE).setAction("Location Permission", new View.OnClickListener() {
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
                        recording.setLatitude(location.getLatitude());
                        recording.setLongitude(location.getLongitude());

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
        NetworkHelper networkHelper = new NetworkHelper(MainActivity.this);
        networkHelper.uploadToFirebase(recording);
        //networkHelper.uploadToPostgreSQL(recording);

    }

    private void startBackgroundRecording() {
        Log.i("MainAct/startBackRec", "1st loop has value: " + rec_model.getLoop().getValue());
        rec_model.getLoop().setValue(REC);
        Log.i("MainAct/startBackRec", "2nd loop has value: " + rec_model.getLoop().getValue());
        rec_model.start();
        Log.i("MainAct/startBackRec","Started backgroundRecording");
        Log.i("MainAct/startBackRec","model hasObservers: " + rec_model.getData().hasObservers());
    }
}