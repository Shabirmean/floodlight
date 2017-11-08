package net.floodlightcontroller.mactracker;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class FlowModifier implements MqttCallback {
    protected static Logger logger;
    MqttClient mqttClient;
    private String OVS_BRIDGE;
    static final String BROKER_URI = "tcp://localhost:1883";
    static final String EVENT_TYPE = "UPLOAD";
    static final String SUBSCRIBE_TOPIC = "ciena" + File.separator + EVENT_TYPE + File.separator + "request/#";
    static final String PUBLISH_TOPIC = "ciena" + File.separator + EVENT_TYPE + File.separator + "reply";
    static final String BELL_FLOW = "BELL";
    static final String FIDO_FLOW = "FIDO";
    static final String OVS_ENDPOINT = "http://localhost:8081/wm/staticflowpusher/json";

    public FlowModifier(String ovsBridge) {
        this.OVS_BRIDGE = ovsBridge;
    }

    void init() {
        logger = LoggerFactory.getLogger(FlowModifier.class);

        Runnable subscriber = () -> {
            try {
                mqttClient = new MqttClient(BROKER_URI, MqttClient.generateClientId(), new MemoryPersistence());
                mqttClient.setCallback(this);
//                this.subscribe();
            } catch (MqttException e) {
                e.printStackTrace();
            }

            while(!mqttClient.isConnected()) {
                try {
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
                    mqttClient.connect(options);

                    if (mqttClient.isConnected()) {
                        logger.info("############# STARTED MQTT Listener...");
                        logger.info("############# Subscribed to : " + SUBSCRIBE_TOPIC);
                        mqttClient.subscribe(SUBSCRIBE_TOPIC, 0);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    logger.error("MQTT Connect-Thread Sleep Interrupt Exception.");
                }
            }
        };

        Thread subscriberThread = new Thread(subscriber);
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("[ERROR] Connection to the broker lost [" + BROKER_URI + "]");
        throwable.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String messageIncoming = mqttMessage.toString();
        String eventIdentifier = topic.substring(topic.lastIndexOf(File.separator) + 1);
        System.out.println(topic + " : " + messageIncoming);
        new MqttPublisher().handleEventRequest(messageIncoming, eventIdentifier);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    private class MqttPublisher{
        MqttPublisher(){
        }

        private void handleEventRequest(String customer, String eventIdentifier) {
            String entryContainerIP = "";
            switch (customer.toUpperCase()) {
                case BELL_FLOW:
                    //TODO:: Setup stuff

                    doPost(StaticFlowEntries.GOTO_TABLE_4);
                    doPost(StaticFlowEntries.GOTO_TABLE_5);
                    doPost(StaticFlowEntries.GOTO_TABLE_6);
                    doPost(StaticFlowEntries.DROP_AT_T4);
                    doPost(StaticFlowEntries.DROP_AT_T5);
                    doPost(StaticFlowEntries.DROP_AT_T6);


                    doPost(StaticFlowEntries.GOTO_TABLE_1);
                    doPost(StaticFlowEntries.GOTO_TABLE_2);
                    doPost(StaticFlowEntries.GOTO_TABLE_3);
                    doPost(StaticFlowEntries.DROP_AT_T1);
                    doPost(StaticFlowEntries.DROP_AT_T2);
                    doPost(StaticFlowEntries.DROP_AT_T3);
                    doPost(StaticFlowEntries.ALLOW_C1_C2);
                    doPost(StaticFlowEntries.ALLOW_C2_C1);
                    doPost(StaticFlowEntries.ALLOW_C2_C3);
                    doPost(StaticFlowEntries.ALLOW_C3_C2);
                    doPost(StaticFlowEntries.NORMAL_FLOW_MODE);
                    entryContainerIP = StaticFlowEntryConstants.CONTAINER_IP.C1.getIP();
                    break;

                case FIDO_FLOW:

                    doPost(StaticFlowEntries.GOTO_TABLE_1);
                    doPost(StaticFlowEntries.GOTO_TABLE_2);
                    doPost(StaticFlowEntries.GOTO_TABLE_3);
                    doPost(StaticFlowEntries.DROP_AT_T1);
                    doPost(StaticFlowEntries.DROP_AT_T2);
                    doPost(StaticFlowEntries.DROP_AT_T3);

                    
                    doPost(StaticFlowEntries.GOTO_TABLE_4);
                    doPost(StaticFlowEntries.GOTO_TABLE_5);
                    doPost(StaticFlowEntries.GOTO_TABLE_6);
                    doPost(StaticFlowEntries.DROP_AT_T4);
                    doPost(StaticFlowEntries.DROP_AT_T5);
                    doPost(StaticFlowEntries.DROP_AT_T6);
                    doPost(StaticFlowEntries.ALLOW_C4_C5);
                    doPost(StaticFlowEntries.ALLOW_C5_C4);
                    doPost(StaticFlowEntries.ALLOW_C5_C6);
                    doPost(StaticFlowEntries.ALLOW_C6_C5);
                    doPost(StaticFlowEntries.NORMAL_FLOW_MODE);
                    entryContainerIP = StaticFlowEntryConstants.CONTAINER_IP.C4.getIP();
                    break;
            }
            publishOK(eventIdentifier, entryContainerIP);
        }


        private boolean doPost(String flowEntry) {
            try {
                URL obj = new URL(OVS_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                connection.setRequestMethod("POST");
                // Send post request
                connection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(flowEntry);
                wr.flush();
                wr.close();

                int responseCode = connection.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + OVS_ENDPOINT);
                System.out.println("Post parameters : \n" + flowEntry);
//            System.out.println("Response Code : " + responseCode);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;

        }

        private void publishOK(String eventIdentifier, String ingressIP) {
            try {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
                MqttClient mqttPublisherClient = new MqttClient(BROKER_URI, MqttClient.generateClientId(), new MemoryPersistence());
                mqttPublisherClient.connect(options);
                String topic = PUBLISH_TOPIC + File.separator + eventIdentifier;
                mqttPublisherClient.publish(topic, (eventIdentifier + ":" + ingressIP).getBytes(UTF_8), 2,
                        false);
                mqttPublisherClient.disconnect();
            } catch (MqttException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

    }
}
