package net.floodlightcontroller.cienaflowcontroller.datahandler;

import net.floodlightcontroller.cienaflowcontroller.dao.container.CustomerContainer;
import net.floodlightcontroller.cienaflowcontroller.controller.FlowControlRemover;
import net.floodlightcontroller.cienaflowcontroller.controller.FlowController;
import net.floodlightcontroller.cienaflowcontroller.dao.ReadyStateHolder;
import net.floodlightcontroller.cienaflowcontroller.dao.container.IngressContainer;
import net.floodlightcontroller.cienaflowcontroller.utils.FlowControllerConstants;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static net.floodlightcontroller.cienaflowcontroller.utils.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
public class FlowRepository implements MqttCallback {
    protected static Logger logger;
    private static final int MAX_TABLE_IDS = 256;
    private static final int EVENT_ID_INDEX = 0;
    private static final int CUSTOMER_INDEX = 1;
    private static final int EGRESS_PORT_INDEX = 5;
    private static final String DELETE_FLOW_MSG = "DELETE_FLOWS";

    private final ConcurrentHashMap<String, CustomerEvent> eventIdToEventsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CustomerEvent> customerToEventsMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ArrayList<ReadyStateHolder>> evntsToReadyConMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CustomerContainer> ipsToCustomerConMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, FlowControlRemover> flowControlsRemoverMap = new ConcurrentHashMap<>();
    private final ArrayList<String> ingressContainerIps = new ArrayList<>();

    private final ConcurrentHashMap<String, Integer> ipToTableIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OFPort> ipToOVSPortNumberMap = new ConcurrentHashMap<>();
    private final Set<String> acknowledgedEvents = new HashSet<>();
    private BitSet flowTableBits;

