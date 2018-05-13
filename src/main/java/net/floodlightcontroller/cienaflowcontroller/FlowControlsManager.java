package net.floodlightcontroller.cienaflowcontroller;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2018-05-10 with some hope.
 */
class FlowControlsManager {
    protected static Logger logger = LoggerFactory.getLogger(FlowControlsManager.class);

    private static final int EVENT_ID_INDEX = 0;
    private static final int CUSTOMER_INDEX = 1;
    private static final int HOSTNAME_INDEX = 2;

    private IOFSwitch ovsSwitch;
    private OFFactory ofFactory;
    private Ethernet eth;
    private OFPort inOFPort;

    FlowControlsManager(IOFSwitch ovsSwitch, OFFactory ofFactory, Ethernet eth, OFPort inOFPort){
        this.ovsSwitch = ovsSwitch;
        this.ofFactory = ofFactory;
        this.eth = eth;
        this.inOFPort = inOFPort;
    }


    void processReadyStateUDP(FlowRepository cienaFlowRepository, IPv4 ipv4) {
        logger.info("Processing received UDP Packet.");
        IPAddress srcIp = ipv4.getSourceAddress();
        UDP udp = (UDP) ipv4.getPayload();
        Data udpData = (Data) udp.getPayload();
        byte[] udpDataBytes = udpData.getData();
        String udpDataString = new String(udpDataBytes);

        if (udpDataString.contains(CONTAINER_READY)) {
            logger.info("[UDP Packet with ready state] " + udpDataString);
            // "<EVENT_ID>:<CUSTOMER>:<HOSTNAME>:READY"
            String[] stringElements = udpDataString.split(COLON);
            String eventId = stringElements[EVENT_ID_INDEX];
            String customer = stringElements[CUSTOMER_INDEX];
            String hostname = stringElements[HOSTNAME_INDEX];
            String containerIp = srcIp.toString();
            ReadyStateHolder readyCon = new ReadyStateHolder(eventId, customer, hostname, containerIp);
            cienaFlowRepository.addReadyStateContainer(readyCon);
        }
    }

    void addAllowIngressToNeighbourFlow() {
        logger.info("Adding (OF) controls for packets between the Ingress Containers and their immediate neighbours");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        MacAddress srcMac = eth.getSourceMACAddress();
        MacAddress dstMac = eth.getDestinationMACAddress();
        IPv4Address srcIp = ipv4.getSourceAddress();
        IPv4Address dstIp = ipv4.getDestinationAddress();

        //TODO:: NEED to check if its an entry container
        Match ingressFlowMatch = ofFactory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, srcIp)
                .setExact(MatchField.ETH_SRC, srcMac)
                .setExact(MatchField.ETH_DST, dstMac)
                .setExact(MatchField.IPV4_DST, dstIp)
                .setExact(MatchField.IN_PORT, inOFPort)
                .build();

        OFActions actions = ofFactory.actions();
        OFInstructions instructions = ofFactory.instructions();
        OFFlowAdd.Builder builder = ofFactory.buildFlowAdd();
        ArrayList<OFInstruction> ingressFlowInstructionList = new ArrayList<>();
        ArrayList<OFAction> ingressFlowActionList = new ArrayList<>();

        OFActionOutput ingressFlowAction =
                actions.buildOutput().setMaxLen(0xFFffFFff).setPort(OFPort.NORMAL).build();
        ingressFlowActionList.add(ingressFlowAction);
        OFInstructionApplyActions ingressFlowInstruction =
                instructions.buildApplyActions().setActions(ingressFlowActionList).build();
        ingressFlowInstructionList.add(ingressFlowInstruction);

        OFFlowAdd allowIngressFlow = builder
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(MAX_PRIORITY)
                .setMatch(ingressFlowMatch)
                .setInstructions(ingressFlowInstructionList)
                .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                .build();
        ovsSwitch.write(allowIngressFlow);
    }

    void addAllowFlowsToAndFromOVS() {
        logger.info("Adding (OF) controls for packets TO and FROM the OVS (Switch)");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        MacAddress srcMac = eth.getSourceMACAddress();
        MacAddress dstMac = eth.getDestinationMACAddress();
        IPv4Address srcIp = ipv4.getSourceAddress();
        IPv4Address dstIp = ipv4.getDestinationAddress();

        Match ovsFlowMatch = ofFactory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, srcMac)
                .setExact(MatchField.IPV4_SRC, srcIp)
                .setExact(MatchField.ETH_DST, dstMac)
                .setExact(MatchField.IPV4_DST, dstIp)
                .build();

        OFFlowAdd.Builder builder = ofFactory.buildFlowAdd();
        ArrayList<OFInstruction> ovsFlowInstructionList = new ArrayList<>();
        ArrayList<OFAction> ovsFlowActionList = new ArrayList<>();

        OFActions actions = ofFactory.actions();
        OFInstructions instructions = ofFactory.instructions();
        OFActionOutput ovsFlowAction =
                actions.buildOutput().setMaxLen(0xFFffFFff).setPort(OFPort.NORMAL).build();
        ovsFlowActionList.add(ovsFlowAction);
        OFInstructionApplyActions ovsFlowInstruction =
                instructions.buildApplyActions().setActions(ovsFlowActionList).build();
        ovsFlowInstructionList.add(ovsFlowInstruction);
        OFFlowAdd allowOVSFlow = builder
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(MAX_PRIORITY)
                .setMatch(ovsFlowMatch)
                .setInstructions(ovsFlowInstructionList)
                .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                .build();
        ovsSwitch.write(allowOVSFlow);
    }

    void gotoContainerSpecificFlowTable(Integer tableId) {
        logger.info("Adding (OF) controls to jump to container specific flow-table");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        MacAddress srcMac = eth.getSourceMACAddress();
        IPv4Address srcIp = ipv4.getSourceAddress();

        Match topLevelMatch = ofFactory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, srcIp)
                .setExact(MatchField.ETH_SRC, srcMac)
                .setExact(MatchField.IN_PORT, inOFPort)
