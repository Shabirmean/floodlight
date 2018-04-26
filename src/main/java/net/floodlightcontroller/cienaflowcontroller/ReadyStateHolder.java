package net.floodlightcontroller.cienaflowcontroller;

/**
 * Created by shabirmean on 2018-04-25 with some hope.
 */
public class ReadyStateHolder {
    private String eventId;
    private String customer;
    private String hostname;
    private String ipAddress;

    public ReadyStateHolder(String eventId, String customer, String hostname, String ipAddress) {
        this.eventId = eventId;
        this.customer = customer;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
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
}
