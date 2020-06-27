package gr.hua.stapps.android.noisepollutionapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class Decibel_Measurements extends AppCompatActivity {

    private static final int PERMISSION_TO_RECORD = 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decibel__measurements);
        final ProgressBar circular_bar = findViewById(R.id.circularBar);
        final TableLayout tl = findViewById(R.id.results_table);

        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {

                HashMap<String, String> decibels;
                NoiseRecorder nc = new NoiseRecorder();

                //Start 10 second recording
                decibels = nc.getNoiseLevel();

                resultPortray(tl, circular_bar, decibels);
                return null;
            }
        };


        //Creating TableLayout
/*
        try {


            //circular_bar.setVisibility(View.GONE);
            resultPortray(tl, circular_bar);
        } catch (Exception e) {
            Toast.makeText(Decibel_Measurements.this,
                    e.toString(),
            Toast.LENGTH_LONG).show();
        }
*/
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_TO_RECORD) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission granted
            } else
                //Permission denied
                Toast.makeText(this, "Permission to record Denied", Toast.LENGTH_LONG).show();
        }
    }


    public void resultPortray(TableLayout tl, ProgressBar progressBar, HashMap<String, String> decibels) {
        if( ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //When permission is not granted, show following message
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permission to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_TO_RECORD);
            } else {
                //Ask again for permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_TO_RECORD);
            }
            //If permission is granted, record
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {


            //Creating tableRow headers to hold the column headings
            TableRow tr_head = new TableRow(this);
            tr_head.setBackgroundColor(Color.CYAN);
            tr_head.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.MATCH_PARENT,
                    1.0f
            ));

            //Adding textViews as headers to each table row
            TextView label_avg = new TextView(this);
            label_avg.setText(R.string.avg_header);
            label_avg.setTextColor(Color.BLACK);
            label_avg.setPadding(5, 5, 5, 5);
            tr_head.addView(label_avg); //add textview to table row

            TextView label_max = new TextView(this);
            label_max.setText(R.string.max_header);
            label_max.setTextColor(Color.BLACK);
            label_max.setPadding(5, 5, 5, 5);
            tr_head.addView(label_max);

            TextView label_alt_avg = new TextView(this);
            label_alt_avg.setText(R.string.alt_avg_header);
            label_alt_avg.setTextColor(Color.BLACK);
            label_alt_avg.setPadding(5, 5, 5, 5);
            tr_head.addView(label_alt_avg);

            TextView label_alt_max = new TextView(this);
            label_alt_max.setText(R.string.alt_max_header);
            label_alt_max.setTextColor(Color.BLACK);
            label_alt_max.setPadding(5, 5, 5, 5);
            tr_head.addView(label_alt_max);

            progressBar.setVisibility(View.GONE);
            tl.setVisibility(View.VISIBLE);
            tl.addView(tr_head, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
            ));

            //Creating tableRows for each object in decibel value
            TableRow tablerow = new TableRow(this);
            tablerow.setLayoutParams(new TableRow.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1.0f
            ));

            //Average Decibel recorded
            TextView txt_avg = new TextView(this);
            txt_avg.setPadding(5, 5, 5, 5);
            //Check for Null values
            if ( decibels.containsKey("Average"))
                txt_avg.setText(decibels.get("Average"));
            else
                txt_avg.setText(" -- ");
            tablerow.addView(txt_avg);

            //Maximum Decibel recorded
            TextView txt_max = new TextView(this);
            txt_max.setPadding(5, 5, 5, 5);
            //Check for Null values
            System.out.println("Why infinite? " + decibels.get("Maximum") + "/" + decibels.get("Maximum"));
            if (decibels.containsKey("Maximum"))
                txt_max.setText(decibels.get("Maximum"));
            else
                txt_max.setText(" -- ");
            tablerow.addView(txt_max);

            //Alternate calculation of Average Decibel recorded
            TextView txt_alt_avg = new TextView(this);
            txt_alt_avg.setPadding(5, 5, 5, 5);
            //Check for Null values
            if (decibels.containsKey("Algorithm 1 Average"))
                txt_alt_avg.setText(decibels.get("Algorithm 1 Average"));
            else
                txt_alt_avg.setText(" -- ");
            tablerow.addView(txt_alt_avg);

            //Alternate calculation of Max Decibel recorded
            TextView txt_alt_max = new TextView(this);
            txt_alt_max.setPadding(5, 5, 5, 5);
            //Check for Null values
            if (decibels.containsKey("Algorithm 2 Maximum"))
                txt_alt_max.setText(decibels.get("Algorithm 2 Maximum"));
            else
                txt_alt_max.setText(" -- ");
            tablerow.addView(txt_alt_max);

            //Add new row
            tl.addView(tablerow);
        }
    }
}