    public FlowRepository() {
        logger = LoggerFactory.getLogger(FlowRepository.class);
        this.flowTableBits = new BitSet(MAX_TABLE_IDS);
        this.flowTableBits.set(DEFAULT_FLOW_TABLE);
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
        if (topic.contains(FlowControllerConstants.REQUEST)) {
            new Thread(() -> processMessage(eventIdentifier, messageIncoming)).start();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    private void processMessage(String eventIdentifier, String message) {
        CustomerEvent newEvent;
        String responseString;
        try {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(message);
            JSONObject jsonObject = (JSONObject) obj;
            logger.info("Received JSON Object from container-manager:: \n" + jsonObject);

            String customer = ((String) jsonObject.get(JSON_ATTRIB_CUSTOMER)).toUpperCase();
            if (customerToEventsMap.get(customer) != null) {
                //TODO:: Indicates an existing flow for the customer
                logger.error("Container map for customer-" + customer + " already exists. Discarding this call");
                responseString = String.format(
                        RESPONSE_MSG_FORMAT_READY, eventIdentifier, "false", "FLOW-EXISTS-FOR-CUSTOMER-ERROR");
            }

            ArrayList<CustomerContainer> containerList = new ArrayList<>();
            String subnet = (String) jsonObject.get(JSON_ATTRIB_SUBNET);
            String eventId = (String) jsonObject.get(JSON_ATTRIB_EVENTID);

            if (!eventId.equals(eventIdentifier)) {
                //TODO:: Mismatch needs to handle the case
                logger.error("The event-id [" + eventId + "] on the message does not match the one on the MQTT-topic.");
                responseString = String.format(
                        RESPONSE_MSG_FORMAT_READY, eventIdentifier, "false", "EVENT-IDS-MISMATCH-ERROR");
            }
            int count = Integer.parseInt((String) jsonObject.get(JSON_ATTRIB_COUNT));
            JSONArray containers = (JSONArray) jsonObject.get(JSON_ATTRIB_CONTAINERS);

            if (containers.size() != count) {
                //TODO:: Separate out all error messages
                logger.warn("Container count and meta-info count does not match for newly received information.");
                responseString = String.format(
                        RESPONSE_MSG_FORMAT_READY, eventIdentifier, "false", "CONTAINER-COUNT-ERROR");
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

                    CustomerContainer cusContainer = new CustomerContainer(customer, cId, index, cName, ip, mac);
                    cusContainer.setBorderContainer(isIngressBool);
                    cusContainer.setAllowedFlows(allowedFlows);
                    cusContainer.setEventId(eventId);

                    if (isIngressBool) {
                        if (!ingressContainerIps.contains(ip)) {
                            ingressContainerIps.add(ip);
                        }

                        IngressContainer ingressCusCon = (IngressContainer) ipsToCustomerConMap.get(ip);
                        if (ingressCusCon == null) {
                            ingressCusCon = new IngressContainer(cusContainer);
//                            ipsToCustomerConMap.put(ip, ingressCusCon);
                        }

                        ingressCusCon.addNewCustomerEvent(customer, eventId);
                        containerList.add(ingressCusCon);
                        ipsToCustomerConMap.put(ip, ingressCusCon);
                    } else {
                        containerList.add(cusContainer);
                        ipsToCustomerConMap.put(ip, cusContainer);
                    }
                }

                synchronized (eventIdToEventsMap) {
                    newEvent = new CustomerEvent(eventId, customer);
                    newEvent.addCustomerContainers(containerList);
                    ArrayList<ReadyStateHolder> readyContainerList = evntsToReadyConMap.get(eventId);
                    if (readyContainerList != null && !readyContainerList.isEmpty()) {
                        newEvent.updateReadyState(readyContainerList);
                    }
                    eventIdToEventsMap.put(eventId, newEvent);
                    customerToEventsMap.put(customer, newEvent);
                }
                responseString = String.format(RESPONSE_MSG_FORMAT_READY, eventIdentifier, "true", "SUCCESS");
            }

        } catch (ParseException e) {
            //TODO:: Handle exceptions correctly
            e.printStackTrace();
            responseString = String.format(RESPONSE_MSG_FORMAT_READY, eventIdentifier, "false", e.getMessage());
        }
        FlowController.respondToContainerManager(MQTT_PUBLISH_EXEC, responseString);
    }

    public void addReadyStateContainer(ReadyStateHolder readyContainer) {
        synchronized (eventIdToEventsMap) {
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
        }
    }

    private synchronized HashMap<String, Integer> cleanUpEventStructures(String eventId, String customer) {
        CustomerEvent event = eventIdToEventsMap.remove(eventId);
        customerToEventsMap.remove(customer);
        evntsToReadyConMap.remove(eventId);

        Enumeration<String> containerIpsOfEvent = event.getContainerIpsOfEvent();
        HashMap<String, Integer> removedIPsToTableIdMap = new HashMap<>();
        while (containerIpsOfEvent.hasMoreElements()) {
            String ip = containerIpsOfEvent.nextElement();
            if (!ingressContainerIps.contains(ip)) {
                //TODO:: Must Uncomment for correct usage
                ipsToCustomerConMap.remove(ip);
                int tableId = clearFlowTableBit(ip);
                ipToTableIdMap.remove(ip);
                removedIPsToTableIdMap.put(ip, tableId);
            } else {
                IngressContainer ingressCon = (IngressContainer) ipsToCustomerConMap.get(ip);
                ingressCon.getCustomerToEventMap().remove(customer);
            }
        }
        return removedIPsToTableIdMap;
    }

    public synchronized boolean isIngressContainerIp(String ipAddress) {
        return ingressContainerIps.contains(ipAddress);
    }

    public synchronized boolean isNeighbourOfIngress(String ipAddress) {
        List<String> listOfIngressNeighbours = getNeighboursOfIngress();
        return listOfIngressNeighbours.contains(ipAddress);
    }

    private synchronized List<String> getNeighboursOfIngress() {
        List<String> listOfAllNeighbours = new ArrayList<>();
        for (String ingressIp : ingressContainerIps) {
            IngressContainer ingressCon = (IngressContainer) ipsToCustomerConMap.get(ingressIp);
            Collection<String> eventsOfIngress = ingressCon.getCustomerToEventMap().values();

            for (String eventId : eventsOfIngress) {
                CustomerEvent cusEvent = eventIdToEventsMap.get(eventId);
                List<String> neighbourConIdxs = getNeighbourContainerIdx(ingressIp);
                for (String idx : neighbourConIdxs) {
                    String ipAdd = cusEvent.getIpFromIndex(idx);
                    listOfAllNeighbours.add(ipAdd);
                }
            }
        }
        return listOfAllNeighbours;
    }

    public synchronized List<IPv4Address> getNeighbourIps(IPv4Address ipAddress) {
        List<IPv4Address> adjacentIpAddresses = new ArrayList<>();
        CustomerContainer customerContainer = ipsToCustomerConMap.get(ipAddress.toString());

        if (customerContainer != null) {
            String eventId = customerContainer.getEventId();
            CustomerEvent customerEvent = eventIdToEventsMap.get(eventId);
            List<String> neighbourIndexes = getNeighbourContainerIdx(ipAddress.toString());
            for (String indx : neighbourIndexes) {
                String ipAdd = customerEvent.getIpFromIndex(indx);
                adjacentIpAddresses.add(IPv4Address.of(ipAdd));
            }
        }
        return adjacentIpAddresses;
    }

    private List<String> getNeighbourContainerIdx(String ipAddress) {
        CustomerContainer customerContainer = ipsToCustomerConMap.get(ipAddress);
        String allowedFlows = customerContainer.getAllowedFlows();
        return Arrays.asList(allowedFlows.split("\\s*,\\s*"));
    }

    public synchronized int getFlowTableId(IPv4Address srcIp) {
        Integer tableId = ipToTableIdMap.get(srcIp.toString());
        if (tableId == null) {
            tableId = createNewTableEntryForIP(srcIp.toString());
        }
        return tableId;
    }

    private int createNewTableEntryForIP(String ipAddress) {
        //TODO:: Need to remove the table Ids later
        int nextTableId = flowTableBits.nextClearBit(DEFAULT_FLOW_TABLE);
        logger.info("New Table Id " + nextTableId + " for IP " + ipAddress);
        flowTableBits.set(nextTableId);
        ipToTableIdMap.put(ipAddress, nextTableId);
        return nextTableId;
    }

    private synchronized int clearFlowTableBit(String ipAddress) {
        int tableId = ipToTableIdMap.remove(ipAddress);
        logger.info("Removing Table Id " + tableId + " for IP " + ipAddress);
        flowTableBits.clear(tableId);
        return tableId;
    }

    public void addInPortForIp(String ipAddress, OFPort ovsPort) {
        if (!ipToOVSPortNumberMap.containsKey(ipAddress)) {
            ipToOVSPortNumberMap.put(ipAddress, ovsPort);
        }
    }

    public void processEventStatusUDP(Ethernet eth, IOFSwitch ovsSwitch) {
        logger.info("[From an ingress container] Processing received UDP Packet.");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        String srcAddress = ipv4.getSourceAddress().toString();

        UDP udp = (UDP) ipv4.getPayload();
        Data udpData = (Data) udp.getPayload();
        byte[] udpDataBytes = udpData.getData();
        String udpDataString = new String(udpDataBytes);
        String[] stringElements = udpDataString.split(COLON);
        String eventId = stringElements[EVENT_ID_INDEX];

        logger.info("#### Received UDP Msg [" + udpDataString + "] from SRC-IP [" + srcAddress + "]");
//        <EVENT_ID>:<CUSTOMER>:<SOME_MSG>
        if (udpDataString.contains(DELETE_FLOW_MSG)) {
            FlowControlRemover flRemover = flowControlsRemoverMap.get(eventId);
            HashMap<String, Integer> eventIPsAndTableIds = cleanUpEventStructures(eventId, flRemover.getCustomer());
            flRemover.clearOVSFlows(ovsSwitch, eventIPsAndTableIds, ipToOVSPortNumberMap);
            //TODO:: Added for runTime measurement metrics. must be removed later
            String cleanUpTime = Long.toString(System.nanoTime());
            String cleanUpCompleteMsg = "{" +
                                                "\"eventId\":\"" + eventId + "\"," +
                                                "\"time\":\"" + cleanUpTime + "\"" +
                                        "}";
            FlowController.respondToContainerManager("ciena/cmanager/fm_cm/alldone", cleanUpCompleteMsg);

        } else if (stringElements.length > 2) {
            // <RECEVD_EVENT_ID>:<RECVD_CUSTOMER>:<CORRECTION>:<MSG>:<FINISHED_TIME>
            if (!acknowledgedEvents.contains(eventId)) {
                acknowledgedEvents.add(eventId);
                String customer = stringElements[CUSTOMER_INDEX];
                FlowControlRemover flRemover = new FlowControlRemover(customer);
                flowControlsRemoverMap.put(eventId, flRemover);

                String updateMsg = udpDataString.substring(0, udpDataString.lastIndexOf(":"));
                //TODO:: Added for runTime measurement metrics. must be removed later
                String eventTime = stringElements[stringElements.length - 2];
                String responseString = String.format(RESPONSE_MSG_FORMAT_TERMINATE, eventId, eventTime, updateMsg);
                FlowController.respondToContainerManager(MQTT_PUBLISH_TERMINATE, responseString);
            }
            int egressPort = Integer.parseInt(stringElements[EGRESS_PORT_INDEX]);
            acknowledgeIngress(srcAddress, egressPort, eventId);
        }
    }

    private void acknowledgeIngress(String ingressIp, int ingressPort, String eventId) {
        try {
            logger.info("Sending ACK to event [" + eventId + "] on Socket [ " + ingressIp + ":" + ingressPort + "]");
            InetAddress address = InetAddress.getByName(ingressIp);
            byte[] buf = ("ACK:" + eventId).getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, ingressPort);
            DatagramSocket dgramSocket = new DatagramSocket();
            dgramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
