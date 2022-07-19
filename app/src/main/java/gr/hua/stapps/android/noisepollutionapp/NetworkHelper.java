package gr.hua.stapps.android.noisepollutionapp;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NetworkHelper {

    // Server location
    private final String url = "http://83.212.240.46";
    // Firebase Object
    private final String childOf = "Recording";
    private final DatabaseReference databaseReference;
    private final RequestQueue requestQueue;
    private final JSONObject postData;
    private final Context context;
    private final DatabaseReference.CompletionListener resultListener;


    public NetworkHelper(Context context) {
        this.context = context;
        //Firebase Initialization
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child(childOf);
        resultListener = new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    Toast.makeText(context, "Data uploaded to Firebase", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(context, "Could not upload data to Firebase", Toast.LENGTH_SHORT).show();
            }
        };

        //PostgreSQL server initialization
        requestQueue = Volley.newRequestQueue(context);
        postData = new JSONObject();
    }

    public void uploadToFirebase(Recording recording) {
        databaseReference.push().setValue(recording, resultListener);
    }

    public void uploadToPostgreSQL(Recording recording) {
        try {
            postData.put("lon", recording.getLongitude());
            postData.put("lat", recording.getLatitude());
            postData.put("dec", recording.getDecibels());
            postData.put("tim", (recording.getDate() + " " + recording.getTime()) );
            postData.put("gen", recording.getGender());
            postData.put("age", recording.getAge());
            postData.put("ant", recording.getAnthropogenic());
            postData.put("nat", recording.getNatural());
            postData.put("tec", recording.getTechnological());
            postData.put("per", recording.getPerception());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        requestQueue.add(
                new JsonObjectRequest(
                        Request.Method.POST, url.concat("/noise"), postData,
                        response -> {
                            Log.i("Post Response", response.toString());
                            try {
                                Toast.makeText(context, response.get("Response").toString(), Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }, error -> {
                    //                error.printStackTrace();
                    Log.wtf("Error posting", error);
                    Toast.makeText(context, "Upload to remote server was unsuccessful, try again later", Toast.LENGTH_SHORT).show();
                }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json; charset=utf-8");
                        return headers;
                    }
                });
    }
}
