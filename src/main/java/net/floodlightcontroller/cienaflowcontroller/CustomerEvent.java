package net.floodlightcontroller.cienaflowcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.MQTT_PUBLISH_READY;
import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.RESPONSE_MSG_FORMAT_READY;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
public class CustomerEvent {
    private static Logger logger = LoggerFactory.getLogger(CustomerEvent.class);
    private static final String CONTAINER_HASH_KEY = "%s:%s";

    private String eventId;
    private String customer;
//    private String subnet;

//    private STATE eventState;                     // To check if the flow is running or has completed running
//    private boolean setupStatus = true;          // This indicates whether the setup was clean for the flow to happen
    private ConcurrentHashMap<String, CustomerContainer> ipToContainerMap;
    private ConcurrentHashMap<String, CustomerContainer> cnameToContainerMap;
    private ConcurrentHashMap<String, CustomerContainer> idxToContainerMap;
    //containerName is same as customer_hostname
    private final HashMap<String, Boolean> readyContainerMap;

    CustomerEvent(String eventId, String customer, String subnet) {
        this.eventId = eventId;
        this.customer = customer.toUpperCase();
//        this.subnet = subnet;
//        this.eventState = STATE.PREPARING;
        this.ipToContainerMap = new ConcurrentHashMap<>();
        this.cnameToContainerMap = new ConcurrentHashMap<>();
        this.idxToContainerMap = new ConcurrentHashMap<>();
        this.readyContainerMap = new HashMap<>();
    }

    void addCustomerContainers(ArrayList<CustomerContainer> customerContainers) {
        for (CustomerContainer cusContainer : customerContainers) {
            addCustomerContainer(cusContainer);
            String hashKey = String.format(CONTAINER_HASH_KEY, cusContainer.getIpAddress(), cusContainer.getName());
            if (cusContainer.isBorderContainer()) {
                readyContainerMap.put(hashKey, true);
            } else {
                readyContainerMap.put(hashKey, false);
            }
        }
    }

    private void addCustomerContainer(CustomerContainer newContainer) {
        ipToContainerMap.put(newContainer.getIpAddress(), newContainer);
        cnameToContainerMap.put(newContainer.getName(), newContainer);
        idxToContainerMap.put(newContainer.getIndex(), newContainer);
    }

    void updateReadyState(ArrayList<ReadyStateHolder> readyContainerList) {
        for (ReadyStateHolder readyContainer : readyContainerList) {
            if (customer.equals(readyContainer.getCustomer().toUpperCase())) {
                updateReadyState(readyContainer.getIpAddress(), readyContainer.getHostname());
            }
        }
    }

    void updateReadyState(String ipAddress, String containerName) {
        synchronized (readyContainerMap) {
            if (ipToContainerMap.containsKey(ipAddress) && cnameToContainerMap.containsKey(containerName)) {
                String hashKey = String.format(CONTAINER_HASH_KEY, ipAddress, containerName);
                readyContainerMap.put(hashKey, true);
                Collection<Boolean> containerStates = readyContainerMap.values();
                if (areAllReady(containerStates)) {
                    logger.info("+++++++++++ ALL ARE READY +++++++++++++");
//                    String responseString = String.format(RESPONSE_MSG_FORMAT_READY, eventId, setupStatus);
                    String responseString = String.format(RESPONSE_MSG_FORMAT_READY, eventId, "true", "");
                    FlowController.respondToContainerManager(MQTT_PUBLISH_READY, responseString);
                }
            }
        }
        //TODO:: If does not contain then some issue has occurred
    }

    String getIpFromIndex(String index) {
        return idxToContainerMap.get(index).getIpAddress();
    }

    Enumeration<String> getContainerIpsOfEvent() {
        return ipToContainerMap.keys();
    }

    private boolean areAllReady(Collection<Boolean> booleanCollection) {
        for (boolean b : booleanCollection) if (!b) return false;
        return true;
    }

//    void watchAndRespondToContainerManager() {
//        Thread eventStatusWatcher = new Thread(() -> {
//            while (true) {
//                if (eventState == STATE.ACTIVE) {
//                    respondToContainerManager(String.format(RESPONSE_MSG_FORMAT_READY, eventId, setupStatus));
//                    return;
//                }
//            }
//        });
//        eventStatusWatcher.start();
//    }

//    private void respondToContainerManager(String responseToCM) {
//        try {
//            MqttConnectOptions options = new MqttConnectOptions();
//            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
//            String clientId = MqttClient.generateClientId();
//            MqttClient mqttPublisherClient = new MqttClient(MQTT_BROKER_URI, clientId, new MemoryPersistence());
//            mqttPublisherClient.connect(options);
//            mqttPublisherClient.publish(MQTT_PUBLISH_READY, responseToCM.getBytes(UTF_8), 2, false);
//            mqttPublisherClient.disconnect();
//        } catch (MqttException | UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//    }

    public String getCustomer() {
        return customer;
    }

//    public STATE getEventState() {
//        return eventState;
//    }
//
//    public void setEventState(STATE eventState) {
//        this.eventState = eventState;
//    }
//
//    private enum STATE {
//        ACTIVE(2),
//        PREPARING(1),
//        INACTIVE(0);
//
//        private int eventStatus;
//
//        STATE(int eventStatus) {
//            this.eventStatus = eventStatus;
//        }
//
//        public int getEventStatus() {
//            return eventStatus;
//        }
//    }

}
