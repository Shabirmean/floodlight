package net.floodlightcontroller.cienaflowcontroller.controller;

import net.floodlightcontroller.cienaflowcontroller.datahandler.FlowRepository;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shabirmean on 2017-11-29 with some hope.
 */
@SuppressWarnings("ALL")
public class MqttListener {
    private static Logger logger;
    private MqttClient mqttClient;
    private String mqttBrokerURI;
    private String mqttSubscribeTopic;

    MqttListener(String brokerURI, String mqttSubscribeTopic) {
        this.mqttBrokerURI = brokerURI;
        this.mqttSubscribeTopic = mqttSubscribeTopic;
    }

    public void init(FlowRepository cienaFlowRepository) {
        logger = LoggerFactory.getLogger(MqttListener.class);
        Runnable subscriber = () -> {
            try {
                String clientId = MqttClient.generateClientId();
                mqttClient = new MqttClient(mqttBrokerURI, clientId, new MemoryPersistence());
                mqttClient.setCallback(cienaFlowRepository);
            } catch (MqttException e) {
                e.printStackTrace();
            }

            while (!mqttClient.isConnected()) {
                try {
                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
                    mqttClient.connect(options);

                    if (mqttClient.isConnected()) {
                        logger.info("############# STARTED MQTT Listener...");
                        logger.info("############# Subscribing to : " + mqttSubscribeTopic);
                        mqttClient.subscribe(mqttSubscribeTopic, 0);
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    logger.error("MQTT Connect-Thread Sleep Interrupt Exception.");
                    ex.printStackTrace();
                }
            }
        };

        Thread subscriberThread = new Thread(subscriber);
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }
}
