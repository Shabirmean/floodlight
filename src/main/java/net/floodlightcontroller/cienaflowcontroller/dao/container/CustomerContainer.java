package net.floodlightcontroller.cienaflowcontroller.dao.container;


/**
 * Created by shabirmean on 2017-11-22 with some hope.
 */
@SuppressWarnings("WeakerAccess")
public class CustomerContainer {
    private static final String CONTNR_STRING_FORMAT =
            "{" +
                    "\"index\":\"%s\"," +
                    "\"id\":\"%s\"," +
                    "\"name\":\"%s\"," +
                    "\"ip\":\"%s\"," +
                    "\"mac\":\"%s\"," +
                    "\"isIngress\":\"%s\"," +
                    "\"allowedFlows\":\"%s\"" +
                    "}";
    private String customer;
    private String cId;
    private String index;
    private String name;
    private String ipAddress;
    private String macAddress;
    private String allowedFlows;
    private String eventId;
    private boolean isReady = false;
    private boolean borderContainer = false;

    public CustomerContainer(
            String customer, String cId, String index, String name, String ip, String mac) {
        this.customer = customer;
        this.cId = cId;
        this.index = index;
        this.name = name;
        this.ipAddress = ip;
        this.macAddress = mac;
    }


    @SuppressWarnings("unused")
    public String getJSONString() {
        return String.format(
                CONTNR_STRING_FORMAT, index, cId, name, ipAddress, macAddress, borderContainer, allowedFlows);
    }

    public String getCustomer() {
        return customer;
    }

    public String getcId() {
        return cId;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @SuppressWarnings("unused")
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public String getAllowedFlows() {
        return allowedFlows;
    }

    public void setAllowedFlows(String allowedFlows) {
        this.allowedFlows = allowedFlows;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public boolean isBorderContainer() {
        return borderContainer;
    }

    public void setBorderContainer(boolean borderContainer) {
        this.borderContainer = borderContainer;
    }

    public String getIndex() {
        return index;
    }
}