//                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .build();

        OFInstructions instructions = ofFactory.instructions();
        ArrayList<OFInstruction> gotoTableInstructionList = new ArrayList<>();
        OFInstructionGotoTable gotoTableInstruction =
                instructions.buildGotoTable().setTableId(TableId.of(tableId)).build();
        gotoTableInstructionList.add(gotoTableInstruction);

        OFFlowAdd goToTableFlow = ofFactory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(MAX_PRIORITY - 1)
                .setMatch(topLevelMatch)
                .setInstructions(gotoTableInstructionList)
                .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                .build();
        ovsSwitch.write(goToTableFlow);
    }

    void addAllowFlowToNeighbours(IPv4Address srcIp, Integer tableId, List<IPv4Address> neighbours) {
        logger.info("Adding (OF) controls for  of container [" + srcIp.toString() + "] in table - " + tableId);
        OFFlowAdd.Builder builder = ofFactory.buildFlowAdd();
        for (IPv4Address newNeighbour : neighbours) {
            logger.info("NEIGHBOUR IP-ADDRESS - " + newNeighbour.toString());
            Match allowAdjacentFlowMatch = ofFactory.buildMatch()
                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                    .setExact(MatchField.IPV4_DST, newNeighbour)
                    .setExact(MatchField.IN_PORT, inOFPort)
                    .build();

            OFActions actions = ofFactory.actions();
            OFInstructions instructions = ofFactory.instructions();
            ArrayList<OFInstruction> normalFlowInstructionList = new ArrayList<>();
            ArrayList<OFAction> normalFlowActionList = new ArrayList<>();

            OFActionOutput normalFlowAction =
                    actions.buildOutput().setMaxLen(0xFFffFFff).setPort(OFPort.NORMAL).build();
            normalFlowActionList.add(normalFlowAction);
            OFInstructionApplyActions normalFlowInstruction =
                    instructions.buildApplyActions().setActions(normalFlowActionList).build();
            normalFlowInstructionList.add(normalFlowInstruction);
            OFFlowAdd allowNormalFlow = builder
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setPriority(MAX_PRIORITY)
                    .setMatch(allowAdjacentFlowMatch)
                    .setInstructions(normalFlowInstructionList)
                    .setTableId(TableId.of(tableId))
                    .build();
            ovsSwitch.write(allowNormalFlow);
        }
    }

    void allowUDPFlowsToOVS(Integer tableId){
        logger.info("Adding (OF) controls to drop all other flows from the container in its flow-table");
        MacAddress switchMac = ovsSwitch.getPort(OFPort.LOCAL).getHwAddr();
        IPv4 ipv4 = (IPv4) eth.getPayload();
        MacAddress srcMac = eth.getSourceMACAddress();
        IPv4Address srcIp = ipv4.getSourceAddress();

        Match allowUDPFlowMatch = ofFactory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//                .setExact(MatchField.IPV4_SRC, srcIp)
//                .setExact(MatchField.ETH_SRC, srcMac)
                .setExact(MatchField.ETH_DST, switchMac)
                .setExact(MatchField.IN_PORT, inOFPort)
//                .setExact(MatchField.IP_PROTO, IpProtocol.UDP)
                .build();

        OFActions actions = ofFactory.actions();
        OFInstructions instructions = ofFactory.instructions();
        ArrayList<OFInstruction> normalFlowInstructionList = new ArrayList<>();
        ArrayList<OFAction> normalFlowActionList = new ArrayList<>();

        OFActionOutput normalFlowAction =
                actions.buildOutput().setMaxLen(0xFFffFFff).setPort(OFPort.NORMAL).build();
        normalFlowActionList.add(normalFlowAction);
        OFInstructionApplyActions normalFlowInstruction =
                instructions.buildApplyActions().setActions(normalFlowActionList).build();
        normalFlowInstructionList.add(normalFlowInstruction);
        OFFlowAdd allowUDPFlow = ofFactory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(MAX_PRIORITY - 1)
                .setMatch(allowUDPFlowMatch)
                .setInstructions(normalFlowInstructionList)
                .setTableId(TableId.of(tableId))
                .build();
        ovsSwitch.write(allowUDPFlow);
    }

    void dropAllOtherFlows(Integer tableId) {
        logger.info("Adding (OF) controls to drop all other flows from the container in its flow-table");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        MacAddress srcMac = eth.getSourceMACAddress();
        IPv4Address srcIp = ipv4.getSourceAddress();

        Match topLevelMatch = ofFactory.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, srcIp)
                .setExact(MatchField.ETH_SRC, srcMac)
                .setExact(MatchField.IN_PORT, inOFPort)
//                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .build();

        OFInstructions instructions = ofFactory.instructions();
        ArrayList<OFInstruction> dropFlowInstructionList = new ArrayList<>();
        ArrayList<OFAction> dropFlowActionList = new ArrayList<>();
        OFInstructionApplyActions dropFlowInstruction =
                instructions.buildApplyActions().setActions(dropFlowActionList).build();
        dropFlowInstructionList.add(dropFlowInstruction);
        OFFlowAdd dropFlow = ofFactory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setPriority(MAX_PRIORITY - 2)
                .setMatch(topLevelMatch)
                .setInstructions(dropFlowInstructionList)
                .setTableId(TableId.of(tableId))
                .build();
        ovsSwitch.write(dropFlow);
    }
}
