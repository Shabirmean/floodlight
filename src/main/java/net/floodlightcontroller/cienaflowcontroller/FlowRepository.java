package net.floodlightcontroller.cienaflowcontroller;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
public class FlowRepository implements MqttCallback {
    protected static Logger logger;
    private final ConcurrentHashMap<String, CustomerEvent> eventIdToEventsMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CustomerEvent> customerToEventsMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CustomerEvent> subnetToEventsMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ArrayList<ReadyStateHolder>> eventsToReadyConMap = new ConcurrentHashMap<>();

    FlowRepository() {
        logger = LoggerFactory.getLogger(FlowRepository.class);
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
        new Thread(() -> {
            processMessage(eventIdentifier, messageIncoming);
        }).start();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    private void processMessage(String eventIdentifier, String message) {
        CustomerEvent newEvent;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(message);
            JSONObject jsonObject = (JSONObject) obj;
            logger.info("Received JSON Object from container-manager:: \n" + jsonObject);

            String customer = ((String) jsonObject.get(JSON_ATTRIB_CUSTOMER)).toUpperCase();
            if (customerToEventsMap.get(customer) != null) {
                //TODO:: Indicates an existing flow for the customer
                logger.error("Container map for customer-" + customer + " already exists. Discarding this call");
            }

            ArrayList<CustomerContainer> containerList = new ArrayList<>();
            String subnet = (String) jsonObject.get(JSON_ATTRIB_SUBNET);
            String eventId = (String) jsonObject.get(JSON_ATTRIB_EVENTID);

            if (!eventId.equals(eventIdentifier)) {
                //TODO:: Mismatch needs to handle the case
                logger.error("The event-id [" + eventId + "] on the message does not match the if on the MQTT-topic.");
            }
            int count = Integer.parseInt((String) jsonObject.get(JSON_ATTRIB_COUNT));
            JSONArray containers = (JSONArray) jsonObject.get(JSON_ATTRIB_CONTAINERS);

            if (containers.size() != count) {
                logger.warn("Container count and meta-info count does not match for newly received information.");
                //TODO:: Indicates an existing flow for the customer

            } else {
                for (Object container : containers) {
                    JSONObject containerObj = (JSONObject) container;
                    String cId = (String) containerObj.get(JSON_ATTRIB_ID);
                    String key = (String) containerObj.get(JSON_ATTRIB_KEY);
                    String cName = (String) containerObj.get(JSON_ATTRIB_NAME);
                    String ip = (String) containerObj.get(JSON_ATTRIB_IP);
                    String mac = (String) containerObj.get(JSON_ATTRIB_MAC);
                    String isIngress = (String) containerObj.get(JSON_ATTRIB_IS_INGRESS);
                    CustomerContainer cusContainer = new CustomerContainer(customer, cId, key, cName, ip, mac);
                    cusContainer.setBorderContainer(Boolean.parseBoolean(isIngress));
                    containerList.add(cusContainer);
                }

                synchronized (eventIdToEventsMap) {
                    newEvent = new CustomerEvent(eventId, customer, subnet);
                    newEvent.addCustomerContainers(containerList);
                    ArrayList<ReadyStateHolder> readyContainerList = eventsToReadyConMap.get(eventId);
                    if (readyContainerList != null && !readyContainerList.isEmpty()) {
                        newEvent.updateReadyState(readyContainerList);
                    }
                    newEvent.watchAndRespondToContainerManager();
                    eventIdToEventsMap.put(eventId, newEvent);
                    customerToEventsMap.put(customer, newEvent);
                    subnetToEventsMap.put(subnet, newEvent);
                }
            }
        } catch (ParseException e) {
            //TODO:: Handle exceptions correctly
            e.printStackTrace();
        }
    }

    void addReadyStateContainer(ReadyStateHolder readyContainer) {
        synchronized (eventIdToEventsMap) {
            String eventId = readyContainer.getEventId();
            CustomerEvent anEvent = eventIdToEventsMap.get(eventId);
            if (anEvent != null) {
                logger.info("@@@@@@@@@@@@@ > " + readyContainer.getHostname() + " - " + readyContainer.getIpAddress());
                if (anEvent.getCustomer().equals(readyContainer.getCustomer().toUpperCase())) {
                    anEvent.updateReadyState(readyContainer.getIpAddress(), readyContainer.getHostname());
                }
            } else {
                logger.info("$$$$$$$$$$$$$ > " + readyContainer.getHostname() + " - " + readyContainer.getIpAddress());
                ArrayList<ReadyStateHolder> readyContainerList = eventsToReadyConMap.get(eventId);
                if (readyContainerList == null) {
                    readyContainerList = new ArrayList<>();
                }
                readyContainerList.add(readyContainer);
                eventsToReadyConMap.put(eventId, readyContainerList);
            }
        }
    }
}
