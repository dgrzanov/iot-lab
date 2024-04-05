package myFirstXbeeApp;


import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.utils.HexUtils;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class DataReceiveListener implements IDataReceiveListener {
	
	private static final int QUALITY_OF_SERVICE = 2;
    private static final String BROKER = "";
    private static final String CLIENT_ID = "";
    private static final String TOPIC_ACTUATION = "";

    private MqttClient mqttClient;
	/*
	 * (non-Javadoc)
	 * @see com.digi.xbee.api.listeners.IDataReceiveListener#dataReceived(com.digi.xbee.api.models.XBeeMessage)
	 */
	@Override
	public void dataReceived(XBeeMessage xbeeMessage) {
		System.out.format("From %s >> %s | %s%n", xbeeMessage.getDevice().get64BitAddress(),
                HexUtils.prettyHexString(HexUtils.byteArrayToHexString(xbeeMessage.getData())),
                new String(xbeeMessage.getData()));

        String content = new String(xbeeMessage.getData());
        try {
            connectToMQTT();

            String acceleration = getSensorValue(content, "ACC");
            String temperature = getSensorValue(content, "TC");
            String humidity = getSensorValue(content, "HUM");

            sendMessage(acceleration, "");
            sendMessage(temperature, "");
            sendMessage(humidity, "");

            disconnectFromMQTT();
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
	}
	private void connectToMQTT() throws MqttException {
        mqttClient = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        mqttClient.connect(connOpts);
        mqttClient.subscribe(TOPIC_ACTUATION, QUALITY_OF_SERVICE);
    }

    private void disconnectFromMQTT() throws MqttException {
        mqttClient.disconnect();
    }

    private void sendMessage(String message, String topic) throws MqttException {
        MqttMessage msg = new MqttMessage(message.getBytes());
        msg.setQos(QUALITY_OF_SERVICE);
        mqttClient.publish(topic, msg);
    }

    private String getSensorValue(String inputString, String sensorName) {
        System.out.println("String from input is -->" + inputString);
        int startIndex = inputString.indexOf(sensorName) + sensorName.length() + 1;
        int endIndex = inputString.indexOf("#", startIndex);
        String valueString = inputString.substring(startIndex, endIndex);
        System.out.println("getSensorValue:" + valueString);
        return valueString;
    }
}

