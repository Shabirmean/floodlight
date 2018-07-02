package net.floodlightcontroller.cienaflowcontroller.dao;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
@SuppressWarnings("WeakerAccess")
public class ReadyStateHolder {
    private String eventId;
    private String customer;
    private String hostname;
    private String ipAddress;
    private String name;

    public ReadyStateHolder(String eventId, String customer, String hostname, String ipAddress) {
        this.eventId = eventId;
        this.customer = customer;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.name = customer.toUpperCase() + "_" + hostname;
    }

    public String getEventId() {
        return eventId;
    }

    public String getCustomer() {
        return customer;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getName() {
        return name;
    }
}
