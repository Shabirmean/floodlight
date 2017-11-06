package net.floodlightcontroller.mactracker;


import org.apache.http.HttpHeaders;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class FlowModifier {
    protected static Logger logger;
    MqttClient mqttClient;
    private String OVS_BRIDGE;
    static final String BROKER_URI = "tcp://localhost:1883";
    static final String EVENT_TYPE = "UPLOAD";
    static final String SUBSCRIBE_TOPIC = "ciena" + File.separator + EVENT_TYPE + File.separator + "#";
    static final String PUBLISH_TOPIC = "ciena" + File.separator + EVENT_TYPE + File.separator + "reply";
    static final String BELL_FLOW = "BELL";
    static final String FIDO_FLOW = "FIDO";
    static final String OVS_ENDPOINT = "http://localhost:8081/wm/staticflowpusher/json";

    public FlowModifier(String ovsBridge) {
        this.OVS_BRIDGE = ovsBridge;
    }
    // "ciena/UPLOAD/#"

    boolean init() {
        logger = LoggerFactory.getLogger(FlowModifier.class);
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
        logger.info("############# STARTED MQTT Listener...");
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
//                String messageIncoming = Arrays.toString(message.getPayload());
                String messageIncoming = message.toString();
                ;
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
            mqttClient.subscribe(SUBSCRIBE_TOPIC, 1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleEventRequest(String customer, String eventIdentifier) {
        try {
            switch (customer.toUpperCase()) {
                case BELL_FLOW:
                    //TODO:: Setup stuff
                    URL obj = new URL(OVS_ENDPOINT);

                    HttpsURLConnection connection = (HttpsURLConnection) obj.openConnection();
                    connection.setHostnameVerifier((arg0, arg1) -> true);
                    //add request header
                    connection.setRequestMethod("POST");
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

                            // Send post request
                    connection.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(flowEntry1);
                    wr.flush();
                    wr.writeBytes(flowEntry2);
                    wr.flush();
                    wr.writeBytes(flowEntry3);
                    wr.flush();
                    wr.writeBytes(flowEntry4);
                    wr.flush();
                    wr.writeBytes(flowEntry5);
                    wr.flush();
                    wr.writeBytes(flowEntry6);
                    wr.flush();
                    wr.writeBytes(flowEntry7);
                    wr.flush();
                    wr.close();

                    int responseCode = connection.getResponseCode();
                    System.out.println("\nSending 'POST' request to URL : " + OVS_ENDPOINT);
                    System.out.println("Post parameters : \n" + flowEntry1 + "\n\n" +
                            flowEntry2 + "\n\n" + flowEntry3 + "\n\n" + flowEntry4 + "\n\n" +
                            flowEntry5 + "\n\n" + flowEntry6 + "\n\n" + flowEntry7);
                    System.out.println("Response Code : " + responseCode);

                    break;

                case FIDO_FLOW:
                    //TODO:: Setup stuff
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        publishOK(eventIdentifier, "192.168.1.1");
    }

    private void publishOK(String eventIdentifier, String ingressIP) {
        try {
            mqttClient.publish(PUBLISH_TOPIC, (eventIdentifier + ":" + ingressIP).getBytes(UTF_8), 2, false);
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
