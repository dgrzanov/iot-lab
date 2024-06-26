/*
    ------ Waspmote Pro Code Example --------

    Explanation: This is the basic Code for Waspmote Pro

    Copyright (C) 2016 Libelium Comunicaciones Distribuidas S.L.
    http://www.libelium.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// Put your libraries here (#include ...)
#include <WaspSensorEvent_v30.h>
#include <WaspXBeeZB.h>
#include <WaspFrame.h>

float acc;
float temp;
float humid;
float pres;
float value;


// Destination MAC address
//////////////////////////////////////////
char GW_ADDRESS[] = "0013A20040F8DC76";
//////////////////////////////////////////
uint8_t  PANID[8] = {0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,  0x22};


// Define the Waspmote ID
char WASPMOTE_ID[] = "node_temp";


// define variable
uint8_t error;
uint8_t send_error;

void setup()
{
  // put your setup code here, to run once:
  // Turn on the USB and print a start message
  USB.ON();
  USB.println(F("Start program"));

  // store Waspmote identifier in EEPROM memory
  frame.setID( WASPMOTE_ID );
  
  // init XBee
  xbeeZB.ON();
  
  xbeeZB.setPAN(PANID);

  // check at command flag
  if (xbeeZB.error_AT == 0)
  {
    USB.println(F("2. PANID set OK"));
  }
  else
  {
    USB.println(F("2. Error while setting PANID"));
  }

  xbeeZB.setScanningChannels(0x00, 0x02);

  // check at command flag
  
  
  if (xbeeZB.error_AT == 0)
  {
    USB.println(F("3. Scanning channels set OK"));
  }
  else
  {
    USB.println(F("3. Error while setting 'Scanning channels'"));
  }
  delay(3000);
  
  //////////////////////////
  // 2. check XBee's network parameters
  //////////////////////////
  checkNetworkParams();
}


void loop()
{
  // put your main code here, to run repeatedly:

  // receive XBee packet (wait for 10 seconds)
  error = xbeeZB.receivePacketTimeout( 10000 );

  // check answer  
  if( error == 0 ) 
  {
    // Show data stored in '_payload' buffer indicated by '_length'
    USB.print(F("Data: "));  
    USB.println( xbeeZB._payload, xbeeZB._length);
    
    // Show data stored in '_payload' buffer indicated by '_length'
    USB.print(F("Length: "));  
    USB.println( xbeeZB._length,DEC);
  }
  else
  {
        // Measure temp and hum
    ///////////////////////////////////////
    // 1. Read BME280 Values
    ///////////////////////////////////////
    // Turn on the sensor board
    acc = 1.0;
    
    Events.ON();
    //Temperature
    temp = Events.getTemperature();
    //Humidity
    humid = Events.getHumidity();
    //Pressure
    pres = Events.getPressure();

    

    ///////////////////////////////////////
    // 2. Print BME280 Values
    ///////////////////////////////////////
    USB.println("-----------------------------");
    USB.print("Temperature: ");
    USB.printFloat(temp, 2);
    USB.println(F(" Celsius"));
    USB.print("Humidity: ");
    USB.printFloat(humid, 1); 
    USB.println(F(" %")); 
  //  USB.print("Pressure: ");
  //  USB.printFloat(pres, 2); 
  //  USB.println(F(" Pa")); 
    USB.println("-----------------------------");  
  
    ///////////////////////////////////////////
    // 1. Create ASCII frame
    ///////////////////////////////////////////  
  
    // create new frame
    frame.createFrame(ASCII);  
    frame.addSensor(SENSOR_ACC, acc);
    frame.addSensor(SENSOR_EVENTS_TC, temp);
    frame.addSensor(SENSOR_EVENTS_HUM, humid);
    // add frame fields
  //  frame.addSensor(SENSOR_STR, "new_sensor_frame");
  //  frame.addSensor(SENSOR_TEMP, PWR.getBatteryLevel()); 
  //  
  
    ///////////////////////////////////////////
    // 2. Send packet
    ///////////////////////////////////////////  
  
    // send XBee packet
    send_error = xbeeZB.send( GW_ADDRESS, frame.buffer, frame.length );   
    
    // check TX flag
    if( send_error == 0 )
    {
      USB.println(F("send ok"));
      
      // blink green LED
      Utils.blinkGreenLED();
      
    }
    else 
    {
      USB.println(F("send error"));
      
      // blink red LED
      Utils.blinkRedLED();
    }
  }
  // wait for five seconds
//  delay(5000);

  
  ///////////////////////////////////////
  // 3. Go to deep sleep mode
  ///////////////////////////////////////
//  USB.println(F("enter deep sleep"));
//  PWR.deepSleep("00:00:00:10", RTC_OFFSET, RTC_ALM1_MODE1, ALL_OFF);
//  USB.ON();
//  USB.println(F("wake up\n"));
}

/*******************************************
 *
 *  checkNetworkParams - Check operating
 *  network parameters in the XBee module
 *
 *******************************************/
void checkNetworkParams()
{
  // 1. get operating 64-b PAN ID
  xbeeZB.getOperating64PAN();

  // 2. wait for association indication
  xbeeZB.getAssociationIndication();
 
  while( xbeeZB.associationIndication != 0 )
  { 
    delay(2000);
    
    // get operating 64-b PAN ID
    xbeeZB.getOperating64PAN();

    USB.print(F("operating 64-b PAN ID: "));
    USB.printHex(xbeeZB.operating64PAN[0]);
    USB.printHex(xbeeZB.operating64PAN[1]);
    USB.printHex(xbeeZB.operating64PAN[2]);
    USB.printHex(xbeeZB.operating64PAN[3]);
    USB.printHex(xbeeZB.operating64PAN[4]);
    USB.printHex(xbeeZB.operating64PAN[5]);
    USB.printHex(xbeeZB.operating64PAN[6]);
    USB.printHex(xbeeZB.operating64PAN[7]);
    USB.println();     
    
    xbeeZB.getAssociationIndication();
  }

  USB.println(F("\nJoined a network!"));

  // 3. get network parameters 
  xbeeZB.getOperating16PAN();
  xbeeZB.getOperating64PAN();
  xbeeZB.getChannel();

  USB.print(F("operating 16-b PAN ID: "));
  USB.printHex(xbeeZB.operating16PAN[0]);
  USB.printHex(xbeeZB.operating16PAN[1]);
  USB.println();

  USB.print(F("operating 64-b PAN ID: "));
  USB.printHex(xbeeZB.operating64PAN[0]);
  USB.printHex(xbeeZB.operating64PAN[1]);
  USB.printHex(xbeeZB.operating64PAN[2]);
  USB.printHex(xbeeZB.operating64PAN[3]);
  USB.printHex(xbeeZB.operating64PAN[4]);
  USB.printHex(xbeeZB.operating64PAN[5]);
  USB.printHex(xbeeZB.operating64PAN[6]);
  USB.printHex(xbeeZB.operating64PAN[7]);
  USB.println();

  USB.print(F("channel: "));
  USB.printHex(xbeeZB.channel);
  USB.println();

}



