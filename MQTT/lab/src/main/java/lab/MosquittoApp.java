package lab;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MosquittoApp {
    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID1 = "JavaMQTTClient1";
    private static final String CLIENT_ID2 = "JavaMQTTClient2";
    private static final String WED_TOPIC = "WED/readings";
    private static final String ESP_TOPIC = "ESP/readings";
    private static final long ONE_MINUTE = 60 * 1000; // Milisekunde

    private static Map<String, Integer> espReadings = new HashMap<>();
    private static Map<String, Integer> wedReadings = new HashMap<>();
    
    private static Queue<Long> receivedTimes = new LinkedList<>();

    public static void main(String[] args) {
        try {
        	MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(BROKER, CLIENT_ID1, persistence);
            
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Connecting to broker: " + BROKER);
            client.connect(connOpts);
            System.out.println("Connected");
            
            client.subscribe(WED_TOPIC);
            client.subscribe(ESP_TOPIC);
            
            startTimer();

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
                    processMessage(topic, mqttMessage);
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

    private static void processMessage(String topic, MqttMessage mqttMessage) {
        if (topic.equals(WED_TOPIC)) {
            // Process message from WED device
            System.out.println("Message from WED device");

            // Split message to get readings
            byte[] payload = mqttMessage.getPayload();
            String message = new String(payload, StandardCharsets.UTF_8);
            String[] parts = message.split(",");
            int temp = Integer.parseInt(parts[0]);
            int humid = Integer.parseInt(parts[1]);
            int press = Integer.parseInt(parts[2]);
            int value = Integer.parseInt(parts[4]);

            // If motion is detected, send action message to ESP device
            if (value == 1) {
            	// Save readings to database or object
                saveReadings("WED", value, temp, humid, press);
                // Send action message to ESP device
                sendActionMessage("ESP");
            }

        } else if (topic.equals(ESP_TOPIC)) {
        	// Add current time to the received times queue
            receivedTimes.add(System.currentTimeMillis());
            
            // Process message from ESP device
            System.out.println("Message from ESP device");

            // Split message to get readings
            byte[] payload = mqttMessage.getPayload();
            String message = new String(payload, StandardCharsets.UTF_8);
            int value = Integer.parseInt(message);

            // Save readings to database or object
            saveReadings("ESP", value);

            // If motion is detected and numReadings is greater than 2, send action message to WED device
            if (value == 1 && receivedTimes.size() > 2) {
                // Send action message to WED device
                sendActionMessage("WED");
            }
        }
    }

    private static void saveReadings(String device, int value, int... values) {
        if (device.equals("WED")) {
            wedReadings.put("Value", value);
            wedReadings.put("Temperature", values[0]);
            wedReadings.put("Humidity", values[1]);
            wedReadings.put("Pressure", values[2]);
        } else if (device.equals("ESP")) {
            espReadings.put("Value", value);
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
            client = new MqttClient(BROKER, CLIENT_ID2);
            client.connect();
            String topic = device.equals("WED") ? "WED/actions" : "ESP/actions";
            String message = "Action required for " + device + " device";
            client.publish(topic, new MqttMessage(message.getBytes()));
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    private static void startTimer() {
        // Create and start a timer task to clear old times every minute
        new java.util.Timer().scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                // Remove times older than one minute
                while (!receivedTimes.isEmpty() && currentTime - receivedTimes.peek() > ONE_MINUTE) {
                    receivedTimes.remove();
                }
            }
        }, 0, ONE_MINUTE);
    }
}
