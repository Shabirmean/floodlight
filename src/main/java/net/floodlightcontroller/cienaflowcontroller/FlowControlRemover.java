package net.floodlightcontroller.cienaflowcontroller;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
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
    private OFPort inOFPort;

    private int tableId;
    private String customer;

    FlowControlRemover(IOFSwitch ovsSwitch, OFFactory ofFactory, Ethernet eth, OFPort inOFPort) {
        this.ovsSwitch = ovsSwitch;
        this.ofFactory = ofFactory;
        this.eth = eth;
        this.inOFPort = inOFPort;
    }

    void processEventStatusUDP(FlowRepository cienaFlowRepository, int tableId) {
        logger.info("[From an ingress container] Processing received UDP Packet.");
        IPv4 ipv4 = (IPv4) eth.getPayload();
        UDP udp = (UDP) ipv4.getPayload();
        Data udpData = (Data) udp.getPayload();
        byte[] udpDataBytes = udpData.getData();
        String udpDataString = new String(udpDataBytes);
        String[] stringElements = udpDataString.split(COLON);

        String eventId = stringElements[EVENT_ID_INDEX];
        String customer = stringElements[CUSTOMER_INDEX];

        String responseString = String.format(RESPONSE_MSG_FORMAT_TERMINATE, eventId, udpDataString);
        FlowController.respondToContainerManager(MQTT_PUBLISH_TERMINATE, responseString);

        this.tableId = tableId;
        this.customer = customer;
        cienaFlowRepository.getFlowControlsRemoverMap().put(eventId, this);
    }


    void clearOVSFlows(HashMap<String, Integer> eventIPsAndTableIds,
                       ConcurrentHashMap<String, OFPort> ipsToOVSPortsMap) {
        MacAddress switchMac = ovsSwitch.getPort(OFPort.LOCAL).getHwAddr();
        OFFlowDelete.Builder deleteBuilder = ofFactory.buildFlowDelete();

//        ovsSwitch.getPorts()
//
//
//        deleteFlowsWithIPAsSrc();
//        deleteFlowsWithIPAsDst();
//        deleteAllTableFlows();
//
//
//
//        deleteFlowsToIngressContainers();
//        deleteFlowToAndFromOVS();
//        deleteGotoTableFlow();
//        deleteAllTableFlows();

    }

    private void deleteFlowsToIngressContainers() {
        logger.info("Deleting (OF) controls for packets between the Ingress Containers and their immediate neighbours");
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

    public String getCustomer() {
        return customer;
    }


}




//        OFFlowMod.Builder fmb = ovsSwitch.getOFFactory().buildFlowDelete();;
//
//
//        MacAddress srcMac = eth.getSourceMACAddress();
//        MacAddress dstMac = eth.getDestinationMACAddress();
//        IPv4Address srcIp = ipv4.getSourceAddress();
//        IPv4Address dstIp = ipv4.getDestinationAddress();
//
//        //TODO:: NEED to check if its an entry container
//        Match deleteFlowMatch = ofFactory.buildMatch()
//                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//                .setExact(MatchField.IPV4_SRC, srcIp)
//                .setExact(MatchField.ETH_SRC, srcMac)
//                .setExact(MatchField.ETH_DST, dstMac)
//                .setExact(MatchField.IPV4_DST, dstIp)
//                .setExact(MatchField.IN_PORT, inOFPort)
//                .build();
//
//
//        fmb.setMatch(deleteFlowMatch);
//        fmb.setBufferId(OFBufferId.NO_BUFFER);
//        fmb.setPriority(MAX_PRIORITY);
//        fmb.setTableId(TableId.of(DEFAULT_FLOW_TABLE));
//        fmb.build();
//        fmb.setOutPort(OFPort.ANY);
//
//
//
//        // set the ofp_action_header/out actions:
//        // from the openflow 1.0 spec: need to set these on a struct ofp_action_output:
//        // uint16_t type; /* OFPAT_OUTPUT. */
//        // uint16_t len; /* Length is 8. */
//        // uint16_t port; /* Output port. */
//        // uint16_t max_len; /* Max length to send to controller. */
//        // type/len are set because it is OFActionOutput,
//        // and port, max_len are arguments to this constructor
//        List<OFAction> al = new ArrayList<OFAction>();
//        al.add(ovsSwitch.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(0xffFFffFF).build());
//
//        FlowModUtils.setActions(fmb, al, sw);
//
//        if (log.isTraceEnabled()) {
//            log.trace("{} {} flow mod {}",
//                    new Object[]{ sw, (command == OFFlowModCommand.DELETE) ? "deleting" : "adding", fmb.build() });
//        }
//
//        counterFlowMod.increment();
//
//        // and write it out
//        sw.write(fmb.build());