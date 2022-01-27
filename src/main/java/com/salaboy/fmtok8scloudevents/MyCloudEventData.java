package com.salaboy.fmtok8scloudevents;

import com.fasterxml.jackson.annotation.JsonClassDescription;

// This class represents the data that I want to send to other applications
@JsonClassDescription("MyCloudEventData")
public class MyCloudEventData {
    private String myData;
    private Integer myCounter;

    public MyCloudEventData() {
    }

    public MyCloudEventData(String myData, Integer myCounter) {
        this.myData = myData;
        this.myCounter = myCounter;
    }

    public String getMyData() {
        return myData;
    }

    public void setMyData(String myData) {
        this.myData = myData;
    }

    public Integer getMyCounter() {
        return myCounter;
    }

    public void setMyCounter(Integer myCounter) {
        this.myCounter = myCounter;
    }

    @Override
    public String toString() {
        return "MyCloudEventData{" +
                "myData='" + myData + '\'' +
                ", myCounter=" + myCounter +
                '}';
    }
}
