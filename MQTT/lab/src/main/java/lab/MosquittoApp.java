package lab;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class MosquittoApp {
    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID = "JavaMQTTClient";
    private static final String WED_TOPIC = "WED/readings";
    private static final String ESP_TOPIC = "ESP/readings";

    private static Map<String, Integer> espReadings = new HashMap<>();
    private static Map<String, Integer> wedReadings = new HashMap<>();

    public static void main(String[] args) {
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Connecting to broker: " + BROKER);
            client.connect(connOpts);
            System.out.println("Connected");

            client.subscribe(WED_TOPIC);
            client.subscribe(ESP_TOPIC);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println("Connection to MQTT broker lost!");
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    System.out.println("Message received:");
                    System.out.println("Topic: " + topic);
                    System.out.println("Message: " + mqttMessage.toString());

                    // Process received message
                    processMessage(topic, mqttMessage.toString());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    // Not used in this example
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private static void processMessage(String topic, String message) {
        if (topic.equals(WED_TOPIC)) {
            // Process message from WED device
            System.out.println("Message from WED device");

            // Split message to get readings
            String[] parts = message.split(",");
            int motion = Integer.parseInt(parts[0]);
            int temperature = Integer.parseInt(parts[1]);
            int humidity = Integer.parseInt(parts[2]);

            // Save readings to database or object
            saveReadings("WED", motion, temperature, humidity);

            // If motion is detected, send action message to ESP device
            if (motion == 1) {
                // Send action message to ESP device
                sendActionMessage("ESP");
            }

        } else if (topic.equals(ESP_TOPIC)) {
            // Process message from ESP device
            System.out.println("Message from ESP device");

            // Split message to get readings
            String[] parts = message.split(",");
            int motion = Integer.parseInt(parts[0]);
            int numReadings = Integer.parseInt(parts[1]);

            // Save readings to database or object
            saveReadings("ESP", motion, numReadings);

            // If motion is detected and numReadings is greater than 2, send action message to WED device
            if (motion == 1 && numReadings > 2) {
                // Send action message to WED device
                sendActionMessage("WED");
            }
        }
    }

    private static void saveReadings(String device, int motion, int... values) {
        if (device.equals("WED")) {
            wedReadings.put("Motion", motion);
            wedReadings.put("Temperature", values[0]);
            wedReadings.put("Humidity", values[1]);
        } else if (device.equals("ESP")) {
            espReadings.put("Motion", motion);
            espReadings.put("NumReadings", values[0]);
        }

        // Save readings to database or object
        // Example:
        // Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        // System.out.println("Timestamp: " + timestamp);
        // System.out.println(device + " Readings: " + readings);
    }

    private static void sendActionMessage(String device) {
        MqttClient client;
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            String topic = device.equals("WED") ? WED_TOPIC : ESP_TOPIC;
            String message = "Action required for " + device + " device";
            client.publish(topic, new MqttMessage(message.getBytes()));
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
