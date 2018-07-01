package net.floodlightcontroller.cienaflowcontroller;

import net.floodlightcontroller.core.IOFConnection;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2018-05-30 with some hope.
 */
public class FlowControlRemover {
    protected static Logger logger = LoggerFactory.getLogger(FlowControlRemover.class);
    private static final int EVENT_ID_INDEX = 0;
    private static final int CUSTOMER_INDEX = 1;
    private IOFSwitch ovsSwitch;
    private OFFactory ofFactory;
    private Ethernet eth;

    private String customer;
    private boolean isTerminated = false;
    private HashMap<String, Integer> eventIPsAndTableIds;
    private ConcurrentHashMap<String, OFPort> ipsToOVSPortsMap;

//    FlowControlRemover() {
    FlowControlRemover(IOFSwitch ovsSwitch, OFFactory ofFactory, Ethernet eth) {
        this.ovsSwitch = ovsSwitch;
        this.ofFactory = ofFactory;
        this.eth = eth;
    }

    void processEventStatusUDP(Ethernet eth, FlowRepository cienaFlowRepository) {
        logger.info("[From an ingress container] Processing received UDP Packet.");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        UDP udp = (UDP) ipv4.getPayload();
        Data udpData = (Data) udp.getPayload();
        byte[] udpDataBytes = udpData.getData();
        String udpDataString = new String(udpDataBytes);
        String[] stringElements = udpDataString.split(COLON);

        String eventId = stringElements[EVENT_ID_INDEX];
        this.customer = stringElements[CUSTOMER_INDEX];
        cienaFlowRepository.getFlowControlsRemoverMap().put(eventId, this);

        String responseString = String.format(RESPONSE_MSG_FORMAT_TERMINATE, eventId, udpDataString);
        FlowController.respondToContainerManager(MQTT_PUBLISH_TERMINATE, responseString);

//        HashMap<String, Integer> eventIPsAndTableIds = cienaFlowRepository.cleanUpEventStructures(eventId, customer);
//        setStructuresForFlowDeletion(eventIPsAndTableIds, cienaFlowRepository.getIpToOVSPortNumberMap());

    }

    void setStructuresForFlowDeletion(HashMap<String, Integer> eventIPsAndTableIds,
                                      ConcurrentHashMap<String, OFPort> ipsToOVSPortsMap) {
        logger.info(">>>>>>>>>>>>>>> Setting termination state to TRUE <<<<<<<<<<<<<<<<<<<<<");
        this.eventIPsAndTableIds = eventIPsAndTableIds;
        this.ipsToOVSPortsMap = ipsToOVSPortsMap;
//        this.clearOVSFlows(this.ovsSwitch);
//        this.isTerminated = true;
    }

    void clearOVSFlows(IOFSwitch ovsSwitch) {
        for (String containerIp : eventIPsAndTableIds.keySet()) {
            int tableId = eventIPsAndTableIds.get(containerIp);
            OFPort portId = ipsToOVSPortsMap.get(containerIp);
            logger.info("IP: " + containerIp + ", TID: " + tableId + ", PID: " + portId);
            deleteFlowByInPort(ovsSwitch, portId);
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

    boolean isTerminated() {
        return isTerminated;
    }

    public void setTerminated(boolean terminated) {
        isTerminated = terminated;
    }
}