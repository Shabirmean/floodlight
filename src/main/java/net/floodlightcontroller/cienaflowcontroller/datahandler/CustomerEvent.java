package net.floodlightcontroller.cienaflowcontroller.datahandler;


import net.floodlightcontroller.cienaflowcontroller.controller.FlowController;
import net.floodlightcontroller.cienaflowcontroller.dao.ReadyStateHolder;
import net.floodlightcontroller.cienaflowcontroller.dao.container.CustomerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.utils.FlowControllerConstants.MQTT_PUBLISH_READY;
import static net.floodlightcontroller.cienaflowcontroller.utils.FlowControllerConstants.RESPONSE_MSG_FORMAT_READY;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
public class CustomerEvent {
    private static Logger logger = LoggerFactory.getLogger(CustomerEvent.class);
    private static final String CONTAINER_HASH_KEY = "%s:%s";

    private String eventId;
    private String customer;

    private ConcurrentHashMap<String, CustomerContainer> ipToContainerMap;
    private ConcurrentHashMap<String, CustomerContainer> cnameToContainerMap;
    private ConcurrentHashMap<String, CustomerContainer> idxToContainerMap;
    //containerName is same as customer_hostname
    private final HashMap<String, Boolean> readyContainerMap;

    CustomerEvent(String eventId, String customer) {
        this.eventId = eventId;
        this.customer = customer.toUpperCase();
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

    public String getCustomer() {
        return customer;
    }

}
