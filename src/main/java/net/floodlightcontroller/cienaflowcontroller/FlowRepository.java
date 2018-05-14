package net.floodlightcontroller.cienaflowcontroller;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
public class FlowRepository implements MqttCallback {
    protected static Logger logger;

    private final ConcurrentHashMap<String, CustomerEvent> eventIdToEventsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CustomerEvent> customerToEventsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CustomerEvent> subnetToEventsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ArrayList<ReadyStateHolder>> evntsToReadyConMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CustomerContainer> ipsToCustomerConMap = new ConcurrentHashMap<>();
    private final ArrayList<String> ingressContainerIps = new ArrayList<>();

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
                    String index = (String) containerObj.get(JSON_ATTRIB_INDEX);
                    String cName = (String) containerObj.get(JSON_ATTRIB_NAME);
                    String ip = (String) containerObj.get(JSON_ATTRIB_IP);
                    String mac = (String) containerObj.get(JSON_ATTRIB_MAC);
                    String isIngress = (String) containerObj.get(JSON_ATTRIB_IS_INGRESS);
                    String allowedFlows = (String) containerObj.get(JSON_ATTRIB_ALLOWED_FLOWS);
                    Boolean isIngressBool = Boolean.parseBoolean(isIngress);
                    CustomerContainer cusContainer =
                            new CustomerContainer(customer, cId, index, cName, ip, mac);
                    cusContainer.setBorderContainer(isIngressBool);
                    cusContainer.setAllowedFlows(allowedFlows);
                    cusContainer.setEventId(eventId);
                    containerList.add(cusContainer);
                    if (isIngressBool) {
                        ingressContainerIps.add(ip);
                    }
                    ipsToCustomerConMap.put(ip, cusContainer);
                }

                synchronized (eventIdToEventsMap) {
                    newEvent = new CustomerEvent(eventId, customer, subnet);
                    newEvent.addCustomerContainers(containerList);
                    ArrayList<ReadyStateHolder> readyContainerList = evntsToReadyConMap.get(eventId);
                    if (readyContainerList != null && !readyContainerList.isEmpty()) {
                        newEvent.updateReadyState(readyContainerList);
                    }
//                    newEvent.watchAndRespondToContainerManager();
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

    boolean addReadyStateContainer(ReadyStateHolder readyContainer) {
        synchronized (eventIdToEventsMap) {
            if (isFirstReadyMsg(readyContainer.getEventId(), readyContainer.getIpAddress())) {
                String eventId = readyContainer.getEventId();
                CustomerEvent anEvent = eventIdToEventsMap.get(eventId);
                logger.info("ReadyContainer > " + readyContainer.getName() + " - " + readyContainer.getIpAddress());
                if (anEvent != null) {
                    if (anEvent.getCustomer().equals(readyContainer.getCustomer().toUpperCase())) {
                        anEvent.updateReadyState(readyContainer.getIpAddress(), readyContainer.getName());
                    }
                } else {
                    ArrayList<ReadyStateHolder> readyContainerList = evntsToReadyConMap.get(eventId);
                    if (readyContainerList == null) {
                        readyContainerList = new ArrayList<>();
                    }
                    readyContainerList.add(readyContainer);
                    evntsToReadyConMap.put(eventId, readyContainerList);
                }
                return true;
            }
        }
        return false;
    }

    boolean isIngressContainerIp(String ipAddress) {
        return ingressContainerIps.contains(ipAddress);
    }

    boolean isNeighbourOfIngress(String ipAddress) {
        List<String> listOfIngressNeighbours = getNeighboursOfIngress();
        return listOfIngressNeighbours.contains(ipAddress);
    }

    private List<String> getNeighboursOfIngress() {
        List<String> listOfAllNeighbours = new ArrayList<>();
        for (String ingressIp : ingressContainerIps) {
            listOfAllNeighbours.addAll(getNeighbourIps(ingressIp));
        }
        return listOfAllNeighbours;
    }

    List<IPv4Address> getNeighbourIps(IPv4Address ipAddress) {
        List<IPv4Address> adjacentIpAddresses = new ArrayList<>();
        CustomerContainer customerContainer = ipsToCustomerConMap.get(ipAddress.toString());
        String eventId = customerContainer.getEventId();
        CustomerEvent customerEvent = eventIdToEventsMap.get(eventId);

        List<String> neighbourIndexes = getNeighbourIps(ipAddress.toString());
        for (String indx : neighbourIndexes) {
            String ipAdd = customerEvent.getIpFromIndex(indx);
            adjacentIpAddresses.add(IPv4Address.of(ipAdd));
        }
        return adjacentIpAddresses;
    }

    private List<String> getNeighbourIps(String ipAddress) {
        CustomerContainer customerContainer = ipsToCustomerConMap.get(ipAddress);
        String allowedFlows = customerContainer.getAllowedFlows();
        return Arrays.asList(allowedFlows.split("\\s*,\\s*"));
    }

    private boolean isFirstReadyMsg(String eventId, String ipAddress) {
        ArrayList<ReadyStateHolder> readyStatesContainers = evntsToReadyConMap.get(eventId);
        if (readyStatesContainers != null) {
            for (ReadyStateHolder readyContainer : readyStatesContainers) {
                if (readyContainer.getIpAddress().equals(ipAddress)) {
                    return false;
                }
            }
        }
        return true;
    }

}
