package com.llu17.youngq.sqlite_gps;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;



public class MainActivity extends AppCompatActivity  implements SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private boolean[] flag_service;

    private String sampling_rate;
    static TextView upload_state;

    private GoogleApiClient googleApiClient;
    private static final int REQ_CODE = 9001;
    private SignInButton SignIn;
    private ImageButton SignOut;
    private TextView TV_Error;
    private LinearLayout Collection_Section;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flag_service = new boolean[]{false, false, false};

        Collection_Section = (LinearLayout)findViewById(R.id.Collection_section);
        SignIn = (SignInButton)findViewById(R.id.Log_In_Button);
        SignOut = (ImageButton)findViewById(R.id.Log_Out_Button);
        TV_Error = (TextView)findViewById(R.id.TV_Error);
        SignIn.setOnClickListener(this);
        SignOut.setOnClickListener(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        sampling_rate = sharedPreferences.getString(getResources().getString(R.string.sr_key_all),"1000");
        Log.e("-----ALL SR-----",""+sampling_rate);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //////////
        /*===check sqlite data using "chrome://inspect"===*/
        Stetho.initializeWithDefaults(this);
        new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        //////////
        upload_state = (TextView)findViewById(R.id.Upload_State);
        upload_state.setVisibility(android.view.View.GONE);
        Collection_Section.setVisibility(View.GONE);
        TV_Error.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, 10);

        }
        //Log in
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN ).requestEmail().build();
        googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this,this).addApi(Auth.GOOGLE_SIGN_IN_API,googleSignInOptions).build();

        //WhiteList to avoid doze mode
        try {
            Intent intent = new Intent();
            String packageName = this.getPackageName();
            Log.e("pachageName: ", packageName);
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)){

//                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*Sampling rate menu*/
    //Add the menu to the menu bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sampling_rate_menu, menu);
        return true;
    }
    //When the "Settings" menu item is pressed, open SettingsActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.sr_key_all))) {
            sampling_rate = sharedPreferences.getString(key, "1000");
            Log.e("-----ALL SR-----","changed: "+sampling_rate);
        }
    }
    public void startService(View view) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        Log.e("unregister","success");

        Toast.makeText(this, "Starting the service", Toast.LENGTH_SHORT).show();
        flag_service[0] = true;
        startService(new Intent(getBaseContext(), CollectorService.class));
        startService(new Intent(getBaseContext(), Activity_Tracker.class));
    }

    // Method to stop the service
    public void stopService(View view) {
        stopService1();
    }
    private void stopService1(){
        Toast.makeText(this, "Stopping the service", Toast.LENGTH_SHORT).show();
        flag_service[0] = false;
        stopService(new Intent(getBaseContext(), CollectorService.class));
        stopService(new Intent(getBaseContext(), Activity_Tracker.class));
        stopService(new Intent(getBaseContext(), HandleActivity.class));
    }

    public void uploadService(View view){
        Toast.makeText(this, "Begin to upload data automatically", Toast.LENGTH_SHORT).show();
        flag_service[1] = true;
        startService(new Intent(getBaseContext(), UploadService.class));
    }

    public void breakService(View view){
        stopService2();
    }
    private void stopService2(){
        Toast.makeText(this, "Stop to upload data automatically", Toast.LENGTH_SHORT).show();
        flag_service[1] = false;
        stopService(new Intent(getBaseContext(), UploadService.class));
    }

    public void uploadServiceM(View view){
        Toast.makeText(this, "Begin to upload data manually", Toast.LENGTH_SHORT).show();
        flag_service[2] = true;
        startService(new Intent(getBaseContext(), UploadServiceM.class));
    }

    public void breakServiceM(View view){
        stopService3();
    }
    private void stopService3(){
        Toast.makeText(this, "Stop to upload data manually", Toast.LENGTH_SHORT).show();
        flag_service[2] = false;
        stopService(new Intent(getBaseContext(), UploadServiceM.class));
    }

    //prevent turn off app by clicking back button
    //just turn off app by clicking Log Out Button
    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.Log_In_Button:
                login();
                break;
            case R.id.Log_Out_Button:
                logout();
                break;
        }
    }

    private void login() {
        TV_Error.setVisibility(View.GONE);
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(intent, REQ_CODE);
    }

    private void logout(){
        if(flag_service[0] == true)
            stopService1();
        if(flag_service[1] == true)
            stopService2();
        if(flag_service[2] == true)
            stopService3();

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                updateUI(false);
                Log.e("Stop the app","!!!");
            }
        });
    }

    private void updateUI(boolean isLogin ){
        if(isLogin){
            Collection_Section.setVisibility(View.VISIBLE);
            SignIn.setVisibility(View.GONE);
        }
        else{
            Collection_Section.setVisibility(View.GONE);
            SignIn.setVisibility(View.VISIBLE);
        }
    }

    private void handleResult(GoogleSignInResult result ){
        if(result.isSuccess()){
            GoogleSignInAccount account = result.getSignInAccount();
            String email = account.getEmail();
//            Pattern emailPattern = Pattern.compile("\\w+([-+.]\\w+)*@binghamton\\.\\w+([-.]\\w+)*");
            Pattern emailPattern = Pattern.compile("\\w+([-+.]\\w+)*@binghamton\\.edu");
            Matcher matcher = emailPattern.matcher(email);
            if(matcher.find()){
                updateUI(true);
            }
            else{
                logout();
                TV_Error.setVisibility(View.VISIBLE);
            }
        }
        else {
            updateUI(false);
            Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleResult(result);
        }
        else {
            Log.e("request code != ", ""+REQ_CODE);
        }
    }
}
