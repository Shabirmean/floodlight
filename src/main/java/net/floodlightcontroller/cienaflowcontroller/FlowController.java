package net.floodlightcontroller.cienaflowcontroller;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.internal.OFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.*;
import net.floodlightcontroller.util.OFMessageUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;
import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
public class FlowController implements IOFMessageListener, IFloodlightModule {
    protected static Logger logger;
    protected IFloodlightProviderService floodlightProvider;

    private ConcurrentHashMap<String, Integer> ipToTableIdMap;
    private FlowRepository cienaFlowRepository;
    private BitSet flowTableBits;

    private static final int EVENT_ID_INDEX = 0;
    private static final int CUSTOMER_INDEX = 1;
    private static final int HOSTNAME_INDEX = 2;
    private static final int MAX_TABLE_IDS = 256;

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
        this.ipToTableIdMap = new ConcurrentHashMap<>();
        this.cienaFlowRepository = new FlowRepository();
        this.flowTableBits = new BitSet(MAX_TABLE_IDS);
        this.flowTableBits.set(DEFAULT_FLOW_TABLE);
        MqttListener mqttListener = new MqttListener(MQTT_BROKER_URI, MQTT_SUBSCRIBE_TOPIC);
        mqttListener.init(cienaFlowRepository);
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

        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        OFPort inOFPort = OFMessageUtils.getInPort((OFPacketIn) msg);
        int inPortNumber = inOFPort.getPortNumber();

        SocketAddress ovsSocketAddress = ovsSwitch.getInetAddress();
        InetSocketAddress inetAddr = (InetSocketAddress) ovsSocketAddress;
        IPv4Address ovsIpv4 = IPv4Address.of(inetAddr.getHostString());
        MacAddress switchMac = ovsSwitch.getPort(OFPort.LOCAL).getHwAddr();
        MacAddress srcMac;
        MacAddress dstMac;
        IPv4Address srcIp;
        IPv4Address dstIp;

