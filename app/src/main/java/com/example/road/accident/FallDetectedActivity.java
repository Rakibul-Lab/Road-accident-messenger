package com.example.road.accident;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.road.accident.app.PreferenceManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.example.road.accident.app.Api.KEY_DIRECTION;
import static com.example.road.accident.app.Api.KEY_LATITUDE;
import static com.example.road.accident.app.Api.KEY_LONGITUDE;
import static com.example.road.accident.app.Api.KEY_POS_X;
import static com.example.road.accident.app.Api.KEY_POS_Y;
import static com.example.road.accident.app.Api.KEY_POS_Z;
import static com.example.road.accident.app.Api.KEY_USER_ID;
import static com.example.road.accident.app.Constant.EXTRA_DIRECITON;
import static com.example.road.accident.app.Constant.EXTRA_POS_X;
import static com.example.road.accident.app.Constant.EXTRA_POS_Y;
import static com.example.road.accident.app.Constant.EXTRA_POS_Z;

@TargetApi(Build.VERSION_CODES.DONUT)
public class FallDetectedActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, TextToSpeech.OnInitListener, LocationListener {

    private static final int REQUEST_ACCESS_LOCATION = 4;
    private static final int REQUEST_CALL_PHONE = 4;
    private Context mContext;
    private TextView mTextViewAlert;
    private Button mButtonNo, mButtonYes;
    private AnimationDrawable mAnimationDrawable;
    private CountDownTimer mCountDownTimer;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    FusedLocationProviderClient fusedLocationClient;
    private TextToSpeech mTextToSpeech;
    private PreferenceManager mPreferenceManager;
    private ProgressDialog mProgressDialog;
    private int mTimeLeft;
    private float mPosX, mPosY, mPosZ;
    private String mDirection;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    String message = "", bestProvider;
    private Criteria criteria;

    private double latitude, longitude;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wakeupScreen();
        setContentView(R.layout.activity_fall_detected);
        initObjects();
        initCallbacks();
        processIntent();
        initCountDownTimer();
        startCountDown();
        buildGoogleApiClient();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        while (latitude == 0) {
            getLocation();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CALL_PHONE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAnimationDrawable != null && !mAnimationDrawable.isRunning()) {
            mAnimationDrawable.start();
        }
        if (mCountDownTimer != null) {
            resetCountDown();
            startCountDown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAnimationDrawable != null && mAnimationDrawable.isRunning()) {
            mAnimationDrawable.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (mCountDownTimer != null) resetCountDown();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }
        if (mGoogleApiClient != null) {

            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mGoogleApiClient != null) mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            mTextToSpeech.setLanguage(Locale.getDefault());
            mTextToSpeech.setPitch(0.8f);
            mTextToSpeech.setSpeechRate(0.8f);
            //noinspection deprecation
            mTextToSpeech.speak("Fall Detected. Did you meet with an accident?", TextToSpeech.QUEUE_FLUSH,
                    null);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mButtonNo) {
            finish();
        } else if (view == mButtonYes) {
            sendAlert();
            String message1 = " Location: https://www.google.com/maps/search/?api=1&query="
                    + latitude + "," + longitude;
            sendSms(message1);
        }
    }

    private void wakeupScreen() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private void initObjects() {
        mTextViewAlert = (TextView) findViewById(R.id.txt_alert);
        mButtonNo = (Button) findViewById(R.id.btn_no);
        mButtonYes = (Button) findViewById(R.id.btn_yes);

        mContext = this;
        mAnimationDrawable = (AnimationDrawable) findViewById(
                R.id.activity_fall_detected).getBackground();
        mTextToSpeech = new TextToSpeech(mContext, this);
        mPreferenceManager = new PreferenceManager(mContext);
        mProgressDialog = new ProgressDialog(mContext);
    }

    private void initCallbacks() {
        mButtonNo.setOnClickListener(this);
        mButtonYes.setOnClickListener(this);
    }

    private void processIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mPosX = bundle.getFloat(EXTRA_POS_X);
            mPosY = bundle.getFloat(EXTRA_POS_Y);
            mPosZ = bundle.getFloat(EXTRA_POS_Z);
            mDirection = bundle.getString(EXTRA_DIRECITON);
        }
    }

    private void initCountDownTimer() {
        mCountDownTimer = new CountDownTimer(30000, 1000) {

            @Override
            public void onTick(long l) {
                mTimeLeft--;
                setTimeLeft();
            }

            @Override
            public void onFinish() {
                mTextViewAlert.setVisibility(View.GONE);
                sendAlert();
                sendSms("Help");
            }
        };
    }

    private void startCountDown() {
        mTimeLeft = 30;
        if (mCountDownTimer != null) mCountDownTimer.start();
    }

    private void resetCountDown() {
        if (mCountDownTimer != null) mCountDownTimer.cancel();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void setTimeLeft() {
        mTextViewAlert.setText(
                String.format(Locale.getDefault(), getString(R.string.format_alert), mTimeLeft));
    }

    private void sendAlert() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_USER_ID, mPreferenceManager.getId());
            jsonObject.put(KEY_POS_X, mPosX);
            jsonObject.put(KEY_POS_Y, mPosY);
            jsonObject.put(KEY_POS_Z, mPosZ);
            jsonObject.put(KEY_DIRECTION, mDirection);

            if (mLocation != null) {
                jsonObject.put(KEY_LATITUDE, mLocation.getLatitude());
                jsonObject.put(KEY_LONGITUDE, mLocation.getLongitude());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_LOCATION);
    }


    private void sendSms(String s) {
        SmsManager smsManager = SmsManager.getDefault();
        String x = mPreferenceManager.getName() + " may have met with an accident. Try to contact "
                + mPreferenceManager.getName() + " soon and help at ";

        smsManager.sendTextMessage(mPreferenceManager.getEmergencyContact1(), null, x, null, null);
        smsManager.sendTextMessage(mPreferenceManager.getEmergencyContact1(), null, s, null, null);

        smsManager.sendTextMessage(mPreferenceManager.getEmergencyContact2(), null, x, null, null);
        smsManager.sendTextMessage(mPreferenceManager.getEmergencyContact2(), null, s, null, null);

        smsManager.sendTextMessage(mPreferenceManager.getEmergencyContact3(), null, x, null, null);
        smsManager.sendTextMessage(mPreferenceManager.getEmergencyContact3(), null, s, null, null);
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();


       startActivity(new Intent(FallDetectedActivity.this, MainActivity.class));
    }


    private void showProgressDialog(String message) {
        mProgressDialog.setMessage(message);
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    protected void getLocation() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        locationManager.removeUpdates(this);
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
