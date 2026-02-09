package com.webview.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private TextView typingText;
    private final String text = "🇮🇳 Made in India 🇮🇳 
by NextGen Homoeo. Lab";
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Logo animation
        findViewById(R.id.logo).animate()
                .alpha(1f)
                .translationYBy(-50)
                .setDuration(1000)
                .setStartDelay(300)
                .start();

        typingText = findViewById(R.id.typingText);
        startTypingEffect();
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
                } else {
                    // Typing finished → wait 3 sec → go to MainActivity
                    handler.postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }, 3000);
                }
            }
        }, 300);
    }
}
