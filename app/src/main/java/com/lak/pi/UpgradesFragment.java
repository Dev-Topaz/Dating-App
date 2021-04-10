package com.lak.pi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lak.pi.app.App;
import com.lak.pi.constants.Constants;
import com.lak.pi.util.CustomRequest;
import com.lak.pi.util.Prefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpgradesFragment extends Fragment implements Constants {

    private ProgressDialog pDialog;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    Button mGetCreditsButton, mGhostModeButton, mVerifiedBadgeButton, mDisableAdsButton, mProModeButton, mMessagePackageButton,showId;
    TextView mLabelCredits, mLabelGhostModeStatus,labelAddsRemaining,labelGhostModeRemaining, mLabelVerifiedBadgeStatus, mLabelDisableAdsStatus, mLabelProModeStatus, mLabelProModeTitle, mLabelMessagePackageStatus;
    RelativeLayout mMessagePackageContainer;

    private Boolean loading = false;

    public UpgradesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        initpDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_upgrades, container, false);

        if (loading) {

            showpDialog();
        }

        mLabelCredits = rootView.findViewById(R.id.labelCredits);

        mMessagePackageContainer = rootView.findViewById(R.id.messagePackageContainer);

        mLabelGhostModeStatus = rootView.findViewById(R.id.labelGhostModeStatus);
        mLabelVerifiedBadgeStatus = rootView.findViewById(R.id.labelVerifiedBadgeStatus);
        mLabelDisableAdsStatus = rootView.findViewById(R.id.labelDisableAdsStatus);
        mLabelProModeStatus = rootView.findViewById(R.id.labelProModeStatus);
        mLabelProModeTitle = rootView.findViewById(R.id.labelProMode);
        showId = rootView.findViewById(R.id.showId);
        mLabelMessagePackageStatus = rootView.findViewById(R.id.labelMessagePackageStatus);

        mGhostModeButton = rootView.findViewById(R.id.ghostModeBtn);
        labelGhostModeRemaining = rootView.findViewById(R.id.labelGhostModeRemaining);
        labelAddsRemaining = rootView.findViewById(R.id.labelAddsRemaining);
        mVerifiedBadgeButton = rootView.findViewById(R.id.verifiedBadgeBtn);
        mDisableAdsButton = rootView.findViewById(R.id.disableAdsBtn);
        mProModeButton = rootView.findViewById(R.id.proModeBtn);
        mMessagePackageButton = rootView.findViewById(R.id.messagePackageBtn);

        mGetCreditsButton = rootView.findViewById(R.id.getCreditsBtn);

        mGetCreditsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(getActivity(), BalanceActivity.class);
                startActivityForResult(i, 1945);
            }
        });


        mGhostModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (App.getInstance().getBalance() >= App.getInstance().getSettings().getGhostModeCost()) {

                    upgrade(PA_BUY_GHOST_MODE, App.getInstance().getSettings().getGhostModeCost());

                } else {

                    Toast.makeText(getActivity(), getString(R.string.error_credits), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mVerifiedBadgeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (App.getInstance().getBalance() >= App.getInstance().getSettings().getVerifiedBadgeCost()) {

                    upgrade(PA_BUY_VERIFIED_BADGE, App.getInstance().getSettings().getVerifiedBadgeCost());

                } else {

                    Toast.makeText(getActivity(), getString(R.string.error_credits), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mProModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (App.getInstance().getBalance() >= App.getInstance().getSettings().getProModeCost()) {

                    upgrade(PA_BUY_PRO_MODE, App.getInstance().getSettings().getProModeCost());

                } else {

                    Toast.makeText(getActivity(), getString(R.string.error_credits), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mDisableAdsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (App.getInstance().getBalance() >= App.getInstance().getSettings().getDisableAdsCost()) {

                    upgrade(PA_BUY_DISABLE_ADS, App.getInstance().getSettings().getDisableAdsCost());

                } else {

                    Toast.makeText(getActivity(), getString(R.string.error_credits), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mMessagePackageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (App.getInstance().getBalance() >= App.getInstance().getSettings().getMessagePackageCost()) {

                    upgrade(PA_BUY_MESSAGE_PACKAGE, App.getInstance().getSettings().getMessagePackageCost());

                } else {

                    Toast.makeText(getActivity(), getString(R.string.error_credits), Toast.LENGTH_SHORT).show();
                }
            }
        });

        update();

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1945 && resultCode == getActivity().RESULT_OK && null != data) {

            update();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void onDestroyView() {

        super.onDestroyView();

        hidepDialog();
    }

    @Override
    public void onStart() {

        super.onStart();

        update();
    }

    public void update() {

        mLabelCredits.setText(getString(R.string.label_credits2) + " : " + App.getInstance().getBalance());
        mGhostModeButton.setText(""+App.getInstance().getSettings().getGhostModeCost()+" Kredi");
        mVerifiedBadgeButton.setText(""+App.getInstance().getSettings().getVerifiedBadgeCost()+" Kredi");
        mProModeButton.setText(""+App.getInstance().getSettings().getProModeCost()+" Kredi");
        mDisableAdsButton.setText(""+App.getInstance().getSettings().getDisableAdsCost()+" Kredi");
        mMessagePackageButton.setText(""+App.getInstance().getSettings().getMessagePackageCost()+" Kredi");
        Log.e("Upgrade ", ""+Prefs.getIntPref(App.getInstance(),Prefs.GENDER_KEY,-1));
        String showidText = App.getInstance().getResources().getString(R.string.scartched_off);
        showidText = showidText.replace("####",""+App.getInstance().getFreeMessagesCount());
        showId.setText(showidText);
        //showId.setVisibility(View.VISIBLE);


        Log.d(TAG, "update: "+App.getInstance().getFreeMessagesCount());

        if (App.getInstance().getGhost() == 0) {

            mLabelGhostModeStatus.setVisibility(View.GONE);
            mGhostModeButton.setEnabled(true);

            mGhostModeButton.setVisibility(View.VISIBLE);
//            labelGhostModeRemaining.setVisibility(View.GONE);

        } else {

            getGhostModeRemaining();
            mLabelGhostModeStatus.setVisibility(View.VISIBLE);
            mGhostModeButton.setEnabled(false);
            mGhostModeButton.setVisibility(View.GONE);
//            labelGhostModeRemaining.setVisibility(View.VISIBLE);
        }

        if (App.getInstance().getVerify() == 0) {

            mLabelVerifiedBadgeStatus.setVisibility(View.GONE);
            mVerifiedBadgeButton.setEnabled(true);
            mVerifiedBadgeButton.setVisibility(View.VISIBLE);

        } else {
            getverifiedRemaining();
            mLabelVerifiedBadgeStatus.setVisibility(View.VISIBLE);
            mVerifiedBadgeButton.setEnabled(false);
            mVerifiedBadgeButton.setVisibility(View.GONE);
        }

        if (App.getInstance().getPro() == 0) {

            mLabelProModeStatus.setVisibility(View.GONE);
            mProModeButton.setEnabled(true);
            mProModeButton.setVisibility(View.VISIBLE);
//            labelAddsRemaining.setVisibility(View.GONE);
            mLabelProModeTitle.setText(getActivity().getString(R.string.label_upgrades_pro_mode));

            mLabelMessagePackageStatus.setText(String.format(Locale.getDefault(), getString(R.string.free_messages_of), App.getInstance().getFreeMessagesCount()));

        } else {

            mLabelProModeStatus.setVisibility(View.VISIBLE);
            mProModeButton.setEnabled(false);
            mProModeButton.setVisibility(View.GONE);
//            labelAddsRemaining.setVisibility(View.VISIBLE);
            mLabelProModeTitle.setText(getActivity().getString(R.string.label_upgrades_pro_mode));
            getProRemaining();
            mMessagePackageContainer.setVisibility(View.GONE);
            showId.setVisibility(View.GONE);

        }

        if (App.getInstance().getAdmob() == ADMOB_ENABLED) {

            mLabelDisableAdsStatus.setVisibility(View.GONE);

            mDisableAdsButton.setEnabled(true);
            mDisableAdsButton.setVisibility(View.VISIBLE);


        } else {

            mLabelDisableAdsStatus.setVisibility(View.VISIBLE);
            getAddsRemaining();
            mDisableAdsButton.setEnabled(false);
//
            mDisableAdsButton.setVisibility(View.GONE);
        }
    }

    public void getGhostModeRemaining(){
        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("ghost_mode").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    Log.e(TAG, "result =   "+ task.getResult());

                    if (document.exists()) {
                        Calendar cal = Calendar.getInstance();

                        String endDate=getDate(Long.parseLong(String.valueOf(document.getData().get("end_date"))));
                        //cal.setTimeInMillis(Long.parseLong(String.valueOf(document.getData().get("end_date"))));
                        //cal.set(Calendar.DAY_OF_MONTH, -1);
                        String current_date=getDate(cal.getTimeInMillis());
                        Date date1;
                        Date date2;
                        SimpleDateFormat dates = new SimpleDateFormat("dd-MM-yyyy");
                        try {
                            date1 = dates.parse(current_date);
                            date2 = dates.parse(endDate);
                            long difference = Math.abs(date1.getTime() - date2.getTime());
                            long differenceDates = difference / (24 * 60 * 60 * 1000);
                            String dayDifference = Long.toString(differenceDates);
                            System.out.println("GET_____DIFFERENCE "+dayDifference);
                            Log.e(TAG, "diff=   "+ dayDifference);

                            if(differenceDates==0){
                                mLabelGhostModeStatus.setVisibility(View.GONE);
                                mGhostModeButton.setEnabled(true);

                                mGhostModeButton.setVisibility(View.VISIBLE);
                            }else{
                                String verifiedbadgetext = App.getInstance().getResources().getString(R.string.verifiedbadgestatus);
                                verifiedbadgetext = verifiedbadgetext.replace("####",dayDifference);
                                mLabelGhostModeStatus.setText(verifiedbadgetext);
                                mLabelGhostModeStatus.setVisibility(View.VISIBLE);
                                Log.e(TAG, "inside else =   "+verifiedbadgetext );

                            }


                        } catch (ParseException e) {
                            e.printStackTrace();
                            Log.e(TAG, "catch  "+ e.getMessage());

                        }

                        System.out.println("DocumentSnapshot data: "+ document.getData());
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.e(TAG, "onfailure : " + e.getMessage());

            }
        });
    }

    public void getProRemaining(){
        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("pro").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Calendar cal = Calendar.getInstance();

                        String endDate=getDate(Long.parseLong(String.valueOf(document.getData().get("end_date"))));
                        String current_date=getDate(cal.getTimeInMillis());
                        Date date1;
                        Date date2;
                        SimpleDateFormat dates = new SimpleDateFormat("dd-MM-yyyy");
                        try {
                            date1 = dates.parse(current_date);
                            date2 = dates.parse(endDate);
                            long difference = Math.abs(date1.getTime() - date2.getTime());
                            long differenceDates = difference / (24 * 60 * 60 * 1000);
                            String dayDifference = Long.toString(differenceDates);
                            System.out.println("GET_____DIFFERENCE"+dayDifference);
                            if(differenceDates==0){
                                mLabelProModeStatus.setVisibility(View.GONE);
                                mProModeButton.setEnabled(true);
                                mProModeButton.setVisibility(View.VISIBLE);
                            }else{
                                String verifiedbadgetext = App.getInstance().getResources().getString(R.string.verifiedbadgestatus);
                                verifiedbadgetext = verifiedbadgetext.replace("####",dayDifference);
                                mLabelProModeStatus.setText(verifiedbadgetext);
                                mLabelProModeStatus.setVisibility(View.VISIBLE);
                            }


                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        System.out.println("DocumentSnapshot data: "+ document.getData());
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    public void getAddsRemaining(){
        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("ads").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Calendar cal = Calendar.getInstance();

                        String endDate=getDate(Long.parseLong(String.valueOf(document.getData().get("end_date"))));
                        String current_date=getDate(cal.getTimeInMillis());
                        Date date1;
                        Date date2;
                        SimpleDateFormat dates = new SimpleDateFormat("dd-MM-yyyy");
                        try {
                            date1 = dates.parse(current_date);
                            date2 = dates.parse(endDate);
                            long difference = Math.abs(date1.getTime() - date2.getTime());
                            long differenceDates = difference / (24 * 60 * 60 * 1000);
                            String dayDifference = Long.toString(differenceDates);
                            System.out.println("GET_____DIFFERENCE"+dayDifference);
                            if(differenceDates==0){
                                mLabelDisableAdsStatus.setVisibility(View.GONE);
                                mDisableAdsButton.setEnabled(true);
                                mDisableAdsButton.setVisibility(View.VISIBLE);
                            }else{
                                String verifiedbadgetext = App.getInstance().getResources().getString(R.string.verifiedbadgestatus);
                                verifiedbadgetext = verifiedbadgetext.replace("####",dayDifference);
                                mLabelDisableAdsStatus.setText(verifiedbadgetext);
                                mLabelDisableAdsStatus.setVisibility(View.VISIBLE);
                            }


                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        System.out.println("DocumentSnapshot data: "+ document.getData());
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }


    public void getverifiedRemaining(){
        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("verified").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Calendar cal = Calendar.getInstance();

                        String endDate=getDate(Long.parseLong(String.valueOf(document.getData().get("end_date"))));
                        String current_date=getDate(cal.getTimeInMillis());
                        Date date1;
                        Date date2;
                        SimpleDateFormat dates = new SimpleDateFormat("dd-MM-yyyy");
                        try {
                            date1 = dates.parse(current_date);
                            date2 = dates.parse(endDate);
                            long difference = Math.abs(date1.getTime() - date2.getTime());
                            long differenceDates = difference / (24 * 60 * 60 * 1000);
                            String dayDifference = Long.toString(differenceDates);
                            System.out.println("GET_____DIFFERENCE"+dayDifference);
                            if(differenceDates==0){
                                mLabelVerifiedBadgeStatus.setVisibility(View.GONE);
                                mVerifiedBadgeButton.setEnabled(true);
                                mVerifiedBadgeButton.setVisibility(View.VISIBLE);
                            }else{
                                String verifiedbadgetext = App.getInstance().getResources().getString(R.string.verifiedbadgestatus);
                                verifiedbadgetext = verifiedbadgetext.replace("####",dayDifference);
                                mLabelVerifiedBadgeStatus.setText(verifiedbadgetext); // this textyes
                                mLabelVerifiedBadgeStatus.setVisibility(View.VISIBLE);
                            }


                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        System.out.println("DocumentSnapshot data: "+ document.getData());
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private String getDate(long timeStamp){

        try{
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }

    public void upgrade(final int upgradeType, final int credits) {

        loading = true;

        showpDialog();

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_UPGRADE, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            if (!response.getBoolean("error")) {

                                switch (upgradeType) {

                                    case PA_BUY_VERIFIED_BADGE: {

                                        App.getInstance().setBalance(App.getInstance().getBalance() - credits);
                                        App.getInstance().setVerify(1);

                                        Toast.makeText(getActivity(), getString(R.string.msg_success_verified_badge), Toast.LENGTH_SHORT).show();

                                        updateVerified();
                                        break;
                                    }

                                    case PA_BUY_GHOST_MODE: {

                                        App.getInstance().setBalance(App.getInstance().getBalance() - credits);
                                        App.getInstance().setGhost(1);

                                        Toast.makeText(getActivity(), getString(R.string.msg_success_ghost_mode), Toast.LENGTH_SHORT).show();

                                        updateGhostMode();
                                        break;
                                    }

                                    case PA_BUY_DISABLE_ADS: {

                                        App.getInstance().setBalance(App.getInstance().getBalance() - credits);
                                        App.getInstance().setAdmob(ADMOB_DISABLED);

                                        Toast.makeText(getActivity(), getString(R.string.msg_success_disable_ads), Toast.LENGTH_SHORT).show();
                                        updateAds();
                                        break;
                                    }

                                    case PA_BUY_PRO_MODE: {

                                        App.getInstance().setBalance(App.getInstance().getBalance() - credits);
                                        App.getInstance().setPro(1);

                                        Toast.makeText(getActivity(), getString(R.string.msg_success_pro_mode), Toast.LENGTH_SHORT).show();
                                        updatePro();
                                        break;
                                    }

                                    case PA_BUY_MESSAGE_PACKAGE: {

                                        App.getInstance().setBalance(App.getInstance().getBalance() - credits);
                                        App.getInstance().setFreeMessagesCount(App.getInstance().getFreeMessagesCount() + 100);

                                        Toast.makeText(getActivity(), getString(R.string.msg_success_buy_message_package), Toast.LENGTH_SHORT).show();

                                        break;
                                    }

                                    default: {

                                        break;
                                    }
                                }
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            loading = false;

                            hidepDialog();

                            update();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                loading = false;

                update();

                hidepDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("upgradeType", Integer.toString(upgradeType));
                params.put("credits", Integer.toString(credits));

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    private void updateGhostMode() {

        Map<String, Object> GhostMode = new HashMap<>();
        Calendar date = Calendar.getInstance();
        GhostMode.put("start_date", date.getTimeInMillis());

        date.add(Calendar.MONTH, 1);
        GhostMode.put("end_date",date.getTimeInMillis());
        System.out.println("GET___DATE_____"+date.getTimeInMillis()+"_____");


        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("ghost_mode").set(GhostMode).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                System.out.println("Written Successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("Written NOT Successfully"+e.getCause()+"____"+e.getMessage());
            }
        });
    }

    private void updateVerified() {

        Map<String, Object> GhostMode = new HashMap<>();
        Calendar date = Calendar.getInstance();
        GhostMode.put("start_date", date.getTimeInMillis());

        date.add(Calendar.MONTH, 1);
        GhostMode.put("end_date",date.getTimeInMillis());
        System.out.println("GET___DATE_____"+date.getTimeInMillis()+"_____");


        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("verified").set(GhostMode).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                System.out.println("Written Successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("Written NOT Successfully"+e.getCause()+"____"+e.getMessage());
            }
        });
    }

    private void updatePro() {

        Map<String, Object> GhostMode = new HashMap<>();
        Calendar date = Calendar.getInstance();
        GhostMode.put("start_date", date.getTimeInMillis());

        date.add(Calendar.MONTH, 1);
        GhostMode.put("end_date",date.getTimeInMillis());
        System.out.println("GET___DATE_____"+date.getTimeInMillis()+"_____");


        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("pro").set(GhostMode).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                System.out.println("Written Successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("Written NOT Successfully"+e.getCause()+"____"+e.getMessage());
            }
        });
    }

    private void updateAds() {

        Map<String, Object> GhostMode = new HashMap<>();
        Calendar date = Calendar.getInstance();
        GhostMode.put("start_date", date.getTimeInMillis());

        date.add(Calendar.MONTH, 1);
        GhostMode.put("end_date",date.getTimeInMillis());
        System.out.println("GET___DATE_____"+date.getTimeInMillis()+"_____");


        db.collection("lakpi-com1-default-rtdb").document("user_id").collection(String.valueOf(App.getInstance().getId())).document("ads").set(GhostMode).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                System.out.println("Written Successfully");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("Written NOT Successfully"+e.getCause()+"____"+e.getMessage());
            }
        });
    }

    public static Date getCurrentDatePlusMonth(int month)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, month);
        Date newDate = calendar.getTime();
        return newDate;
    }

    protected void initpDialog() {

        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage(getString(R.string.msg_loading));
        pDialog.setCancelable(false);
    }

    protected void showpDialog() {

        if (!pDialog.isShowing()) pDialog.show();
    }

    protected void hidepDialog() {

        if (pDialog.isShowing()) pDialog.dismiss();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}