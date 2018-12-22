package com.example.jyothisp.dhishna;

public class NewsItem {
    private String mTitle;
    private String mMessage;
    private String mPhotoUrl;

    public NewsItem(){}

    public NewsItem(String title, String message){
        mTitle = title;
        mMessage = message;
    }

    public NewsItem(String title, String message, String phtotUrl){
        mTitle = title;
        mMessage = message;
        mPhotoUrl = phtotUrl;
    }

    public String getmTitle() {
        return mTitle;
    }

    public String getmMessage() {
        return mMessage;
    }

    public String getmPhotoUrl() {
        return mPhotoUrl;
    }
}
