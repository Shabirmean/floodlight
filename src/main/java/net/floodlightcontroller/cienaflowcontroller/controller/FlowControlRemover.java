package net.floodlightcontroller.cienaflowcontroller.controller;

import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by shabirmean on 2018-05-30 with some hope.
 */
public class FlowControlRemover {
    protected static Logger logger = LoggerFactory.getLogger(FlowControlRemover.class);
    private String customer;

    public FlowControlRemover(String customer) {
        this.customer = customer;
    }

    public void clearOVSFlows(IOFSwitch ovsSwitch, HashMap<String, Integer> eventIPsAndTableIds,
                       ConcurrentHashMap<String, OFPort> ipsToOVSPortsMap) {
        for (String containerIp : eventIPsAndTableIds.keySet()) {
            int tableId = eventIPsAndTableIds.get(containerIp);
            OFPort portId = ipsToOVSPortsMap.get(containerIp);
            logger.info("IP: " + containerIp + ", TID: " + tableId + ", PID: " + portId);
            deleteFlowByInPort(ovsSwitch, portId);
            deleteFlowByInTableId(ovsSwitch, tableId);
            deleteFlowByDestinationIP(ovsSwitch, containerIp);
        }
        System.out.println("--------------------------------------------");
    }

    private void deleteFlowByInPort(IOFSwitch ovsSwitch, OFPort inPort) {
        logger.info("Deleting (OF) controls for given OVS Port: " + inPort);
        //TODO:: NEED to check if its an entry container
        OFFactory ofFactory = ovsSwitch.getOFFactory();
        Match flowMatchByPort = ofFactory.buildMatch()
                .setExact(MatchField.IN_PORT, inPort)
                .build();

        OFFlowDelete.Builder builder = ofFactory.buildFlowDelete();
        OFFlowDelete deleteFlowWithPortId = builder.setMatch(flowMatchByPort).build();
        ovsSwitch.write(deleteFlowWithPortId);
    }

    private void deleteFlowByInTableId(IOFSwitch ovsSwitch, int tableId) {
        logger.info("Deleting (OF) controls for given OVS Flow table: " + tableId);
        //TODO:: NEED to check if its an entry container
        OFFactory ofFactory = ovsSwitch.getOFFactory();
        OFFlowDelete.Builder builder = ofFactory.buildFlowDelete();
        OFFlowDelete deleteFlowWithPortId = builder.setTableId(TableId.of(tableId)).build();
        ovsSwitch.write(deleteFlowWithPortId);
    }

    private void deleteFlowByDestinationIP(IOFSwitch ovsSwitch, String ipAddress) {
        logger.info("Deleting (OF) controls for given destination IP address: " + ipAddress);
        //TODO:: NEED to check if its an entry container
        OFFactory ofFactory = ovsSwitch.getOFFactory();
        Match flowMatchByDestinationIP = ofFactory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, IPv4Address.of(ipAddress))
                .build();
        OFFlowDelete.Builder builder = ofFactory.buildFlowDelete();
        OFFlowDelete deleteFlowWithDestinationIp = builder.setMatch(flowMatchByDestinationIP).build();
        ovsSwitch.write(deleteFlowWithDestinationIp);
    }

    public String getCustomer() {
        return customer;
    }
}