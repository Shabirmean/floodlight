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

//    private void subscribe() {
//        Runnable subscriber = () -> {
//            while(!mqttClient.isConnected()) {
//                try {
//                    MqttConnectOptions options = new MqttConnectOptions();
//                    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
//                    mqttClient.connect(options);
//
//                    if (mqttClient.isConnected()) {
//                        logger.info("############# STARTED MQTT Listener...");
//                        mqttClient.subscribe(SUBSCRIBE_TOPIC, 1);
//                    }
//                } catch (MqttException e) {
//                    e.printStackTrace();
//                }
//
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException ex) {
//                    logger.error("MQTT Connect-Thread Sleep Interrupt Exception.");
//                }
//            }
//        };
//
//        Thread subscriberThread = new Thread(subscriber);
//        subscriberThread.setDaemon(true);
//        subscriberThread.start();
//    }

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
            switch (customer.toUpperCase()) {
                case BELL_FLOW:
                    //TODO:: Setup stuff
                    String flowEntry1 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-1-2-1\"," +
                            "\"priority\":\"32768\",\"in_port\":\"1\",\"active\":\"true\", \"eth_type\":\"0x0800\"," +
                            "\"eth_src\":\"1e:c7:d9:4c:cb:7d\", \"eth_dst\":\"62:a4:25:4c:7b:a0\", " +
                            "\"ipv4_src\":\"192.168.1.1\",\"ipv4_dst\":\"192.168.1.2\", \"actions\":\"output=normal\"}";

                    String flowEntry2 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-1-other\"," +
                            "\"priority\":\"32767\",\"in_port\":\"1\",\"active\":\"true\", \"eth_type\":\"0x0800\"," +
                            "\"eth_src\":\"1e:c7:d9:4c:cb:7d\", \"ipv4_src\":\"192.168.1.1\", " +
                            "\"instruction_goto_table\":\"1\"}";

                    String flowEntry3 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-1=tab-1\"," +
                            "\"table\":\"1\"," + "\"priority\":\"32768\",\"in_port\":\"1\",\"active\":\"true\", " +
                            "\"eth_type\":\"0x0800\"," + "\"eth_src\":\"1e:c7:d9:4c:cb:7d\", " +
                            "\"ipv4_src\":\"192.168.1.1\", \"actions\":\"\"}";

                    String flowEntry4 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-1-2-3\"," +
                            "\"priority\":\"32768\",\"in_port\":\"2\",\"active\":\"true\", \"eth_type\":\"0x0800\"," +
                            "\"eth_src\":\"62:a4:25:4c:7b:a0\", \"eth_dst\":\"2a:74:6c:22:0f:96\", " +
                            "\"ipv4_src\":\"192.168.1.2\",\"ipv4_dst\":\"192.168.1.3\", \"actions\":\"output=normal\"}";

                    String flowEntry5 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-1-2-2\"," +
                            "\"priority\":\"32768\",\"in_port\":\"2\",\"active\":\"true\", \"eth_type\":\"0x0800\"," +
                            "\"eth_src\":\"62:a4:25:4c:7b:a0\", \"eth_dst\":\"1e:c7:d9:4c:cb:7d\", " +
                            "\"ipv4_src\":\"192.168.1.2\",\"ipv4_dst\":\"192.168.1.1\", \"actions\":\"output=normal\"}";

                    String flowEntry6 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-2-other\"," +
                            "\"priority\":\"32767\",\"in_port\":\"2\",\"active\":\"true\", \"eth_type\":\"0x0800\"," +
                            "\"eth_src\":\"62:a4:25:4c:7b:a0\", \"ipv4_src\":\"192.168.1.2\", " +
                            "\"instruction_goto_table\":\"1\"}";

                    String flowEntry7 = "{\"switch\":\"00:00:d6:ed:a6:a2:0c:44\",\"name\":\"flow-2=tab-1\"," +
                            "\"table\":\"1\",\"priority\":\"32768\",\"in_port\":\"2\",\"active\":\"true\", " +
                            "\"eth_type\":\"0x0800\",\"eth_src\":\"62:a4:25:4c:7b:a0\", \"ipv4_src\":\"192.168.1.2\"," +
                            " \"actions\":\"\"}";

                    doPost(flowEntry1);
                    doPost(flowEntry2);
                    doPost(flowEntry3);
                    doPost(flowEntry4);
                    doPost(flowEntry5);
                    doPost(flowEntry6);
                    doPost(flowEntry7);
                    break;

                case FIDO_FLOW:
                    //TODO:: Setup stuff
                    break;
            }
            publishOK(eventIdentifier, "192.168.1.1");
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
                mqttPublisherClient.publish(PUBLISH_TOPIC, (eventIdentifier + ":" + ingressIP).getBytes(UTF_8), 2, false);
                mqttPublisherClient.disconnect();
            } catch (MqttException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

    }
}
