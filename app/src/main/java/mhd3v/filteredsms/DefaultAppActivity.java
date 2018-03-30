package mhd3v.filteredsms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class DefaultAppActivity extends AppCompatActivity {

    private static final int DEF_SMS_REQ = 0;
    Intent MainIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_app);

    }


    public void nextClicked(View view) {

        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, DEF_SMS_REQ);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case DEF_SMS_REQ:
                String currentDefault = Telephony.Sms.getDefaultSmsPackage(this);

                if(currentDefault.equals("mhd3v.filteredsms")){

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                            != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                            != PackageManager.PERMISSION_GRANTED){
                        finish();
                        startActivity(new Intent(this, PermissionsActivity.class));
                    }
                    else {
                        finish();
                        MainIntent = new Intent(this, MainActivity.class);
                        MainIntent.putExtra("cameFromFirst", true);
                        startActivity(MainIntent);
                    }
                }

                else{
                    Toast.makeText(this, "Not set as the default SMS app :(", Toast.LENGTH_SHORT).show();
                }


        }
    }

    public void skipClicked(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED){
           finish();
           startActivity(new Intent(this, PermissionsActivity.class));
        }
        else{
            finish();
            MainIntent = new Intent(this, MainActivity.class);
            MainIntent.putExtra("cameFromFirst", true);
            startActivity(MainIntent);
        }

    }
}
