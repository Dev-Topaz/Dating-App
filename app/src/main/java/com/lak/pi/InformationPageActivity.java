package com.lak.pi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

public class InformationPageActivity extends AppCompatActivity {
    Toolbar mToolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information_page);
        mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

       getSupportActionBar().setTitle("Kredi Bilgisi");
        //mToolbar.setTitle("Information");
        //title
       // TextView tvToolbartitle = mToolbar.findViewById(R.id.tvToolbartitle);
       // tvToolbartitle.setText("Kredi Bilgisi");

        WebView webView = (WebView) findViewById(R.id.myWebView);

        webView.loadUrl("http://www.lakpi.com/api/v2/method/info.php");
        webView.getSettings().setJavaScriptEnabled(true);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}