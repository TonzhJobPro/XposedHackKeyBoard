package com.kikatech.hackkeyboardxposedmodule;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by xm on 17/4/19.
 */

public class MainActivity extends Activity implements View.OnClickListener{
    private EditText editText;
    private Button button;
    private TextView textView;

    private static File prefsFile = new File(Environment.getDataDirectory(),
            "data/" + Common.MY_PACKAGE_NAME + "/shared_prefs/" + Common.PREFS + ".xml");

    private SharedPreferences prefs;

    private Button mIKeyboard;
    private Button mGBoard;
    private Button mSwiftKey;
    private Button mTouchPal;
    private Button mGHindi;
    private Button mKikaIndia;
    private Button mFacemoji;
    private Button mTypany;
    private Button mKikaOem;
    private Button mEmojiPro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsFile.setReadable(true, false);
        prefs = getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);

        setContentView(R.layout.main_activity);
        editText = (EditText) findViewById(R.id.editText);
        editText.setText(prefs.getString("packageName", ""));
        button = ((Button) findViewById(R.id.button));
        textView = (TextView) findViewById(R.id.textView);
        textView.setText(BuildConfig.APPLICATION_ID);
        button.setOnClickListener(this);

        mIKeyboard = (Button) findViewById(R.id.btn_ikeyboard);
        mGBoard = (Button) findViewById(R.id.btn_gboard);
        mSwiftKey = (Button) findViewById(R.id.btn_swiftkey);
        mTouchPal = (Button) findViewById(R.id.btn_touchpal);
        mGHindi = (Button) findViewById(R.id.btn_g_hindi);
        mKikaIndia = (Button) findViewById(R.id.btn_kika_india);
        mFacemoji = (Button) findViewById(R.id.btn_facemoji);
        mTypany = (Button) findViewById(R.id.btn_typany);
        mKikaOem = (Button) findViewById(R.id.btn_kika_oem);
        mEmojiPro = (Button) findViewById(R.id.btn_emoji_pro);

        mIKeyboard.setOnClickListener(new MyOnClickListener());
        mGBoard.setOnClickListener(new MyOnClickListener());
        mSwiftKey.setOnClickListener(new MyOnClickListener());
        mTouchPal.setOnClickListener(new MyOnClickListener());
        mGHindi.setOnClickListener(new MyOnClickListener());
        mKikaIndia.setOnClickListener(new MyOnClickListener());
        mFacemoji.setOnClickListener(new MyOnClickListener());
        mTypany.setOnClickListener(new MyOnClickListener());
        mKikaOem.setOnClickListener(new MyOnClickListener());
        mEmojiPro.setOnClickListener(new MyOnClickListener());
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String pkgName = ((Button) v).getText().toString();

            if (!TextUtils.isEmpty(pkgName)) {
                Toast.makeText(MainActivity.this, pkgName, Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString("packageName", pkgName);
                edit.commit();
                Toast.makeText(MainActivity.this, "save SharedPreferences '" + pkgName + "' success", Toast.LENGTH_SHORT).show();
                prefsFile.setReadable(true, false);

                editText.setText(pkgName);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        CharSequence text = editText.getText();
        if (!TextUtils.isEmpty(text)) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("packageName", text.toString());
            edit.commit();
            Toast.makeText(this, "save SharedPreferences success", Toast.LENGTH_SHORT).show();
            prefsFile.setReadable(true, false);
        }
    }
}
