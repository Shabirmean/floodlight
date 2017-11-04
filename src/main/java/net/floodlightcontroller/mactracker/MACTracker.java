package net.floodlightcontroller.mactracker;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


public class MACTracker implements IOFMessageListener, IFloodlightModule, IOFSwitchListener {
//public class MACTracker implements IDeviceListener, IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener, IL3Routing {
    private static final String OVS_BRIDGE = "ciena-bridge";
    private static long SWITCH_DPID;    // 00000255be34334e
    protected IFloodlightProviderService floodlightProvider;
    protected Set<Long> macAddresses;
    protected static Logger logger;
    protected FlowModifier flowModifier;

    @Override
    public String getName() {
        return MACTracker.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(IFloodlightProviderService.class);
        return l;
    }


//    ca:70:c4:1a:d7:28 - 192.168.1.1
//    ea:73:aa:1e:64:13 - 192.168.1.2
//    d6:f0:14:70:f7:5b - 192.168.1.3
//    66:c4:a7:42:04:b7 - 192.168.1.4
//    4e:c8:0c:c7:e8:c9 - 192.168.1.5
//    e6:1d:c8:fe:c9:ad - 192.168.1.6

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        logger = LoggerFactory.getLogger(MACTracker.class);
        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.macAddresses = new ConcurrentSkipListSet<Long>();
        this.flowModifier = new FlowModifier(OVS_BRIDGE);
        this.flowModifier.init();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        this.floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        logger.info("==================== This is registered and is working as expected.");
    }

    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        OFFactory myFactory = sw.getOFFactory();
        OFVersion ofVersion = myFactory.getVersion();
        System.out.println(ofVersion.toString());


        Match myMatch = myFactory.buildMatch()
                .setExact(MatchField.IN_PORT, OFPort.of(1))
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, IPv4Address.of("192.168.1.1"))
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.IP_PROTO, IpProtocol.UDP)
                .setExact(MatchField.TCP_DST, TransportPort.of(0))
                .build();

//        OFActions allActions = myFactory.actions();

        OFInstructions instructions = myFactory.instructions();
        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFOxms oxms = myFactory.oxms();


        OFActionSetField setDlDst = actions.buildSetField()
                .setField(oxms.buildEthDst().setValue(MacAddress.of("2a:74:6c:22:0f:96")).build()).build();
        actionList.add(setDlDst);

        OFActionSetField setNwDst = actions.buildSetField()
                .setField(oxms.buildIpv4Dst().setValue(IPv4Address.of("192.168.1.3")).build()).build();
        actionList.add(setNwDst);

        OFActionPopVlan popVlan = actions.popVlan();
        actionList.add(popVlan);

        OFActionOutput output = actions.buildOutput().setMaxLen(0xFFffFFff).setPort(OFPort.of(3)).build();
        actionList.add(output);

        OFInstructionApplyActions applyActions = instructions.buildApplyActions().setActions(actionList).build();
        ArrayList<OFInstruction> instructionList = new ArrayList<OFInstruction>();
        instructionList.add(applyActions);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(3600)
                .setIdleTimeout(10)
                .setPriority(32768)
                .setMatch(myMatch)
                .setInstructions(instructionList)
                .setTableId(TableId.of(3))
                .build();

        sw.write(flowAdd);

        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        logger.info("++++++ A message from " + eth.getSourceMACAddress().toString());
        Long sourceMACHash = eth.getSourceMACAddress().getLong();
        if (!macAddresses.contains(sourceMACHash)) {
            macAddresses.add(sourceMACHash);
            logger.info("MAC Address: {} seen on switch: {}",
                    eth.getSourceMACAddress().toString(), sw.getId().toString());
        }
        return Command.CONTINUE;
    }

    @Override
    public void switchAdded(DatapathId switchId) {
        this.SWITCH_DPID = switchId.getLong();
        logger.info("[Switch DPID] : " + SWITCH_DPID);
    }

    public static long getSwitchDpid() {
        return SWITCH_DPID;
    }

    @Override
    public void switchRemoved(DatapathId switchId) {

    }

    @Override
    public void switchActivated(DatapathId switchId) {

    }

    @Override
    public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {

    }

    @Override
    public void switchChanged(DatapathId switchId) {

    }

    @Override
    public void switchDeactivated(DatapathId switchId) {

    }
}