        try {
            if (eth.getEtherType() == EthType.IPv4) {
                IPv4 ipv4 = (IPv4) eth.getPayload();
                srcMac = eth.getSourceMACAddress();
                dstMac = eth.getDestinationMACAddress();
                srcIp = ipv4.getSourceAddress();
                dstIp = ipv4.getDestinationAddress();
//                logger.info("################ SOURCE: {} seen with IP: {}", srcMac, srcIp);
//                logger.info("################ DESTINATION: {} seen with IP: {}", dstMac, dstIp);
//                logger.info("----------------------------------------------------------------");

                //MUST CHECK THIS
                if (ipv4.getProtocol() == IpProtocol.UDP && srcMac != switchMac) {
                    logger.info("########## OVS-SWITCH SOCKET ADD: " + ovsSocketAddress);
                    logger.info("########## OVS-SWITCH INET ADDR: " + inetAddr);
                    logger.info("########## OVS-SWITCH IPv4 ADDR: " + ovsIpv4);
                    logger.info("########## TABLE-ID: " + inOFPort);
                    logger.info("########## SWITCH-MAC: " + switchMac.toString());

                    logger.info("################ SOURCE: {} seen with IP: {}", srcMac, srcIp);
                    logger.info("################ DESTINATION: {} seen with IP: {}", dstMac, dstIp);
                    logger.info("----------------------------------------------------------------");


                    // "<EVENT_ID>:<CUSTOMER>:<HOSTNAME>:<IP_ADDRESS>:READY"
                    // "<EVENT_ID>:<CUSTOMER>:<HOSTNAME>:READY"
                    UDP udp = (UDP) ipv4.getPayload();
                    Data udpData = (Data) udp.getPayload();
                    byte[] udpDataBytes = udpData.getData();
                    String udpDataString = new String(udpDataBytes);

                    if (udpDataString.contains(CONTAINER_READY)) {
                        System.out.println("########>>>>>>> " + udpDataString);
                        String[] stringElements = udpDataString.split(COLON);
                        String eventId = stringElements[EVENT_ID_INDEX];
                        String customer = stringElements[CUSTOMER_INDEX];
                        String hostname = stringElements[HOSTNAME_INDEX];
                        String containerIp = srcIp.toString();
                        ReadyStateHolder readyCon = new ReadyStateHolder(eventId, customer, hostname, containerIp);
                        cienaFlowRepository.addReadyStateContainer(readyCon);
                    }
                } else {
                    if ((cienaFlowRepository.isIngressContainerIp(srcIp.toString()) &&
                            cienaFlowRepository.isNeighbourOfIngress(dstIp.toString())) ||
                            (cienaFlowRepository.isIngressContainerIp(dstIp.toString()) &&
                                    cienaFlowRepository.isNeighbourOfIngress(srcIp.toString()))) {
                        //TODO:: NEED to check if its an entry container
                        logger.info("######## CAME INSIDE HERE");
                        Match ingressFlowMatch = myFactory.buildMatch()
                                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                .setExact(MatchField.IPV4_SRC, srcIp)
                                .setExact(MatchField.ETH_SRC, srcMac)
                                .setExact(MatchField.ETH_DST, dstMac)
                                .setExact(MatchField.IPV4_DST, dstIp)
                                .setExact(MatchField.IN_PORT, inOFPort)
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

                    } else if (srcMac.toString().equals(switchMac.toString()) ||
                            dstMac.toString().equals(switchMac.toString())) {
                        logger.info("######## CAME INSIDE HERE 2");

                        Match ovsFlowMatch = myFactory.buildMatch()
                                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                .setExact(MatchField.ETH_SRC, srcMac)
                                .setExact(MatchField.IPV4_SRC, srcIp)
                                .setExact(MatchField.ETH_DST, dstMac)
                                .setExact(MatchField.IPV4_DST, dstIp)
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
                        return Command.CONTINUE;

                    }

                    logger.info("######## CAME INSIDE HERE 3");

                    Integer tableId = ipToTableIdMap.get(srcIp.toString());
                    if (tableId == null) {
                        tableId = createNewTableEntryForIP(srcIp.toString());
                        logger.info("%%%%%%%%%%% New Table Id " + tableId + " for IP " + srcIp.toString());
                    }

                    Match topLevelMatch = myFactory.buildMatch()
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_SRC, srcIp)
                            .setExact(MatchField.ETH_SRC, srcMac)
                            .setExact(MatchField.IN_PORT, inOFPort)
                            .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                            .build();

                    ArrayList<OFInstruction> gotoTableInstructionList = new ArrayList<>();
                    OFInstructionGotoTable gotoTableInstruction =
                            instructions.buildGotoTable().setTableId(TableId.of(tableId)).build();
//                            instructions.buildGotoTable().setTableId(TableId.of(inOFPort.getPortNumber())).build();
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


//                    String customer = cienaFlowRepository.getCustomerFromSubnet(srcIp);
//                    HashMap<String, CustomerContainer> customerContainers = containerMap.get(customer);
//                    CustomerContainer srcContainer = customerContainers.get(srcIp.toString());
//
//                    CustomerContainer[] adjacentContainers = getAdjacentContainers(srcContainer, customerContainers);
                    logger.info("######## Illegal IPs: " + srcIp.toString());
                    List<IPv4Address> neighbours = cienaFlowRepository.getNeighbourIps(srcIp);
                    OFFlowAdd.Builder builder = myFactory.buildFlowAdd();
                    for (IPv4Address newNeighbour : neighbours) {
//                        MacAddress macAdd = MacAddress.of(adjContainer.getMacAddress());
                        Match allowAdjacentFlowMatch = myFactory.buildMatch()
                                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                .setExact(MatchField.IPV4_DST, newNeighbour)
//                                .setExact(MatchField.ETH_DST, macAdd)
                                .setExact(MatchField.IN_PORT, inOFPort)
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
                                .setTableId(TableId.of(tableId))
                                .build();
                        ovsSwitch.write(allowNormalFlow);
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
                            .setTableId(TableId.of(tableId))
                            .build();

                    ovsSwitch.write(dropFlow);
                }
            }

//        } catch (FlowControllerException e) {
        } catch (Exception e) {
            //TODO:: Handle exceptions properly
            e.printStackTrace();
        }
        return Command.CONTINUE;
    }

    private int createNewTableEntryForIP(String ipAddress) {
        //TODO:: Need to remove the table Ids later
        int nextTableId = flowTableBits.nextClearBit(DEFAULT_FLOW_TABLE);
        flowTableBits.set(nextTableId);
        ipToTableIdMap.put(ipAddress, nextTableId);
        return nextTableId;
    }

//    private String getCustomerFromSubnet(IPv4Address srcIp) throws FlowControllerException {
//        String[] ipStringArr = srcIp.toString().split("\\" + PERIOD);
//        StringBuilder subnetString = new StringBuilder();
//        for (int i = 0; i < 3; i++) {
//            subnetString.append(ipStringArr[i]).append(PERIOD);
//        }
//        subnetString.append(ZERO);
//        String customer = subnetToCustomerMap.get(subnetString.toString());
//        if (customer == null) {
//            throw new FlowControllerException("Invalid subnet [" + subnetString + "]. " +
//                    "No customer found with matching subnet for IP: " + srcIp);
//        }
//        return customer;
//    }
//
//    private CustomerContainer[] getAdjacentContainers(CustomerContainer srcContainer,
//                                                      HashMap<String, CustomerContainer> containerList) {
//        CustomerContainer[] adjacentContainers = new CustomerContainer[ADJACENT_CONTAINERS];
//        String srcIp = srcContainer.getIpAddress();
//        int pipelineIndx = srcContainer.getPipeLineIndex();
//        for (CustomerContainer cusContainer : containerList.values()) {
//            if (!cusContainer.getIpAddress().equals(srcIp)) {
//                int pIndex = cusContainer.getPipeLineIndex();
//                if (pIndex == (pipelineIndx + 1)) {
//                    adjacentContainers[RIGHT_CONTAINER_INDX] = cusContainer;
//                } else if (pIndex == (pipelineIndx - 1)) {
//                    adjacentContainers[LEFT_CONTAINER_INDX] = cusContainer;
//                }
//            }
//        }
//        return adjacentContainers;
//    }

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