package com.lak.pi;

public interface CallBackListener {
    void onCallBack(String iban, String fullName, int amount);// pass any parameter in your onCallBack which you want to return
}
