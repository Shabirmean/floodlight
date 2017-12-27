package net.floodlightcontroller.cienaflowcontroller;

import java.io.File;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
class FlowControllerConstants {
    public static final int MAX_PRIORITY = 32768;
    public static final int DEFAULT_FLOW_TABLE = 0;
    public static final String SEP = File.separator;
    public static final String PERIOD = ".";
    public static final String ZERO = "0";
    static final String CIENA = "ciena";
    static final String CMANAGER_FMANAGER = "cm_fm";
    static final String FMANAGER_CMANAGER = "fm_cm";
    static final String REQUEST = "request";
    static final String RESPONSE = "response";

    static final String MQTT_BROKER_URI = "tcp://localhost:1883";
    static final String MQTT_SUBSCRIBE_TOPIC = CIENA + SEP + CMANAGER_FMANAGER + SEP + REQUEST + SEP + "+";
    static final String MQTT_PUBLISH_TOPIC = CIENA + SEP + FMANAGER_CMANAGER + SEP + RESPONSE;
    static final String RESPONSE_MSG_FORMAT = "{\"eventId\":\"%s\",\"status\":\"%s\"}";

    static final String JSON_ATTRIB_CUSTOMER = "customer";
    static final String JSON_ATTRIB_SUBNET = "subnet";
    static final String JSON_ATTRIB_EVENTID = "eventId";
    static final String JSON_ATTRIB_COUNT = "count";
    static final String JSON_ATTRIB_CONTAINERS = "containers";
    static final String JSON_ATTRIB_PIPELINE = "pipeline";
    static final String JSON_ATTRIB_KEY = "key";
    static final String JSON_ATTRIB_ID = "cId";
    static final String JSON_ATTRIB_NAME = "name";
    static final String JSON_ATTRIB_IP = "ip";
    static final String JSON_ATTRIB_MAC = "mac";

}
