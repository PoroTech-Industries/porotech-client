package com.porotech_industries.porocar;

/**
 * Hello world!
 */
import com.porotech_industries.porocar.datatransfer.mqtt.*;
import com.porotech_industries.porocar.datatransfer.serial.*;
import com.porotech_industries.porocar.datatransfer.webrtc.WebRtcServiceManager;
import com.porotech_industries.porocar.utils.parser.PoroArduinoParser;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.porotech_industries.porocar.utils.logger.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class  App {
    public static void main(String[] args) throws MqttException, InterruptedException, IOException {
		WebRtcServiceManager webrtc = new WebRtcServiceManager();
		webrtc.startPythonWebRtcService();

		Runtime.getRuntime().addShutdownHook(new Thread(webrtc::stopPythonService));
		PoroLogger logger = new PoroLogger();
		PoroSerialReceiver serialReceiver = new PoroSerialReceiver();



		logger.enable();
		logger.setLogLevel(PoroLogger.LogLevel.INFO);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			boolean healthy = webrtc.isHealthy();
			PoroLogger.info("WebRTC", (healthy ? "✅ OK" : "❌ DOWN"));
		}, 0, 10, TimeUnit.SECONDS);
		System.out.println("Hello World!");
		String broker = "tcp://10.93.133.78:1883";
		String TOPIC = "test/1";


		serialReceiver.listSerialPorts();

		if (!serialReceiver.connect("ttyACM0", 115200, 100)) {
			PoroLogger.error("SerialReceiver", "Failed to connect, dying now");
			//System.exit(69420);
		} else {
			  PoroLogger.info("YAY", "MORE YAY");
		}

		//serialReceiver.receiveData(1000);

		PoroLogger.info("Test", "this is a test msg");

		PoroMqttClient subscriber = new PoroMqttClient(broker);
		subscriber.connect();

		PoroMqttClient publisher = new PoroMqttClient(broker);
		publisher.connect();

		PoroMqttClient publisher2 = new PoroMqttClient(broker);
		publisher2.connect();

		subscriber.subscribe("poroCar/Lidar", 1, (topic, message) -> {
			PoroLogger.info("MQTT Received", "Topic: %s, Content: %s, QoS: %d", topic, new String(message.getPayload()), message.getQos());
		});

		String testMessage = "Hello from PoroCar!";
		publisher.publish(TOPIC, testMessage.getBytes(), 1, false);

		Thread.sleep(2000);


		publisher.publish(TOPIC, testMessage.getBytes(), 1, false);

		//publisher.disconnect();

		publisher2.publish("PoroCar/Stream/LD06/send", "im still here".getBytes(), 1, false);
		Thread.sleep(2000);

		//subscriber.disconnect();
		publisher2.disconnect();
		while (true) {
			String data = serialReceiver.receiveData(1000);
			if (Objects.equals(data, "")) {
				continue;
			}
			HashMap<String, String> parsedData = PoroArduinoParser.parseArduinoData(data);

			for (String topic : parsedData.keySet()) {
				PoroLogger.info("Parsed Arduino Data", "key: %s, value %s", topic, parsedData.get(topic));
				publisher.publish(topic, parsedData.get(topic).getBytes(), 1, false);
			}
			//System.exit(-1);
		}
	}
		//serialReceiver.close();
}
