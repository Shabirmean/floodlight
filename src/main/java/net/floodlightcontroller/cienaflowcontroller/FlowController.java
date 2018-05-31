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
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.MQTT_BROKER_URI;
import static net.floodlightcontroller.cienaflowcontroller.FlowControllerConstants.MQTT_SUBSCRIBE_TOPIC;
import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
public class FlowController implements IOFMessageListener, IFloodlightModule {
    protected static Logger logger;
    protected IFloodlightProviderService floodlightProvider;
    private FlowRepository cienaFlowRepository;

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
        this.cienaFlowRepository = new FlowRepository();
        MqttListener mqttListener = new MqttListener(MQTT_BROKER_URI, MQTT_SUBSCRIBE_TOPIC);
        mqttListener.init(cienaFlowRepository);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        logger.debug("########################### : " + "CIENA FLOW CONTROLLER IS REGISTERED.");
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
        FlowControlSetupManager controlsManager = new FlowControlSetupManager(ovsSwitch, myFactory, eth, inOFPort);

        try {
            // If it is an ethernet frame
            if (eth.getEtherType() == EthType.IPv4) {
                MacAddress switchMac = ovsSwitch.getPort(OFPort.LOCAL).getHwAddr();
                IPv4 ipv4 = (IPv4) eth.getPayload();
                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();
                IPv4Address srcIp = ipv4.getSourceAddress();
                IPv4Address dstIp = ipv4.getDestinationAddress();
                cienaFlowRepository.addInPortForIp(srcIp.toString(), inOFPort);
                logger.info("\n\n##### Adding Port: [" + inOFPort.getPortNumber() + "] for " +
                        "IP [" + srcIp.toString() + "]\n\n ");

                // if it is a UDP Packet and its source is not the OVS SWITCH itself
                if (ipv4.getProtocol() == IpProtocol.UDP && srcMac != switchMac) {
                    if (cienaFlowRepository.isIngressContainerIp(srcIp.toString())) {
                        // if UDP packet from an ingress containers then notify end state
                        String srcIpString = srcIp.toString();
                        int tableId = cienaFlowRepository.getIpToTableIdMap().get(srcIpString);

                        // TODO:: Strip down OVS flow controls
                        FlowControlRemover fcRem = new FlowControlRemover(ovsSwitch, myFactory, eth, inOFPort);
                        fcRem.processEventStatusUDP(cienaFlowRepository, tableId);

                    } else {
                        // if UDP packet from any intermediary containers then update ready state
                        boolean validUDPPacket = controlsManager.processReadyStateUDP(cienaFlowRepository);
                        if (validUDPPacket) {
                            setupContainerSpecificTableEntries(controlsManager, srcIp);
                        }
                    }
                } else {
                    // if the incoming packet is between an Ingress (In or Out) and its neighbour
                    if ((
                            cienaFlowRepository.isIngressContainerIp(srcIp.toString()) &&
                                    cienaFlowRepository.isNeighbourOfIngress(dstIp.toString())
                    ) || (
                            cienaFlowRepository.isIngressContainerIp(dstIp.toString()) &&
                                    cienaFlowRepository.isNeighbourOfIngress(srcIp.toString()))) {
                        controlsManager.addAllowIngressToNeighbourFlow();
                        // if the incoming packet is [TO] or [FROM] the OVS
                    } else if (srcMac.toString().equals(switchMac.toString()) ||
                            dstMac.toString().equals(switchMac.toString())) {
                        controlsManager.addAllowFlowsToAndFromOVS();

                    } else {
                        setupContainerSpecificTableEntries(controlsManager, srcIp);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Command.CONTINUE;
    }

    private void setupContainerSpecificTableEntries(FlowControlSetupManager controlsManager, IPv4Address srcIp) {
        int tableId = cienaFlowRepository.getFlowTableId(srcIp);
        List<IPv4Address> neighbours = cienaFlowRepository.getNeighbourIps(srcIp);
        controlsManager.gotoContainerSpecificFlowTable(tableId);
        controlsManager.addAllowFlowToNeighbours(srcIp, tableId, neighbours);
        controlsManager.allowUDPFlowsToOVS(tableId);
        controlsManager.dropAllOtherFlows(tableId);
    }

    static void respondToContainerManager(String topic, String responseToCM) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            String clientId = MqttClient.generateClientId();
            MqttClient mqttPublisherClient = new MqttClient(MQTT_BROKER_URI, clientId, new MemoryPersistence());
            mqttPublisherClient.connect(options);
            mqttPublisherClient.publish(topic, responseToCM.getBytes(UTF_8), 2, false);
            mqttPublisherClient.disconnect();
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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