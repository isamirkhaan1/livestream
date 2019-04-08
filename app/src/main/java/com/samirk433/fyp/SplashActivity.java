package com.samirk433.fyp;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import data.MyPrefs;

public class SplashActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                MyPrefs prefs = new MyPrefs(SplashActivity.this);
                if (prefs.getUserId() == -1 && prefs.getUsername() == null) {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, HomeActivity.class);
                }

                startActivity(intent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);


    }
}
