package com.lak.pi;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lak.pi.app.App;
import com.lak.pi.constants.Constants;
import com.lak.pi.model.Image;
import com.lak.pi.util.CustomRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GiftedCoinsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GiftedCoinsFragment extends Fragment implements Constants {

    private CallBackListener callBackListener;

    private static final String ARG_MINCOINS = "minCoins";
    private static final String ARG_COINS = "coins";
    private static final String ARG_IBAN = "iban";
    private static final String ARG_HAVEREQUEST = "haveRequest";
    private static final String ARG_IBANFULLNAME = "ibanFullname";

    private int minCoins;
    private int coins;
    private String iban;
    private String ibanFullname;
    private boolean haveRequest;

    private LinearLayout holder;
    private TextView header;
    private TextView infoExchange;
    TextView infoBox;
    private EditText input;
    private EditText inputFullname;
    private Button btn;

    public GiftedCoinsFragment() {
        // Required empty public constructor
    }

    public static GiftedCoinsFragment newInstance(int minCoins, int coins, String iban, String ibanFullname, boolean haveRequest) {
        GiftedCoinsFragment fragment = new GiftedCoinsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MINCOINS, minCoins);
        args.putInt(ARG_COINS, coins);
        args.putString(ARG_IBAN, iban);
        args.putString(ARG_IBANFULLNAME, ibanFullname);
        args.putBoolean(ARG_HAVEREQUEST, haveRequest);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            minCoins = getArguments().getInt(ARG_MINCOINS);
            coins = getArguments().getInt(ARG_COINS);
            iban = getArguments().getString(ARG_IBAN);
            ibanFullname = getArguments().getString(ARG_IBANFULLNAME);
            haveRequest = getArguments().getBoolean(ARG_HAVEREQUEST);
        }

        Log.d(TAG, "user_id: " + String.valueOf(App.getInstance().getId()));
        Log.d(TAG, "GiftedCoinsFragment onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "GiftedCoinsFragment onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gifted_coins, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "GiftedCoinsFragment onViewCreated");
        Log.d(TAG, "GiftedCoinsFragment onViewCreated min_coins: " + minCoins);
        header = (TextView) getView().findViewById(R.id.header_exchange_page);
        infoExchange = (TextView) getView().findViewById(R.id.info_exchange_page);
        input = (EditText) getView().findViewById(R.id.input_iban);
        inputFullname = (EditText) getView().findViewById(R.id.input_iban_fullname);
        btn = (Button) getView().findViewById(R.id.btn_request_exchange);

        infoBox=(TextView)getView().findViewById(R.id.infoBox);
        holder = (LinearLayout) getView().findViewById(R.id.layout_exchange_page);


        if (App.getInstance().getOtpVerified() == 1) {
            holder.setVisibility(View.VISIBLE);
            infoBox.setVisibility(View.GONE);
        }else {
            holder.setVisibility(View.GONE);
            infoBox.setVisibility(View.VISIBLE);
        }
        if (haveRequest) {
            header.setText("Kredi Bozdurma Şuan Beklemede");
        } else if (coins > 0) {
            if (coins >= minCoins) {
                header.setText("Mevcut Bakiyeniz " + coins + " Kredi");
                if (iban != null && iban != "" && iban != "null") {
                    input.setText(iban);
                }
                if (ibanFullname != null && ibanFullname != "" && ibanFullname != "null") {
                    inputFullname.setText(ibanFullname);
                }
                infoExchange.setVisibility(View.VISIBLE);
                input.setVisibility(View.VISIBLE);
                inputFullname.setVisibility(View.VISIBLE);
                btn.setVisibility(View.VISIBLE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "click");
                        String inputTxt = input.getText().toString();
                        String inputTxtFullName = inputFullname.getText().toString();
                        Log.d(TAG, "inputTxt: " + inputTxt);
                        if(inputTxt != null && inputTxt != "" && inputTxt.length() > 10 &&
                           inputTxtFullName != null && inputTxtFullName != "" && inputTxtFullName.length() > 3)
                           callBackListener.onCallBack(inputTxt, inputTxtFullName, coins);
                    }
                });
            } else {
                header.setText("Mevcut bakiyeniz " + coins + " kredidir. kredi bozdurmak için en az 40 kredinizin olması gerekir [10 kredi 2.50 kuruş değerindedir.]");
            }
        }
        //layout_exchange_page
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //getActivity() is fully created in onActivityCreated and instanceOf differentiate it between different Activities
        if (getActivity() instanceof CallBackListener)
            callBackListener = (CallBackListener) getActivity();
    }

}