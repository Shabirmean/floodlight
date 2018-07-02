package net.floodlightcontroller.cienaflowcontroller.utils;

import java.io.File;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
public class FlowControllerConstants {
    private static final String SEP = File.separator;
    private static final String CIENA = "ciena";
    private static final String CMANAGER_FMANAGER = "cm_fm";
    private static final String FMANAGER_CMANAGER = "fm_cm";
    private static final String RESPONSE = "response";
    private static final String EXEC = "exec";
    private static final String TERMINATE = "terminate";

    public static final String INGRESS_CUSTOMER_CONTAINER = "INGRESS_CUSTOMER_CONTAINER";
    public static final String PERIOD = ".";
    public static final String ZERO = "0";

    public static final String REQUEST = "request";
    public static final int MAX_PRIORITY = 32768;
    public static final int DEFAULT_FLOW_TABLE = 0;
    public static final String COLON = ":";

    public static final String CONTAINER_READY = "READY";
    public static final String MQTT_BROKER_URI = "tcp://localhost:1883";
    public static final String MQTT_SUBSCRIBE_TOPIC = CIENA + SEP + CMANAGER_FMANAGER + SEP + "#";
    public static final String MQTT_PUBLISH_EXEC = CIENA + SEP + "cmanager" + SEP + FMANAGER_CMANAGER + SEP + EXEC;
    public static final String MQTT_PUBLISH_READY = CIENA + SEP + "cmanager" + SEP + FMANAGER_CMANAGER + SEP + RESPONSE;
    public static final String MQTT_PUBLISH_TERMINATE = CIENA + SEP + "cmanager" + SEP + FMANAGER_CMANAGER + SEP + TERMINATE;
    public static final String RESPONSE_MSG_FORMAT_READY = "{\"eventId\":\"%s\",\"status\":%s,\"msg\":\"%s\"}";
    public static final String RESPONSE_MSG_FORMAT_TERMINATE = "{\"eventId\":\"%s\",\"msg\":\"%s\"}";

    public static final String JSON_ATTRIB_CUSTOMER = "customer";
    public static final String JSON_ATTRIB_SUBNET = "subnet";
    public static final String JSON_ATTRIB_EVENTID = "eventId";
    public static final String JSON_ATTRIB_COUNT = "count";
    public static final String JSON_ATTRIB_CONTAINERS = "containers";
    public static final String JSON_ATTRIB_IS_INGRESS = "isIngress";
    public static final String JSON_ATTRIB_ALLOWED_FLOWS = "allowedFlows";
    public static final String JSON_ATTRIB_INDEX = "index";
    public static final String JSON_ATTRIB_ID = "id";
    public static final String JSON_ATTRIB_NAME = "name";
    public static final String JSON_ATTRIB_IP = "ip";
    public static final String JSON_ATTRIB_MAC = "mac";

}
