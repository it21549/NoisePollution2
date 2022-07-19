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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;

import gr.hua.stapps.android.noisepollutionapp.databinding.NoisePollutionActivityBinding;


public class NoisePollutionActivity extends AppCompatActivity {

    private NoisePollutionActivityBinding binding;
    private NoisePollutionViewModel rec_model;
    //Handler in case of app crash
    private Thread.UncaughtExceptionHandler appCrashHandler = new AppCrashHandler(this);

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    // Permission to record
    private Boolean permissionToRecordGranted = false;
    public void setPermissionToRecordGranted(Boolean permissionToRecordGranted) {
        this.permissionToRecordGranted = permissionToRecordGranted;
    }

    private static final int RECORDING_PERMISSION = 0;
    private static final int LOCATION_PERMISSION = 1;

    boolean hasBeenUploadedUploaded;

    private static DecimalFormat decimalFormat = new DecimalFormat("#.#");
    final static Integer IS_NOT_RECORDING = 0; //Not recording



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NoisePollutionActivityBinding.inflate(getLayoutInflater());
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
                rec_model.setPerception(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(NoisePollutionActivity.this, "Perception cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        //Location Request Handling
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(NoisePollutionActivity.this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2 * 1000); // 2 seconds
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if(locationResult ==  null) {
                    Log.wtf("locationCallback", "locationResult = null");
                }
                for(Location location : locationResult.getLocations()) {
                    if(location !=null) {
                        rec_model.setLocationData(location.getLatitude(), location.getLongitude());
                        mFusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        };

        //Request permission to record if it is not already granted
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(NoisePollutionActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission to record granted");
                    setPermissionToRecordGranted(true);
                } else {
                    System.out.println("Permission to record not granted");
                    setPermissionToRecordGranted(false);
                    requestAudioPermission();
                }
            }
        }).start();


        final Observer<Double> rec_observer = decibels -> {
            if(!decibels.isNaN()) {
                //Setting live recording value
                binding.liveDecibels.setText(decimalFormat.format(decibels));
                rec_model.recording.setDecibels(decibels);
                rec_model.recordings.add(decibels);
                binding.averageDecibels.setText(decimalFormat.format(Utils.calculateAverage(rec_model.recordings)));
                rec_model.setDateTime();
            } else {
                System.out.println("double value is nan");
            }
        };

        final Observer<Integer> up_observer= integer -> {
            if(integer.equals(IS_NOT_RECORDING)) {
                binding.stopRec.setClickable(false);
                binding.recordButton.setClickable(true);
                rec_model.recordings.clear();
            }
        };

        rec_model.getData().observe(this, rec_observer);
        rec_model.getLoop().observe(this, up_observer);
        

