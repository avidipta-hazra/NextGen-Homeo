package com.webview.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private TextView typingText;
    private final String text = "Powered by: Hazra Tech LTD";
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // STEP 3 STARTS HERE 👇
        View logo = findViewById(R.id.logo);
        logo.animate()
                .alpha(1f)
                .translationYBy(-40)
                .setDuration(1000)
                .setStartDelay(300)
                .start();
        // STEP 3 ENDS HERE 👆

        typingText = findViewById(R.id.typingText);
        startTypingEffect();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 3000);
    }

    private void startTypingEffect() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < text.length()) {
                    typingText.setText(text.substring(0, index + 1));
                    index++;
                    handler.postDelayed(this, 80);
                }
            }
        }, 300);
    }
}
