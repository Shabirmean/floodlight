package net.floodlightcontroller.cienaflowcontroller.utils;

/**
 * Created by shabirmean on 2018-05-28 with some hope.
 */
public enum UPDATE_STATUS{
    INVALID_EVENT_ID("INVALID_EVENT_ID"),
    INCORRECT_TABLE_UPDATE("INCORRECT_TABLE_UPDATE"),
    MATCHING_UPDATE("MATCHING_UPDATE");

    private String status;

    UPDATE_STATUS(String status){
        this.status = status;
    }

    public String getStatus(){
        return this.status;
    }
}
