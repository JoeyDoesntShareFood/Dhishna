package com.example.jyothisp.dhishna;

public class DhishnaUser {
    private String mName, mEmail, mPhone;
    private int mGender; // 1=male, 2=female.

    public DhishnaUser(String name, String email, String phone, int gender){
        mName = name;
        mEmail = email;
        mPhone = phone;
        mGender = gender;
    }

    public String getmName() {
        return mName;
    }

    public String getmEmail() {
        return mEmail;
    }

    public String getmPhone() {
        return mPhone;
    }

    public int getmGender() {
        return mGender;
    }
}
