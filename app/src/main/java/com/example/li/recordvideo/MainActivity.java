package com.example.li.recordvideo;

import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.example.VideoCaptureActivity;
import com.example.configuration.CaptureConfiguration;
import com.example.configuration.PredefinedCaptureConfigurations;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String videofilename = SystemClock.uptimeMillis() + "_VIDEO.mp4";
                CaptureConfiguration captureConfiguration = new CaptureConfiguration(PredefinedCaptureConfigurations.CaptureResolution.RES_720P, PredefinedCaptureConfigurations.CaptureQuality.MEDIUM, 10, 200, true);
                Intent intent = new Intent(MainActivity.this, VideoCaptureActivity.class);
                intent.putExtra(VideoCaptureActivity.EXTRA_OUTPUT_FILENAME, videofilename);
                intent.putExtra(VideoCaptureActivity.EXTRA_CAPTURE_CONFIGURATION,captureConfiguration);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1&&resultCode==RESULT_OK&&data!=null){
           String filepath= data.getStringExtra(VideoCaptureActivity.EXTRA_OUTPUT_FILENAME);
            EditText mEditText= (EditText) findViewById(R.id.editText);
            mEditText.setText(filepath);
        }
    }
}
