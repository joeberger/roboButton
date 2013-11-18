/*
  Example Bluetooth Serial Passthrough Sketch
 by: Jim Lindblom
 SparkFun Electronics
 date: February 26, 2013
 license: Public domain

 This example sketch converts an RN-42 bluetooth module to
 communicate at 9600 bps (from 115200), and passes any serial
 data between Serial Monitor and bluetooth module.
 */
#include <SoftwareSerial.h>  

int bluetoothTx = 2;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 3;  // RX-I pin of bluetooth mate, Arduino D3

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

int localState1 = 0; // default state value

String inputBuffer = "";

void setup()
{
  Serial.begin(9600);  // Begin the serial monitor at 9600bps

  bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
  bluetooth.print("$");  // Print three times individually
  bluetooth.print("$");
  bluetooth.print("$");  // Enter command mode
  delay(100);  // Short delay, wait for the Mate to send back CMD
  bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
  // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
  bluetooth.begin(9600);  // Start bluetooth serial at 9600
}

void loop()
{  
  if(bluetooth.available())  // If the bluetooth sent any characters
  {
    char incomingByte = (char)bluetooth.read();
    inputBuffer += incomingByte;
     
    Serial.print(inputBuffer);      
     
    if (inputBuffer.startsWith("@@@")) {
  
      Serial.print("Received StateReport request!");      

      // we've received a request for local state report!
      bluetooth.print(localState1); // the print command encodes in ASCII
      inputBuffer = "";
    }  
    
    if (inputBuffer.startsWith("XXX") && inputBuffer.length() == 4) {
  
      localState1 = String(inputBuffer.charAt(3)).toInt();
      
      Serial.print("Received StateChange request! Changed to '" + String(localState1) + "'.");      

      // NJD TODO  - send acknowledgement
      
      inputBuffer = "";
    }    
}
  
  if(Serial.available())  // If stuff was typed in the serial monitor
  {
    // Send any characters the Serial monitor prints to the bluetooth
    bluetooth.print((char)Serial.read());
  }
  // and loop forever and ever!
}
