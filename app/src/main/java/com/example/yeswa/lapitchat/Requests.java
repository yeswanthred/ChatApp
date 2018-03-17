package com.example.yeswa.lapitchat;

/**
 * Created by yeswa on 17-03-2018.
 */

public class Requests {

    public String requestType;

    public Requests(){

    }

    public Requests(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
}
