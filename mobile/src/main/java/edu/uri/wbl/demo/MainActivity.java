package edu.uri.wbl.demo;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import edu.uri.wbl.wbl_library.ble.BluetoothLeService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = this;
        Button button = findViewById(R.id.send_message_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothLeService.CONNECT(context, "00:43:A8:23:10:F0");
            }
        });
    }
}
