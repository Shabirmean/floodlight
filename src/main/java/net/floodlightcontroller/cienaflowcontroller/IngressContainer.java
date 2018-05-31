package net.floodlightcontroller.cienaflowcontroller;

import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.INGRESS_CUSTOMER_CONTAINER;

/**
 * Created by shabirmean on 2018-05-30 with some hope.
 */
@SuppressWarnings("WeakerAccess, unused")
public class IngressContainer extends CustomerContainer {

    private static final String CONTNR_STRING_FORMAT =
            "{" +
                    "\"customers\":\"%s\"," +
                    "\"index\":\"%s\"," +
                    "\"id\":\"%s\"," +
                    "\"name\":\"%s\"," +
                    "\"ip\":\"%s\"," +
                    "\"mac\":\"%s\"," +
                    "\"isIngress\":\"%s\"," +
                    "\"allowedFlows\":\"%s\"" +
                    "}";

    private ConcurrentHashMap<String, String> customerToEventMap;

    IngressContainer(CustomerContainer cusCon) {
        super(INGRESS_CUSTOMER_CONTAINER,
                cusCon.getcId(), cusCon.getIndex(), cusCon.getName(), cusCon.getIpAddress(), cusCon.getMacAddress());
        super.setBorderContainer(cusCon.isBorderContainer());
        super.setAllowedFlows(cusCon.getAllowedFlows());
        super.setEventId(INGRESS_CUSTOMER_CONTAINER);
        this.customerToEventMap = new ConcurrentHashMap<>();
        customerToEventMap.put(cusCon.getCustomer(), cusCon.getEventId());
    }

    public String getJSONString() {
        return String.format(CONTNR_STRING_FORMAT,
                customerToEventMap.keys(), getIndex(), getcId(), getName(), getIpAddress(),
                getMacAddress(), isBorderContainer(), getAllowedFlows());
    }

    public ConcurrentHashMap<String, String> getCustomerToEventMap() {
        return customerToEventMap;
    }

    public void addNewCustomerEvent(String customer, String eventId) {
        customerToEventMap.put(customer, eventId);
    }
}
