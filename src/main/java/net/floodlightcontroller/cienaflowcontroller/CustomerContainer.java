package net.floodlightcontroller.cienaflowcontroller;


/**
 * Created by shabirmean on 2017-11-22 with some hope.
 */
public class CustomerContainer {
    private static final String CONTNR_STRING_FORMAT =
            "{\"key\":\"%s\",\"id\":\"%s\",\"name\":\"%s\",\"ip\":\"%s\",\"mac\":\"%s\"}";
    private String customer;
    private String cId;
    private String key;
    private String name;
    private String ipAddress;
    private String macAddress;
    private int pipeLineIndex;
    private boolean borderContainer;

    public CustomerContainer(String customer, String cId, String key, String name){
        this.customer = customer;
        this.cId = cId;
        this.key = key;
        this.name = name;
    }

    public CustomerContainer(String customer, String cId, String key, String name, String ip, String mac){
        this.customer = customer;
        this.cId = cId;
        this.key = key;
        this.name = name;
        this.ipAddress = ip;
        this.macAddress = mac;
    }

//    public int getFlowTableId(){
//        String subnetNum = this.ipAddress.split(".")[2];
//        return Integer.parseInt(subnetNum + Integer.toString(pipeLineIndex));
//    }

    public String getJSONString(){
        return String.format(CONTNR_STRING_FORMAT, key, cId, name, ipAddress, macAddress);
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

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getPipeLineIndex() {
        return pipeLineIndex;
    }

    public void setPipeLineIndex(int pipeLineIndex) {
        this.pipeLineIndex = pipeLineIndex;
    }

    public boolean isBorderContainer() {
        return borderContainer;
    }

    public void setBorderContainer(boolean borderContainer) {
        this.borderContainer = borderContainer;
    }

}
