package com.lak.pi;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.lak.pi.app.App;
import com.lak.pi.constants.Constants;
import com.lak.pi.model.Image;
import com.lak.pi.util.CustomRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//GiftedCoinsFragment
public class GiftedCoinsActivity extends AppCompatActivity implements Constants, CallBackListener {

    Toolbar mToolbar;

    Fragment fragment;
    private CharSequence mTitle;
    public ProgressBar spinner;

    private int minCoins;
    private int coins;
    private String iban;
    private String ibanFullname;
    private boolean haveRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gifted_coins);

        mToolbar = findViewById(R.id.toolbar);
        spinner = (ProgressBar)  findViewById(R.id.progressBar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "currentFragment");
            mTitle = savedInstanceState.getString("mTitle");
        } else {
            fragment = new GiftedCoinsFragment();
            mTitle = getString(R.string.nav_gifts);
        }

        getSupportActionBar().setTitle(mTitle);
        getItems();

        //title bar
        //my credit balance
        //TextView tvToolbartitle = mToolbar.findViewById(R.id.tvToolbartitle);
        //tvToolbartitle.setText("Kredi Bakiyem");

        //info code
       ImageView ivINfo =  mToolbar.findViewById(R.id.toolbar_information);
       ivINfo.setVisibility(View.VISIBLE);
       ivINfo.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lakpi.com/api/v2/method/info.php"));
               Intent browserIntent = new Intent(GiftedCoinsActivity.this, InformationPageActivity.class);
               startActivity(browserIntent);
           }
       });
    }

    public void getItems() {

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_GET_COINS, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());

                        try {
                            Log.d(TAG, "try");

                            if (!response.getBoolean("error")) {
                                minCoins = response.getInt("min_coins");
                                coins = response.getInt("coins");
                                iban = response.getString("iban");
                                ibanFullname = response.getString("iban_fullname");
                                haveRequest = response.getBoolean("haveRequest");
                            }

                        } catch (JSONException e) {
                            Log.d(TAG, "catch");
                            e.printStackTrace();
                            spinner.setVisibility(View.GONE);
                        } finally {
                            Log.d(TAG, "finally");
                            spinner.setVisibility(View.GONE);
                            Bundle bundle = new Bundle();
                            bundle.putInt("minCoins", minCoins);
                            bundle.putInt("coins", coins);
                            bundle.putString("iban", iban);
                            bundle.putString("ibanFullname", ibanFullname);
                            Log.d(TAG, ibanFullname);
                            bundle.putBoolean("haveRequest", haveRequest);
                            fragment.setArguments(bundle);
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.container_body, fragment)
                                    .commit();

                            //loadingComplete();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //loadingComplete();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                //params.put("itemId", Integer.toString(itemId));
                params.put("language", "en");

                return params;
            }
        };

        RetryPolicy policy = new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(VOLLEY_REQUEST_SECONDS), DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonReq.setRetryPolicy(policy);

        App.getInstance().addToRequestQueue(jsonReq);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        getSupportFragmentManager().putFragment(outState, "currentFragment", fragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        fragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {

            case android.R.id.home: {

                finish();

                return true;
            }

            default: {

                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // your code.

        finish();
    }

    @Override
    public void onCallBack(String iban, String fullname, int coins) {
        Log.d(TAG, "onCallBack");
        sendRequest(iban, fullname, coins);
    }

    public void sendRequest(String ibans, String fullname, int coinss) {
        Log.d(TAG, "sendRequest");
        iban = ibans;
        ibanFullname = fullname;
        coins = coinss;

        fragment.getView().findViewById(R.id.layout_exchange_page).setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_EXCHANGE_COINS, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        try {
                            Log.d(TAG, "try");
                            if (!response.getBoolean("error")) {
                                getItems();
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "catch");
                            e.printStackTrace();
                            spinner.setVisibility(View.GONE);

                        } finally {
                            Log.d(TAG, "finally");
                            //spinner.setVisibility(View.GONE);
                            fragment.getView().findViewById(R.id.info_exchange_page).setVisibility(View.GONE);
                            fragment.getView().findViewById(R.id.input_iban).setVisibility(View.GONE);
                            fragment.getView().findViewById(R.id.input_iban_fullname).setVisibility(View.GONE);
                            fragment.getView().findViewById(R.id.btn_request_exchange).setVisibility(View.GONE);
                            TextView header = (TextView) fragment.getView().findViewById(R.id.header_exchange_page);
                            header.setText("İstek gönderildi!");
                            fragment.getView().findViewById(R.id.layout_exchange_page).setVisibility(View.VISIBLE);

                            //loadingComplete();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse");
                //Log.d(TAG, error.getMessage());
                //loadingComplete();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("iban", iban);
                params.put("ibanFullname", ibanFullname);
                params.put("coins", String.valueOf(coins));
                params.put("language", "en");

                return params;
            }
        };

        RetryPolicy policy = new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(VOLLEY_REQUEST_SECONDS), DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonReq.setRetryPolicy(policy);

        App.getInstance().addToRequestQueue(jsonReq);
    }
}

