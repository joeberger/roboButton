
//
// Bluetooth 
//
#include <SoftwareSerial.h>  

int bluetoothTx = 2;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 3;  // RX-I pin of bluetooth mate, Arduino D3

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

int localState = 1; // default state value

String inputBuffer = "";

// 
// Switch Debounce
//
int switchPinIn = 8;                // the number of the input pin
int statePinOut = 13;                  // the number of the state pin
int switchReadin = LOW;             // the current reading from the switch.. our goal is to debounce this
int previousSwitchReading = LOW;    // the previous reading from the switch.
int switchSteadyState = LOW;        // This is the debounced position of the switch 
long lastReadingChangeTime = 0;     // the last time the reading measured a change
long debounce = 50;                // the debounce time in milliseconds.. This is really a function of the
                                    // physical switch used.

void setup()
{
     Serial.begin(9600);  // Begin the serial monitor at 9600bps

     bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
     bluetooth.print("$");  // Print three times individually
     bluetooth.print("$");
     bluetooth.print("$");  // Enter command mode
     delay(300);  // Short delay, wait for the Mate to send back CMD
     bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
     // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
     bluetooth.begin(9600);  // Start bluetooth serial at 9600

     // Switch Setup
     pinMode(switchPinIn, INPUT_PULLUP); // This means there's an internal pull-up resistor which sets the input high.  The physical
                                           // switch should be tied directly to ground.  When the switch is actuated, the input goes from goes
                                           // from high to low as the internal resistor drops all the voltage.
  
     pinMode(statePinOut, OUTPUT); // we'll use this to convey our single localState variable.
}

void loop()
{    
     debounceAndProcessSwitch();
  
     processBluetooth();

     // and loop forever and ever!
}

void processBluetooth()
{ 
     if(bluetooth.available())  // If the bluetooth sent any characters
     {
          char incomingByte = (char)bluetooth.read();
          inputBuffer += incomingByte;
     
          Serial.print("Current Buffer: '" + String(inputBuffer) + "'.");      
     
          int bufferLength = inputBuffer.length();
          switch (bufferLength) {
            
            case 1:
          
                if (!inputBuffer.startsWith("Q") && !inputBuffer.startsWith("X")) {
                    // incoming garbage.. ignore.
                    inputBuffer = "";  
                }
                break;
            
            case 2:
                
                if (!inputBuffer.startsWith("QQ") && !inputBuffer.startsWith("XX")) {
                    // incoming garbage.. ignore.
                    inputBuffer = "";  
                }
          
              break;  
                 
            case 3:
                
                if (inputBuffer.startsWith("QQQ")) {
                    Serial.print("Received StateReport request!");      

                    // we've received a request for local state report!
                    bluetooth.print(localState); // the print command encodes in ASCII
                    inputBuffer = "";
                } else 
    
                if (!inputBuffer.startsWith("XXX")) { 
                    inputBuffer = "";
                }
          
                break; 
              
            case 4:
    
                if (inputBuffer.startsWith("XXX")) { 
                    localState = String(inputBuffer.charAt(3)).toInt();
      
                    Serial.print("Received StateChange request! Changed to '" + String(localState) + "'.");      
      
                    inputBuffer = "";
                    digitalWrite(13, localState);     
                }
          }
     }
  
     if(Serial.available())  // If stuff was typed in the serial monitor
     {
          // Send any characters the Serial monitor prints to the bluetooth
          bluetooth.print((char)Serial.read());
     }  
}

void debounceAndProcessSwitch()
{
     int switchReading = digitalRead(switchPinIn);
 
     // If the switch changed, due to bounce or pressing...
     if (switchReading != previousSwitchReading)
     {
          // reset the debouncing timer
          lastReadingChangeTime = millis();
     } 
 
     if ((millis() - lastReadingChangeTime) > debounce)
     {
          // Switch has maintained its state long enough to consider valid        
          if (switchReading != switchSteadyState)
          { 
               // The debounced position is different than the current switch state...
               if (switchReading == LOW) 
               {
                    // Since the input has pull-up resistor, a LOW state indicates the switch is closed.. which
                    // is what we want to trigger an internal state change..
                    if (localState == 1)
                    {
                         localState = 0;
                    }
                    else
                    {
                         localState = 1;
                    } 
                    
                    Serial.print("LocalStateChange: '" + String(localState) + "'.\n");      
                    digitalWrite(13, localState); 
               } 
               switchSteadyState = switchReading; // since the reading is valid and different, let's consider it our new state!
          }
     }
    
     previousSwitchReading = switchReading; 
}
