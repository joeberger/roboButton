
//
// Bluetooth 
//
#include <SoftwareSerial.h>  
#include <avr/pgmspace.h>

// ******** DEBUG ==== should auto config to adapt different mother board *********
//#define DATA_1 (PORTF |=  0X01)    // DATA 1    // for ATMEGA
//#define DATA_0 (PORTF &=  0XFE)    // DATA 0    // for ATMEGA
//#define STRIP_PINOUT DDRF=0xFF  // for ATMEGA
#define DATA_1 (PORTC |=  0X01)    // DATA 1    // for UNO
#define DATA_0 (PORTC &=  0XFE)    // DATA 0    // for UNO
#define STRIP_PINOUT (DDRC=0xFF)    // for UNO

PROGMEM const unsigned long pattern_test_red[10][10]={
  {0xff0000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0xff0000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0xff0000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0xff0000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0xff0000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0xff0000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xff0000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xff0000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xff0000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xff0000},
};

PROGMEM const unsigned long pattern_test_blue[10][10]={
  {0x00ff00,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x00ff00,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x00ff00,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x00ff00,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x00ff00,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x00ff00,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x00ff00,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x00ff00,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x00ff00,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x00ff00},
};

PROGMEM const unsigned long pattern_test_green[10][10]={
  {0x0000ff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x0000ff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x0000ff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x0000ff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x0000ff,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x0000ff,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x0000ff,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x0000ff,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x0000ff,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x0000ff},
};

PROGMEM const unsigned long pattern_test_white[10][10]={
  {0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff},
};

PROGMEM const unsigned long pattern_test_comet1[][10]={
  {0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000},
  {0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x111111,0x444444,0xffffff},
};

PROGMEM const unsigned long pattern_test_comet2[][10]={
  {0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000},
  {0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000},
  {0x111111,0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000},
  {0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000},
  {0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff},
  {0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000},
  {0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000},
  {0x111111,0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000},
  {0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000},
  {0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0x111111,0x444444,0xffffff},
};

PROGMEM const unsigned long pattern_test_comet3[][10]={
  {0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff},
  {0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xffffff,0x444444},
  {0x111111,0x444444,0xffffff,0x000000,0x000000,0x000000,0x000000,0xffffff,0x444444,0x111111},
  {0x000000,0x111111,0x444444,0xffffff,0x000000,0x000000,0xffffff,0x444444,0x111111,0x000000},
  {0x000000,0x000000,0x111111,0x444444,0xffffff,0xffffff,0x444444,0x111111,0x000000,0x000000},
  {0x000000,0x000000,0x111111,0x444444,0xffffff,0xffffff,0x444444,0x111111,0x000000,0x000000},
  {0x000000,0x000000,0x000000,0xffffff,0x444444,0x444444,0xffffff,0x000000,0x000000,0x000000},
  {0x000000,0x000000,0xffffff,0x444444,0x111111,0x111111,0x444444,0xffffff,0x000000,0x000000},
  {0x000000,0xffffff,0x444444,0x111111,0x000000,0x000000,0x111111,0x444444,0xffffff,0x000000},
  {0xffffff,0x444444,0x111111,0x000000,0x000000,0x000000,0x000000,0x111111,0x444444,0xffffff},
};

