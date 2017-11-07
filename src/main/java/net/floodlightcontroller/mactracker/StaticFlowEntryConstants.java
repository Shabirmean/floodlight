package net.floodlightcontroller.mactracker;


public class StaticFlowEntryConstants {

    public static final String CIENA_SWITCH_ID = "00:00:d6:ed:a6:a2:0c:44";
    public static final String HIGHEST_PRIORITY = "32768";
    public static final String NEUTRAL_PRIORITY = "0";
    public static final String IP_ETHERNET = "0x0800";
    public static final String ACTIVE_TRUE = "true";
    public static final String ACTIVE_FALSE = "false";

    public static final String SWITCH = "switch";
    public static final String FLOW_NAME = "name";
    public static final String TABLE = "table";
    public static final String PRIORITY = "priority";
    public static final String ACTIVE = "active";
    public static final String IN_PORT = "in_port";
    public static final String SRC_MAC = "eth_src";
    public static final String DST_MAC = "eth_dst";
    public static final String SRC_IP = "ipv4_src";
    public static final String DST_IP = "ipv4_dst";
    public static final String ETH_TYPE = "eth_type";
    public static final String ACTIONS = "actions";
    public static final String OUTPUT_NORMAL = "output=normal";
    public static final String GOTO_TABLE = "instruction_goto_table";

    public static final String GOTO_C1_TABLE = "Flow-C1";
    public static final String GOTO_C2_TABLE = "Flow-C2";
    public static final String GOTO_C3_TABLE = "Flow-C3";
    public static final String GOTO_C4_TABLE = "Flow-C4";
    public static final String GOTO_C5_TABLE = "Flow-C5";
    public static final String GOTO_C6_TABLE = "Flow-C6";

    public static final String FLOW_C1_DROP = "Flow-C1-Drop";
    public static final String FLOW_C2_DROP = "Flow-C2-Drop";
    public static final String FLOW_C3_DROP = "Flow-C3-Drop";
    public static final String FLOW_C4_DROP = "Flow-C4-Drop";
    public static final String FLOW_C5_DROP = "Flow-C5-Drop";
    public static final String FLOW_C6_DROP = "Flow-C6-Drop";

    public static final String FLOW_C1_C2 = "Flow-C1-C2";
    public static final String FLOW_C2_C1 = "Flow-C2-C1";
    public static final String FLOW_C2_C3 = "Flow-C2-C3";
    public static final String FLOW_C3_C2 = "Flow-C3-C2";

    public static final String FLOW_C4_C5 = "Flow-C4-C5";
    public static final String FLOW_C5_C4 = "Flow-C5-C4";
    public static final String FLOW_C5_C6 = "Flow-C5-C6";
    public static final String FLOW_C6_C5 = "Flow-C6-C5";

    public static final String NORMAL_FLOW = "Flow-Normal";

    public static final String GOTO_TABLE_FLOW = "{\"" + SWITCH + "\":\"%s\",\"" + FLOW_NAME + "\":\"%s\"," +
            "\"" + PRIORITY + "\":\"%s\",\"" + IN_PORT + "\":\"%s\",\"" + ACTIVE + "\":\"%s\", \"" + ETH_TYPE +
            "\":\"%s\",\"" + SRC_MAC + "\":\"%s\", \"" + SRC_IP + "\":\"%s\", \"" + GOTO_TABLE + "\":\"%s\"}";

    public static final String DROP_PACKET_FLOW = "{\"" + SWITCH + "\":\"%s\",\"" + FLOW_NAME + "\":\"%s\"," +
            "\"" + TABLE + "\":\"%s\",\"" + PRIORITY + "\":\"%s\",\"" + IN_PORT + "\":\"%s\",\"" + ACTIVE +
            "\":\"%s\",\"" + ETH_TYPE + "\":\"%s\",\"" + SRC_MAC + "\":\"%s\", \"" + SRC_IP + "\":\"%s\", \"" +
            ACTIONS + "\":\"\"}";

    public static final String ALLOW_FLOW = "{\"" + SWITCH + "\":\"%s\",\"" + FLOW_NAME + "\":\"%s\",\"" + TABLE +
            "\":\"%s\",\"" + PRIORITY + "\":\"%s\",\"" + IN_PORT + "\":\"%s\",\"" + ACTIVE + "\":\"%s\",\"" +
            ETH_TYPE + "\":\"%s\",\"" + DST_MAC + "\":\"%s\",\"" + DST_IP + "\":\"%s\", \"" + ACTIONS + "\":\"%s\"}";

    public static final String NORMAL_PFLOW = "{\"" + SWITCH + "\":\"%s\",\"" + FLOW_NAME + "\":\"%s\",\"" + TABLE +
            "\":\"%s\",\"" + PRIORITY + "\":\"%s\",\"" + ACTIVE + "\":\"%s\",\"" + ACTIONS + "\":\"%s\"}";

    public enum FLOW_TABLE {
        ZERO("0"),
        ONE("1"),
        TWO("2"),
        THREE("3"),
        FOUR("4"),
        FIVE("5"),
        SIX("6");

        private String fTable;

        FLOW_TABLE(String fTable) {
            this.fTable = fTable;
        }

        public String getFTable() {
            return fTable;
        }
    }

    public enum CONTAINER_MAC {
        C1("1e:c7:d9:4c:cb:7d"),
        C2("62:a4:25:4c:7b:a0"),
        C3("2a:74:6c:22:0f:96"),
        C4("86:99:92:82:ee:4e"),
        C5("d6:14:16:72:e1:4b"),
        C6("7a:a5:20:d0:79:9f");

        private String mac_address;

        CONTAINER_MAC(String mac_address) {
            this.mac_address = mac_address;
        }

        public String getMAC() {
            return mac_address;
        }
    }

    public enum CONTAINER_IP {
        C1("192.168.1.1"),
        C2("192.168.1.2"),
        C3("192.168.1.3"),
        C4("192.168.1.4"),
        C5("192.168.1.5"),
        C6("192.168.1.6");

        private String container_ip;

        CONTAINER_IP(String container_ip) {
            this.container_ip = container_ip;
        }

        public String getIP() {
            return container_ip;
        }
    }
}
