package net.floodlightcontroller.cienaflowcontroller;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.util.OFMessageUtils;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
public class FlowController implements IOFMessageListener, IFloodlightModule {
    protected static Logger logger;
    protected IFloodlightProviderService floodlightProvider;
    static ConcurrentHashMap<String, String> subnetToCustomerMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, HashMap<String, CustomerContainer>> containerMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ArrayList<String>> customerEventIdMap = new ConcurrentHashMap<>();
    static HashSet<String> entryContainerIPSet = new HashSet<>();

    private static final int ADJACENT_CONTAINERS = 2;
    private static final int LEFT_CONTAINER_INDX = 0;
    private static final int RIGHT_CONTAINER_INDX = 1;
    private static final String INGRESS_IP = "192.168.0.250";
    private static final String SWITCH_IP = "192.168.0.1";

    @Override
    public String getName() {
        return FlowController.class.getSimpleName();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> flServices = new ArrayList<>();
        flServices.add(IFloodlightProviderService.class);
        return flServices;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        logger = LoggerFactory.getLogger(FlowController.class);
        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        MqttListener mqttListener = new MqttListener();
        mqttListener.init();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        debugPrint("CIENA FLOW CONTROLLER IS REGISTERED.");
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
//        return (type.equals(OFType.PACKET_IN) && (name.equals("topology") || name.equals("devicemanager")));
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    //TODO:: Strictly check for OpenFLow Versions in all of the messages
    @Override
    public Command receive(IOFSwitch ovsSwitch, OFMessage msg, FloodlightContext cntx) {
        OFFactory myFactory = OFFactories.getFactory(msg.getVersion());
        OFActions actions = myFactory.actions();
        OFOxms oxms = myFactory.oxms();
        OFInstructions instructions = myFactory.instructions();

        OFPort ofPort = OFMessageUtils.getInPort((OFPacketIn) msg);
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

        SocketAddress ovsSocketAddress = ovsSwitch.getInetAddress();
        InetSocketAddress inetAddr = (InetSocketAddress) ovsSocketAddress;
        IPv4Address ovsIpv4 = IPv4Address.of(inetAddr.getHostString());

        logger.info("########## OVS-SWITCH SOCKET ADD: " + ovsSocketAddress);
        logger.info("########## OVS-SWITCH INET ADDR: " + inetAddr);
        logger.info("########## OVS-SWITCH IPv4 ADDR: " + ovsIpv4);

        try {
            if (eth.getEtherType() == EthType.IPv4) {
                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();
                IPv4 ipv4 = (IPv4) eth.getPayload();
                IPv4Address srcIp = ipv4.getSourceAddress();
                IPv4Address dstIp = ipv4.getDestinationAddress();
                logger.info("################ SOURCE: {} seen with IP: {}", srcMac, srcIp);
                logger.info("################ DESTINATION: {} seen with IP: {}", dstMac, dstIp);
                logger.info("----------------------------------------------------------------");

                if ( ( srcIp.toString().equals(INGRESS_IP) && entryContainerIPSet.contains(dstIp.toString()) ) ||
                        ( entryContainerIPSet.contains(srcIp.toString()) && dstIp.toString().equals(INGRESS_IP) ) ) {
                    //TODO:: NEED to check if its an entry container
                    Match ingressFlowMatch = myFactory.buildMatch()
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_SRC, srcIp)
                            .setExact(MatchField.ETH_SRC, srcMac)
                            .setExact(MatchField.ETH_DST, dstMac)
                            .setExact(MatchField.IPV4_DST, dstIp)
                            .setExact(MatchField.IN_PORT, ofPort)
                            .build();

                    OFFlowAdd.Builder builder = myFactory.buildFlowAdd();
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
//                        .setHardTimeout(3600)
//                        .setIdleTimeout(10)
                            .setPriority(MAX_PRIORITY)
                            .setMatch(ingressFlowMatch)
                            .setInstructions(ingressFlowInstructionList)
                            .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                            .build();
                    ovsSwitch.write(allowIngressFlow);
                    return Command.CONTINUE;

                } else if (srcIp.toString().equals(SWITCH_IP) || dstIp.toString().equals(SWITCH_IP)) {
                    Match ovsFlowMatch = myFactory.buildMatch()
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_SRC, srcIp)
                            .setExact(MatchField.ETH_SRC, srcMac)
                            .build();

                    OFFlowAdd.Builder builder = myFactory.buildFlowAdd();
                    ArrayList<OFInstruction> ovsFlowInstructionList = new ArrayList<>();
                    ArrayList<OFAction> ovsFlowActionList = new ArrayList<>();

                    OFActionOutput ovsFlowAction =
                            actions.buildOutput().setMaxLen(0xFFffFFff).setPort(OFPort.NORMAL).build();
                    ovsFlowActionList.add(ovsFlowAction);
                    OFInstructionApplyActions ovsFlowInstruction =
                            instructions.buildApplyActions().setActions(ovsFlowActionList).build();
                    ovsFlowInstructionList.add(ovsFlowInstruction);
                    OFFlowAdd allowOVSFlow = builder
                            .setBufferId(OFBufferId.NO_BUFFER)
//                        .setHardTimeout(3600)
//                        .setIdleTimeout(10)
                            .setPriority(MAX_PRIORITY)
                            .setMatch(ovsFlowMatch)
                            .setInstructions(ovsFlowInstructionList)
                            .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                            .build();
                    ovsSwitch.write(allowOVSFlow);


                    ovsFlowMatch = myFactory.buildMatch()
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_DST, dstIp)
                            .setExact(MatchField.ETH_DST, dstMac)
                            .build();
                    allowOVSFlow = builder
                            .setBufferId(OFBufferId.NO_BUFFER)
//                        .setHardTimeout(3600)
//                        .setIdleTimeout(10)
                            .setPriority(MAX_PRIORITY)
                            .setMatch(ovsFlowMatch)
                            .setInstructions(ovsFlowInstructionList)
                            .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                            .build();
                    ovsSwitch.write(allowOVSFlow);
                    return Command.CONTINUE;
                }

//                IPv4Address dstIp = ipv4.getDestinationAddress();
                String customer = getCustomerFromSubnet(srcIp);
                HashMap<String, CustomerContainer> customerContainers = containerMap.get(customer);
                CustomerContainer srcContainer = customerContainers.get(srcIp.toString());
//                CustomerContainer dstContainer = customerContainers.get(dstIp.toString());

                Match topLevelMatch = myFactory.buildMatch()
                        .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                        .setExact(MatchField.IPV4_SRC, srcIp)
                        .setExact(MatchField.ETH_SRC, srcMac)
                        .setExact(MatchField.IN_PORT, ofPort)
                        .build();

                ArrayList<OFInstruction> gotoTableInstructionList = new ArrayList<>();
                OFInstructionGotoTable gotoTableInstruction =
                        instructions.buildGotoTable().setTableId(TableId.of(ofPort.getPortNumber())).build();
                gotoTableInstructionList.add(gotoTableInstruction);

                OFFlowAdd goToTableFlow = myFactory.buildFlowAdd()
                        .setBufferId(OFBufferId.NO_BUFFER)
//                        .setHardTimeout(3600)
//                        .setIdleTimeout(10)
                        .setPriority(MAX_PRIORITY - 1)
                        .setMatch(topLevelMatch)
                        .setInstructions(gotoTableInstructionList)
                        .setTableId(TableId.of(DEFAULT_FLOW_TABLE))
                        .build();
                ovsSwitch.write(goToTableFlow);

                CustomerContainer[] adjacentContainers = getAdjacentContainers(srcContainer, customerContainers);
                OFFlowAdd.Builder builder = myFactory.buildFlowAdd();
                for (CustomerContainer adjContainer : adjacentContainers) {
                    if (adjContainer != null) {
                        IPv4Address ipAdd = IPv4Address.of(adjContainer.getIpAddress());
//                        MacAddress macAdd = MacAddress.of(adjContainer.getMacAddress());
                        Match allowAdjacentFlowMatch = myFactory.buildMatch()
                                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                .setExact(MatchField.IPV4_DST, ipAdd)
//                                .setExact(MatchField.ETH_DST, macAdd)
                                .setExact(MatchField.IN_PORT, ofPort)
                                .build();

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
//                        .setHardTimeout(3600)
//                        .setIdleTimeout(10)
                                .setPriority(MAX_PRIORITY)
                                .setMatch(allowAdjacentFlowMatch)
                                .setInstructions(normalFlowInstructionList)
                                .setTableId(TableId.of(ofPort.getPortNumber()))
                                .build();
                        ovsSwitch.write(allowNormalFlow);
                    }
                }

                ArrayList<OFInstruction> dropFlowInstructionList = new ArrayList<>();
                ArrayList<OFAction> dropFlowActionList = new ArrayList<>();
                OFInstructionApplyActions dropFlowInstruction =
                        instructions.buildApplyActions().setActions(dropFlowActionList).build();
                dropFlowInstructionList.add(dropFlowInstruction);
                OFFlowAdd dropFlow = myFactory.buildFlowAdd()
                        .setBufferId(OFBufferId.NO_BUFFER)
//                        .setHardTimeout(3600)
//                        .setIdleTimeout(10)
                        .setPriority(MAX_PRIORITY - 2)
                        .setMatch(topLevelMatch)
                        .setInstructions(dropFlowInstructionList)
                        .setTableId(TableId.of(ofPort.getPortNumber()))
                        .build();

                ovsSwitch.write(dropFlow);
            }

        } catch (FlowControllerException e) {
            //TODO:: Handle exceptions properly
            e.printStackTrace();
        }
        return Command.CONTINUE;
    }


    private String getCustomerFromSubnet(IPv4Address srcIp) throws FlowControllerException {
        String[] ipStringArr = srcIp.toString().split("\\" + PERIOD);
        StringBuilder subnetString = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            subnetString.append(ipStringArr[i]).append(PERIOD);
        }
        subnetString.append(ZERO);
        String customer = subnetToCustomerMap.get(subnetString.toString());
        if (customer == null) {
            throw new FlowControllerException("Invalid subnet [" + subnetString + "]. " +
                    "No customer found with matching subnet for IP: " + srcIp);
        }
        return customer;
    }

    private CustomerContainer[] getAdjacentContainers(CustomerContainer srcContainer,
                                                      HashMap<String, CustomerContainer> containerList) {
        CustomerContainer[] adjacentContainers = new CustomerContainer[ADJACENT_CONTAINERS];
        String srcIp = srcContainer.getIpAddress();
        int pipelineIndx = srcContainer.getPipeLineIndex();
        for (CustomerContainer cusContainer : containerList.values()) {
            if (!cusContainer.getIpAddress().equals(srcIp)) {
                int pIndex = cusContainer.getPipeLineIndex();
                if (pIndex == (pipelineIndx + 1)) {
                    adjacentContainers[RIGHT_CONTAINER_INDX] = cusContainer;
                } else if (pIndex == (pipelineIndx - 1)) {
                    adjacentContainers[LEFT_CONTAINER_INDX] = cusContainer;
                }
            }
        }
        return adjacentContainers;
    }

    private void debugPrint(String line) {
        logger.debug("########################### : " + line);
    }
}





/*
{
	"customer":"%s",
	"subnet":"%s",
	"eventId":"%s",
	"count":"%s",
	"containers":[
					{"key":"%s","cId":"%s","name":"%s","ip":"%s"},
					{"key":"%s","cid":"%s","name":"%s","ip":"%s"},
					{"key":"%s","cId":"%s","name":"%s","ip":"%s"},
					{"key":"%s","cId":"%s","name":"%s","ip":"%s"}
	]
}

*/