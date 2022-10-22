package gr.hua.stapps.android.noisepollutionapp.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AppCrashHandler implements Thread.UncaughtExceptionHandler {
    //RFC 822 message format for email
    private final String intentType = "message/rfc822";
    private final String[] recipient = new String[] {"it21549@hua.gr"};
    private final String subject = "Error Report";
    private final String title = "Send email";
    private final Context context;

    public AppCrashHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(intentType);
        intent.putExtra(Intent.EXTRA_EMAIL, recipient);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, Log.getStackTraceString(throwable));

        try {
            Toast.makeText(context, "Please choose an email client to send report of what went wrong!", Toast.LENGTH_LONG).show();
            context.startActivity(Intent.createChooser(intent, title));
        } catch (android.content.ActivityNotFoundException exception) {
            Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }

    }
}
