package net.floodlightcontroller.cienaflowcontroller;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.*;
import net.floodlightcontroller.util.OFMessageUtils;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.*;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
public class FlowController implements IOFMessageListener, IFloodlightModule {
    protected static Logger logger;
    private static final int MAX_TABLE_IDS = 256;

    protected IFloodlightProviderService floodlightProvider;
    private ConcurrentHashMap<String, Integer> ipToTableIdMap;
    private FlowRepository cienaFlowRepository;
    private BitSet flowTableBits;

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
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        OFPort inOFPort = OFMessageUtils.getInPort((OFPacketIn) msg);
        FlowControlsManager controlsManager = new FlowControlsManager(ovsSwitch, myFactory, eth, inOFPort);

        try {
            // If it is an ethernet frame
            if (eth.getEtherType() == EthType.IPv4) {
                MacAddress switchMac = ovsSwitch.getPort(OFPort.LOCAL).getHwAddr();
                IPv4 ipv4 = (IPv4) eth.getPayload();
                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();
                IPv4Address srcIp = ipv4.getSourceAddress();
                IPv4Address dstIp = ipv4.getDestinationAddress();

                // if it is a UDP Packet and its source is not the OVS SWITCH itself
//                if (ipv4.getProtocol() == IpProtocol.UDP && srcMac != switchMac) {
//                    controlsManager.processReadyStateUDP(cienaFlowRepository, ipv4);
//
//                } else {
                // if the incoming packet is between an Ingress (In or Out) and its neighbour
                if ((
                        cienaFlowRepository.isIngressContainerIp(srcIp.toString()) &&
                                cienaFlowRepository.isNeighbourOfIngress(dstIp.toString())
                ) || (
                        cienaFlowRepository.isIngressContainerIp(dstIp.toString()) &&
                                cienaFlowRepository.isNeighbourOfIngress(srcIp.toString()))) {
                    controlsManager.addAllowIngressToNeighbourFlow();
                    return Command.CONTINUE;
                    // if the incoming packet is [TO] or [FROM] the OVS
                } else if (srcMac.toString().equals(switchMac.toString()) ||
                        dstMac.toString().equals(switchMac.toString())) {
                    controlsManager.addAllowFlowsToAndFromOVS();
                    return Command.CONTINUE;
                }
//                    } else {
                List<IPv4Address> neighbours = cienaFlowRepository.getNeighbourIps(srcIp);
                Integer tableId = ipToTableIdMap.get(srcIp.toString());
                if (tableId == null) {
                    tableId = createNewTableEntryForIP(srcIp.toString());
                }
                controlsManager.gotoContainerSpecificFlowTable(tableId);
                controlsManager.addAllowFlowToNeighbours(srcIp, tableId, neighbours);
//                        controlsManager.allowUDPFlowsToOVS(tableId);
                controlsManager.dropAllOtherFlows(tableId);
//                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Command.CONTINUE;
    }

    private int createNewTableEntryForIP(String ipAddress) {
        //TODO:: Need to remove the table Ids later
        int nextTableId = flowTableBits.nextClearBit(DEFAULT_FLOW_TABLE);
        logger.info("New Table Id " + nextTableId + " for IP " + ipAddress);
        flowTableBits.set(nextTableId);
        ipToTableIdMap.put(ipAddress, nextTableId);
        return nextTableId;
    }

    private void printDebugValues(Ethernet eth) {
        IPv4 ipv4 = (IPv4) eth.getPayload();
        MacAddress srcMac = eth.getSourceMACAddress();
        MacAddress dstMac = eth.getDestinationMACAddress();
        IPv4Address srcIp = ipv4.getSourceAddress();
        IPv4Address dstIp = ipv4.getDestinationAddress();

//        logger.info("########## OVS-SWITCH SOCKET ADD: " + ovsSocketAddress);
//        logger.info("########## OVS-SWITCH INET ADDR: " + inetAddr);
//        logger.info("########## OVS-SWITCH IPv4 ADDR: " + ovsIpv4);
//        logger.info("########## TABLE-ID: " + inOFPort);
//        logger.info("########## SWITCH-MAC: " + switchMac.toString());

        logger.info("################ SOURCE: {} seen with IP: {}", srcMac, srcIp);
        logger.info("################ DESTINATION: {} seen with IP: {}", dstMac, dstIp);
        logger.info("----------------------------------------------------------------");
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