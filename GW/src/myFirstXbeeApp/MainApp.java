package myFirstXbeeApp;

import com.digi.xbee.api.XBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;

public class MainApp {
	
	private static final String PORT = "COM3";
	private static final int BAUD_RATE = 115200;


	public static void main(String[] args) {
		System.out.println(" +---------+");
		System.out.println(" |  XBee   |");
		System.out.println(" +---------+\n");
		
		XBeeDevice myDevice = new XBeeDevice(PORT, BAUD_RATE);
		
		try {
			myDevice.open();
			
			myDevice.addDataListener(new DataReceiveListener());
			
			System.out.println("\n>> Waiting for data...");
			
		} catch (XBeeException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

