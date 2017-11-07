package net.floodlightcontroller.mactracker;

public class StaticFlowEntries {
    //TODO::Flow Entries for Specific Flow Tables
    public static final String GOTO_TABLE_1 = String.format(
            StaticFlowEntryConstants.GOTO_TABLE_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.GOTO_C1_TABLE, StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C1.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C1.getIP(), StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable());

    public static final String GOTO_TABLE_2 = String.format(
            StaticFlowEntryConstants.GOTO_TABLE_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.GOTO_C2_TABLE, StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C2.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C2.getIP(), StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable());

    public static final String GOTO_TABLE_3 = String.format(
            StaticFlowEntryConstants.GOTO_TABLE_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.GOTO_C3_TABLE, StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.THREE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C3.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C3.getIP(), StaticFlowEntryConstants.FLOW_TABLE.THREE.getFTable());

    public static final String GOTO_TABLE_4 = String.format(
            StaticFlowEntryConstants.GOTO_TABLE_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.GOTO_C4_TABLE, StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.FOUR.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C4.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C4.getIP(), StaticFlowEntryConstants.FLOW_TABLE.FOUR.getFTable());

    public static final String GOTO_TABLE_5 = String.format(
            StaticFlowEntryConstants.GOTO_TABLE_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.GOTO_C5_TABLE, StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C5.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C5.getIP(), StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable());

    public static final String GOTO_TABLE_6 = String.format(
            StaticFlowEntryConstants.GOTO_TABLE_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.GOTO_C6_TABLE, StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.SIX.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C6.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C6.getIP(), StaticFlowEntryConstants.FLOW_TABLE.SIX.getFTable());

    //TODO::Flow Entries for Drop Rules
    public static final String DROP_AT_T1 = String.format(
            StaticFlowEntryConstants.DROP_PACKET_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.FLOW_C1_DROP, StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable(),
            (Integer.parseInt(StaticFlowEntryConstants.HIGHEST_PRIORITY) - 2),
            StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C1.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C1.getIP());

    public static final String DROP_AT_T2 = String.format(
            StaticFlowEntryConstants.DROP_PACKET_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.FLOW_C2_DROP, StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(),
            (Integer.parseInt(StaticFlowEntryConstants.HIGHEST_PRIORITY) - 2),
            StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C2.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C2.getIP());

    public static final String DROP_AT_T3 = String.format(
            StaticFlowEntryConstants.DROP_PACKET_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.FLOW_C3_DROP, StaticFlowEntryConstants.FLOW_TABLE.THREE.getFTable(),
            (Integer.parseInt(StaticFlowEntryConstants.HIGHEST_PRIORITY) - 2),
            StaticFlowEntryConstants.FLOW_TABLE.THREE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C3.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C3.getIP());

    public static final String DROP_AT_T4 = String.format(
            StaticFlowEntryConstants.DROP_PACKET_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.FLOW_C4_DROP, StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable(),
            (Integer.parseInt(StaticFlowEntryConstants.HIGHEST_PRIORITY) - 2),
            StaticFlowEntryConstants.FLOW_TABLE.FOUR.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C4.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C4.getIP());

    public static final String DROP_AT_T5 = String.format(
            StaticFlowEntryConstants.DROP_PACKET_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.FLOW_C5_DROP, StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(),
            (Integer.parseInt(StaticFlowEntryConstants.HIGHEST_PRIORITY) - 2),
            StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C5.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C5.getIP());

    public static final String DROP_AT_T6 = String.format(
            StaticFlowEntryConstants.DROP_PACKET_FLOW, StaticFlowEntryConstants.CIENA_SWITCH_ID,
            StaticFlowEntryConstants.FLOW_C6_DROP, StaticFlowEntryConstants.FLOW_TABLE.SIX.getFTable(),
            (Integer.parseInt(StaticFlowEntryConstants.HIGHEST_PRIORITY) - 2),
            StaticFlowEntryConstants.FLOW_TABLE.SIX.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C6.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C6.getIP());

    //TODO::Flow Entries for Customer 1
    public static final String ALLOW_C1_C2 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C1_C2,
            StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.ONE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C2.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C2.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    public static final String ALLOW_C2_C1 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C2_C1,
            StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C1.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C1.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    public static final String ALLOW_C2_C3 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C2_C3,
            StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.TWO.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C3.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C3.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    public static final String ALLOW_C3_C2 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C3_C2,
            StaticFlowEntryConstants.FLOW_TABLE.THREE.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.THREE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C2.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C2.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    //TODO::Flow Entries for Customer 2

    public static final String ALLOW_C4_C5 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C4_C5,
            StaticFlowEntryConstants.FLOW_TABLE.FOUR.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.FOUR.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C5.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C5.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    public static final String ALLOW_C5_C4 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C5_C4,
            StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C4.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C4.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    public static final String ALLOW_C5_C6 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C5_C6,
            StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.FIVE.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C6.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C6.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    public static final String ALLOW_C6_C5 = String.format(StaticFlowEntryConstants.ALLOW_FLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.FLOW_C6_C5,
            StaticFlowEntryConstants.FLOW_TABLE.SIX.getFTable(), StaticFlowEntryConstants.HIGHEST_PRIORITY,
            StaticFlowEntryConstants.FLOW_TABLE.SIX.getFTable(), StaticFlowEntryConstants.ACTIVE_TRUE,
            StaticFlowEntryConstants.IP_ETHERNET, StaticFlowEntryConstants.CONTAINER_MAC.C5.getMAC(),
            StaticFlowEntryConstants.CONTAINER_IP.C5.getIP(), StaticFlowEntryConstants.OUTPUT_NORMAL);

    //TODO::Flow Entries for normal flow
    public static final String NORMAL_FLOW_MODE = String.format(
            StaticFlowEntryConstants.NORMAL_PFLOW,
            StaticFlowEntryConstants.CIENA_SWITCH_ID, StaticFlowEntryConstants.NORMAL_FLOW,
            StaticFlowEntryConstants.FLOW_TABLE.ZERO.getFTable(), StaticFlowEntryConstants.NEUTRAL_PRIORITY,
            StaticFlowEntryConstants.ACTIVE_TRUE, StaticFlowEntryConstants.OUTPUT_NORMAL);
}