PROGMEM const unsigned long pattern_test_rainbow[10][10]={
  {0xff0000,0xff7f00,0xffff00,0x00ff00,0x0000ff,0x6f00ff,0x8f00ff,0x000000,0x000000,0x000000},
  {0x000000,0xff0000,0xff7f00,0xffff00,0x00ff00,0x0000ff,0x6f00ff,0x8f00ff,0x000000,0x000000},
  {0x000000,0x000000,0xff0000,0xff7f00,0xffff00,0x00ff00,0x0000ff,0x6f00ff,0x8f00ff,0x000000},
  {0x000000,0x000000,0x000000,0xff0000,0xff7f00,0xffff00,0x00ff00,0x0000ff,0x6f00ff,0x8f00ff},
  {0x8f00ff,0x000000,0x000000,0x000000,0xff0000,0xff7f00,0xffff00,0x00ff00,0x0000ff,0x6f00ff},
  {0x6f00ff,0x8f00ff,0x000000,0x000000,0x000000,0xff0000,0xff7f00,0xffff00,0x00ff00,0x0000ff},
  {0x0000ff,0x6f00ff,0x8f00ff,0x000000,0x000000,0x000000,0xff0000,0xff7f00,0xffff00,0x00ff00},
  {0x00ff00,0x0000ff,0x6f00ff,0x8f00ff,0x000000,0x000000,0x000000,0xff0000,0xff7f00,0xffff00},
  {0xffff00,0x00ff00,0x0000ff,0x6f00ff,0x8f00ff,0x000000,0x000000,0x000000,0xff0000,0xff7f00},
  {0xff7f00,0xffff00,0x00ff00,0x0000ff,0x6f00ff,0x8f00ff,0x000000,0x000000,0x000000,0xff0000},
};


int bluetoothTx = 2;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 3;  // RX-I pin of bluetooth mate, Arduino D3

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

int localState = 0; // default state value

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
     
     STRIP_PINOUT;        // set output pin - DEBUG: should auto detect which mother board for use

     reset_strip();
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
                    changeLocalState(String(inputBuffer.charAt(3)).toInt());
      
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

void changeLocalState(int newState) {
     localState = newState;
   
     if (localState == 0) {
          send_1M_pattern(pattern_test_red, 10, 50);
     } 
     
     if (localState == 1) {
          send_1M_pattern(pattern_test_green, 10, 50);
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
                         changeLocalState(0);
                    }
                    else
                    {
                         changeLocalState(1);
                    } 
                    
                    Serial.print("LocalStateChange: '" + String(localState) + "'.\n");      
                    digitalWrite(13, localState); 
               } 
               switchSteadyState = switchReading; // since the reading is valid and different, let's consider it our new state!
          }
     }
    
     previousSwitchReading = switchReading; 
}

/*******************************************************************************
 * Function Name  : send_1M_pattern
 * Description    : Transmit pattern to whole 1 meter strip
 *                  
 * Input          : pointer to ROM pattern; pattern length; frame rate
 *                  
 * Output         : None
 * Return         : None
 *******************************************************************************/
void send_1M_pattern(const unsigned long data[][10], int pattern_no, int frame_rate)
{
  int i=0;
  int j=0;
  uint32_t temp_data;

  for (i=0;i<pattern_no;i++)
  {
    noInterrupts();
    for (j=0;j<10;j++)
    {
      temp_data=pgm_read_dword_near(&data[i][j]);
      send_strip(temp_data);
    }
    interrupts();

    delay(frame_rate);

  }




}


/*******************************************************************************
 * Function Name  : send_strip
 * Description    : Transmit 24 pulse to LED strip
 *                  
 * Input          : 24-bit data for the strip
 *                  
 * Output         : None
 * Return         : None
 *******************************************************************************/
void send_strip(uint32_t data)
{
  int i;
  unsigned long j=0x800000;
  
 
  for (i=0;i<24;i++)
  {
    if (data & j)
    {
      DATA_1;
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");    
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      
/*----------------------------*/
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");  
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");  
      __asm__("nop\n\t");  
      __asm__("nop\n\t");        
/*----------------------------*/      
      DATA_0;
    }
    else
    {
      DATA_1;
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");    
      DATA_0;
/*----------------------------*/      
       __asm__("nop\n\t");
      __asm__("nop\n\t");
      __asm__("nop\n\t");      
/*----------------------------*/         
    }

    j>>=1;
  }


  
}

/*******************************************************************************
 * Function Name  : reset_strip
 * Description    : Send reset pulse to reset all color of the strip
 *                  
 * Input          : None
 *                  
 * Output         : None
 * Return         : None
 *******************************************************************************/
void	reset_strip()
{
  DATA_0;
  delayMicroseconds(20);
}

