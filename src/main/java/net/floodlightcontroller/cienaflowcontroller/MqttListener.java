package net.floodlightcontroller.cienaflowcontroller;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
@SuppressWarnings("ALL")
public class MqttListener implements MqttCallback {
    protected static Logger logger;
    private MqttClient mqttClient;



    MqttListener() {
    }

    public void init() {
        logger = LoggerFactory.getLogger(MqttListener.class);
        Runnable subscriber = () -> {
            try {
                String clientId = MqttClient.generateClientId();
                mqttClient = new MqttClient(FlowControllerConstants.MQTT_BROKER_URI, clientId, new MemoryPersistence());
                mqttClient.setCallback();
            } catch (MqttException e) {
                e.printStackTrace();
            }

            while (!mqttClient.isConnected()) {
                try {
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
                    mqttClient.connect(options);

                    if (mqttClient.isConnected()) {
                        logger.info("############# STARTED MQTT Listener...");
                        logger.info("############# Subscribing to : " + FlowControllerConstants.MQTT_SUBSCRIBE_TOPIC);
                        mqttClient.subscribe(FlowControllerConstants.MQTT_SUBSCRIBE_TOPIC, 0);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    logger.error("MQTT Connect-Thread Sleep Interrupt Exception.");
                    ex.printStackTrace();
                }
            }
        };

        Thread subscriberThread = new Thread(subscriber);
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    @Override
    public void connectionLost(Throwable throwable) {
        logger.error("############# Connection to Mqtt broker lost.");
        throwable.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String messageIncoming = mqttMessage.toString();
        String eventIdentifier = topic.substring(topic.lastIndexOf(File.separator) + 1, topic.length());
        logger.info("Mqtt-Msg [" + topic + "] : [ " + messageIncoming + " ]");
        new Thread(new Runnable() {
            @Override
            public void run() {
                processMessage(eventIdentifier, messageIncoming);
            }
        }).start();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public void processMessage(String eventIdentifier, String message) {
        boolean status = true;
        String eventId = eventIdentifier;
        String subnet;
        HashMap<String, CustomerContainer> containerList;
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(message);
            JSONObject jsonObject = (JSONObject) obj;
            logger.info("Received JSON Object from container-manager:: \n" + jsonObject);

            String customer = ((String) jsonObject.get(FlowControllerConstants.JSON_ATTRIB_CUSTOMER)).toUpperCase();
            containerList = FlowController.containerMap.get(customer);
            if (containerList != null) {
                logger.error("Container map for customer-" + customer + " already exists. Discarding this call");
                status = false;
            }
            containerList = new HashMap<>();
            subnet = (String) jsonObject.get(FlowControllerConstants.JSON_ATTRIB_SUBNET);
            eventId = (String) jsonObject.get(FlowControllerConstants.JSON_ATTRIB_EVENTID);
            int count = Integer.parseInt((String) jsonObject.get(FlowControllerConstants.JSON_ATTRIB_COUNT));
            JSONArray containers = (JSONArray) jsonObject.get(FlowControllerConstants.JSON_ATTRIB_CONTAINERS);
//            String pipeline = (String) jsonObject.get(FlowControllerConstants.JSON_ATTRIB_PIPELINE);

            if (containers.size() != count) {
                logger.warn("Container count and meta-info count does not match for newly received information.");
                status = false;
            } else {
//                String[] pipelineArr = pipeline.split(",");
//                int index = 0;
                for (Object container : containers) {
                    JSONObject containerObj = (JSONObject) container;
                    String cId = (String) containerObj.get(FlowControllerConstants.JSON_ATTRIB_ID);
                    String key = (String) containerObj.get(FlowControllerConstants.JSON_ATTRIB_KEY);
                    String cName = (String) containerObj.get(FlowControllerConstants.JSON_ATTRIB_NAME);
                    String ip = (String) containerObj.get(FlowControllerConstants.JSON_ATTRIB_IP);
                    String mac = (String) containerObj.get(FlowControllerConstants.JSON_ATTRIB_MAC);
                    String isIngress = (String) containerObj.get(FlowControllerConstants.JSON_ATTRIB_IS_INGRESS);
//                    int pipelineIndex = Integer.parseInt(pipelineArr[index].trim());
                    CustomerContainer cusContainer = new CustomerContainer(customer, cId, key, cName, ip, mac);
                    cusContainer.setBorderContainer(Boolean.parseBoolean(isIngress));
//                    if (pipelineIndex == 1) {
//                        cusContainer.setBorderContainer(true);
////                        cusContainer.setEntryContainer(true);
//                        FlowController.entryContainerIPSet.add(ip);
//                    } else if (pipelineIndex == pipelineArr.length) {
//                        cusContainer.setBorderContainer(true);
//                    }
                    containerList.put(ip, cusContainer);
                    synchronized (FlowController.LOCK) {
                        FlowController.ipToEventIdMap.put(ip, eventId);
                    }
//                    index++;
                }
                FlowController.containerMap.put(customer, containerList);
                FlowController.subnetToCustomerMap.put(subnet, customer);

                ArrayList<String> customerEvents = FlowController.customerEventIdMap.get(customer);
                if (customerEvents == null) {
                    customerEvents = new ArrayList<>();
                }
                customerEvents.add(eventId);
                FlowController.customerEventIdMap.put(customer, customerEvents);
            }
        } catch (ParseException e) {
            //TODO:: Handle exceptions correctly
            status = false;
            e.printStackTrace();
        }

        String responseToCM = String.format(FlowControllerConstants.RESPONSE_MSG_FORMAT, eventId, status);
        synchronized (FlowController.LOCK) {
            if (FlowController.readyStateEvents.contains(eventId)) {
                respondToContainerManager(responseToCM);
            } else {
                FlowController.readyStateEvents.put(eventId, responseToCM);
            }
        }
    }

    void respondToContainerManager(String responseToCM) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            String clientId = MqttClient.generateClientId();
            MqttClient mqttPublisherClient =
                    new MqttClient(FlowControllerConstants.MQTT_BROKER_URI, clientId, new MemoryPersistence());
            mqttPublisherClient.connect(options);
            String topic = FlowControllerConstants.MQTT_PUBLISH_TOPIC;
            mqttPublisherClient.publish(topic, responseToCM.getBytes(UTF_8), 2, false);
            mqttPublisherClient.disconnect();
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
