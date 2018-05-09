package net.floodlightcontroller.cienaflowcontroller;

import java.io.File;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
class FlowControllerConstants {
    private static final String SEP = File.separator;
    private static final String CIENA = "ciena";
    private static final String CMANAGER_FMANAGER = "cm_fm";
    private static final String FMANAGER_CMANAGER = "fm_cm";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";

    public static final int MAX_PRIORITY = 32768;
    public static final int DEFAULT_FLOW_TABLE = 0;
    public static final String PERIOD = ".";
    public static final String COLON = ":";
    public static final String ZERO = "0";

    static final String CONTAINER_READY = "READY";
    static final String MQTT_BROKER_URI = "tcp://localhost:1883";
    static final String MQTT_SUBSCRIBE_TOPIC = CIENA + SEP + CMANAGER_FMANAGER + SEP + REQUEST + SEP + "+";
    static final String MQTT_PUBLISH_TOPIC = CIENA + SEP + "cmanager" + SEP + FMANAGER_CMANAGER + SEP + RESPONSE;
    static final String RESPONSE_MSG_FORMAT = "{\"eventId\":\"%s\",\"status\":\"%s\"}";

    static final String JSON_ATTRIB_CUSTOMER = "customer";
    static final String JSON_ATTRIB_SUBNET = "subnet";
    static final String JSON_ATTRIB_EVENTID = "eventId";
    static final String JSON_ATTRIB_COUNT = "count";
    static final String JSON_ATTRIB_CONTAINERS = "containers";
//    static final String JSON_ATTRIB_PIPELINE = "pipeline";
    static final String JSON_ATTRIB_IS_INGRESS = "isIngress";
    static final String JSON_ATTRIB_ALLOWED_FLOWS = "allowedFlows";
    static final String JSON_ATTRIB_INDEX = "index";
    static final String JSON_ATTRIB_ID = "id";
    static final String JSON_ATTRIB_NAME = "name";
    static final String JSON_ATTRIB_IP = "ip";
    static final String JSON_ATTRIB_MAC = "mac";

}