        binding.recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.stopRec.setClickable(true);
                hasBeenUploadedUploaded = false;
                Log.i("MainActivity", "loop is " + rec_model.getLoop().getValue().toString());
                if (rec_model.getLoop().getValue().equals(IS_NOT_RECORDING)) { //If not recording
                    //If permission is granted to record
                    if (permissionToRecordGranted) {
                        Log.d("MainActivity", "starting audio recording");
                        rec_model.startBackgroundRecording();
                        binding.recordButton.setClickable(false);
                    } else {
                        Log.d("DEBUG", permissionToRecordGranted.toString());
                        requestAudioPermission();
                    }
                }
            }
        });

        binding.stopRec.setOnClickListener(v -> {
            rec_model.getLoop().postValue(IS_NOT_RECORDING);
            binding.stopRec.setClickable(false);
            binding.recordButton.setClickable(true);
        });

        binding.uploadButton.setOnClickListener(v -> {
            if (!binding.stopRec.isClickable() && !hasBeenUploadedUploaded) { //while not recording
                //Set User Info
                boolean error = false;

                //Gender info
                int checked_gender = binding.radioGroupGender.getCheckedRadioButtonId();
                if (checked_gender == binding.radioMale.getId()) {
                    //Male
                    rec_model.recording.setGender(0);
                } else if (checked_gender == binding.radioFem.getId()) {
                    //Female
                    rec_model.recording.setGender(1);
                } else if (checked_gender == binding.radioOther.getId()) {
                    //Undefined
                    rec_model.recording.setGender(2);
                }

                //Age info
                if (!binding.editTextAge.getText().toString().matches("")) {
                    int age = Integer.parseInt(binding.editTextAge.getText().toString());
                    rec_model.recording.setAge(age);
                    if (age > 100 || age < 0) {
                        Toast.makeText(NoisePollutionActivity.this, "Age must be a reasonable number", Toast.LENGTH_SHORT).show();
                        error = true;
                    }
                } else
                    Toast.makeText(NoisePollutionActivity.this, "Age field is required", Toast.LENGTH_SHORT).show();

                //Noise type info

                //Type Anthropogenic
                int anthrop;
                if (binding.editTextAnthropogenic.getText().toString().matches(""))
                    anthrop = 0;
                else
                    anthrop = Integer.parseInt(binding.editTextAnthropogenic.getText().toString());
                rec_model.recording.setAnthropogenic(anthrop);

                //Type Natural
                int natural;
                if (binding.editTextNatural.getText().toString().matches(""))
                    natural = 0;
                else
                    natural = Integer.parseInt(binding.editTextNatural.getText().toString());
                rec_model.recording.setNatural(natural);
                //Type Technological
                int techno;
                if (binding.editTextTechnological.getText().toString().matches(""))
                    techno = 0;
                else
                    techno = Integer.parseInt(binding.editTextTechnological.getText().toString());
                rec_model.recording.setTechnological(techno);

                int total = anthrop + natural + techno;
                if (total != 10) {
                    Toast.makeText(NoisePollutionActivity.this, "Sum of Anthropogenic, Natural and Technological value must equal 10", Toast.LENGTH_SHORT).show();
                    error = true;
                }

                //If some error exist inform user to re-submit
                if (error)
                    Toast.makeText(NoisePollutionActivity.this, "Please re-submit with correct values", Toast.LENGTH_SHORT).show();
                else {
                    accessLocation();
                }
            } else if(hasBeenUploadedUploaded)
                Toast.makeText(NoisePollutionActivity.this, "You have already uploaded, make a new recording to upload!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(NoisePollutionActivity.this, "You cannot upload while recording!", Toast.LENGTH_SHORT).show();
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
        if (ActivityCompat.shouldShowRequestPermissionRationale(NoisePollutionActivity.this, Manifest.permission.RECORD_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Log.d("PERMISSIONS", "requestMicPermission()/if");
            Snackbar.make(binding.getRoot(), "Permission to record audio is required", Snackbar.LENGTH_INDEFINITE).setAction("Audio Permission", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Request the permission
                    ActivityCompat.requestPermissions(NoisePollutionActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDING_PERMISSION);
                }
            }).show();
        } else {
            Snackbar.make(binding.getRoot(), "Audio recording not available", Snackbar.LENGTH_SHORT).show();
            //Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(NoisePollutionActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDING_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("PERMISSIONS", "onRequestPermissionsResult()");

        if (requestCode == RECORDING_PERMISSION) {
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
        if (ActivityCompat.checkSelfPermission(NoisePollutionActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED /*&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED*/) {
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
                    ActivityCompat.requestPermissions(NoisePollutionActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
                }
            }).show();
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(NoisePollutionActivity.this, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        rec_model.setLocationData(location.getLatitude(), location.getLongitude());
                        //Send data to database
                        sendAndRequestResponse();
                    } else
                        Toast.makeText(NoisePollutionActivity.this, "GPS cannot be found", Toast.LENGTH_SHORT).show();
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            });
        }
    }

    private void sendAndRequestResponse() {
        hasBeenUploadedUploaded = true;
        NetworkHelper networkHelper = new NetworkHelper(NoisePollutionActivity.this);
        networkHelper.uploadToFirebase(rec_model.recording);
        networkHelper.uploadToPostgreSQL(rec_model.recording);
    }
}