package net.floodlightcontroller.mactracker;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class FlowModifier {
    MqttClient mqttClient;
    private String OVS_BRIDGE;
    static final String BROKER_URI = "tcp://localhost:1883";
    static final String EVENT_TYPE = "UPLOAD";
    static final String SUBSCRIBE_TOPIC = "ciena" + File.separator + EVENT_TYPE + File.separator + "#";
    static final String PUBLISH_TOPIC = "ciena" + File.separator + EVENT_TYPE + File.separator + "reply";
    static final String BELL_FLOW = "BELL-FLOW";
    static final String FIDO_FLOW = "FIDO-FLOW";

    public FlowModifier(String ovsBridge) {
        this.OVS_BRIDGE = ovsBridge;
    }
    // "ciena/UPLOAD/#"

    boolean init() {
        try {
            mqttClient = new MqttClient(BROKER_URI, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            mqttClient.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }

        if (!mqttClient.isConnected()) {
            return false;
        }

        this.subscribe();
        return true;
    }


    void subscribe() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) { //Called when the client lost the connection to the broker
                System.out.println("[ERROR] Connection to the broker lost [" + BROKER_URI + "]");
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String messageIncoming = Arrays.toString(message.getPayload());
                String eventIdentifier = topic.substring(topic.lastIndexOf(File.separator) + 1);
                System.out.println(topic + " : " + messageIncoming);
                handleEventRequest(messageIncoming, eventIdentifier);
//                mqttClient.disconnect();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete
            }
        });

        try {
            mqttClient.connect();
            mqttClient.subscribe(SUBSCRIBE_TOPIC, 1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleEventRequest(String customer, String eventIdentifier) {
        switch (customer.toUpperCase()) {
            case "BELL":
                //TODO:: Setup stuff
                break;
            case "FIDO":
                //TODO:: Setup stuff
                break;
        }
        publishOK(eventIdentifier);
    }

    private void publishOK(String eventIdentifier) {
        try {
            mqttClient.publish(PUBLISH_TOPIC, eventIdentifier.getBytes(UTF_8), 2, false);
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
