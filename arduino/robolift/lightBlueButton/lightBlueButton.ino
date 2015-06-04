/*
  LightBlue Latch - Sandbox Keycode

  For motor control using Sparkfun:
  https://www.sparkfun.com/products/9457
  Motor Driver 1A Dual TB6612FNG
  P/N: ROB-09457

  Using Lock:
  http://www.amazon.com/dp/B000FBU2KW/ref=pe_385040_30332200_TE_item
  Morning Industry RF-01SN Radio Frequency Remote Deadbolt
  P/N: RF-01SN

  Motor Controller Pin Connections:
  PWMA:  1
  AIN1:  0
  AIN2:  2
  STBY:  3

  Switch Pin Conneciton: (Active Low)
  SW1:  5

  Switch High = Locked
  Switch Low = Unlocked


*/
/*************************************************************************/
/* Defines */
/*************************************************************************/
#define UNLOCK_TIMEOUT_MS    300
#define LOCK_TIMEOUT_MS      275
#define KEYCODE_SIZE         sizeof(keycode)
/*************************************************************************/
/* Pin Defines */
/*************************************************************************/
// Motor Controller
int PWMA = 1; //Speed control
int AIN1 = 0; //Direct ion
int AIN2 = 2; //Direction
int STBY = 3; //standby

// Switch
int SW1 = 5;
/*************************************************************************/
/* Global Variables */
/*************************************************************************/
/* Define the unlock keycode from the sandbox */
const char keycode[] = {'1', '2', '3', '4'};
/*************************************************************************/
void setup() {
  Serial.begin(57600);
  Serial.setTimeout(25);
  Bean.enableWakeOnConnect(true); // wakes on connect or disconnect 

  // Motor Controller Setup
  pinMode(STBY, OUTPUT);
  pinMode(PWMA, OUTPUT);
  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);

  // Switch Setup
  pinMode(SW1, INPUT);
  digitalWrite(SW1, HIGH); // Enable internal pullup .. while switch is open (unlocked), pin will read HIGH
  digitalWrite(STBY, LOW); //enable standby
}
/*************************************************************************/
void loop() {
  
  Serial.println("Loop entered.");
  
  char buffer[10];
  size_t length = 10;
  static char last_value = 0;
  static char index = 0;
  static char lock_state = 0;

  length = Serial.readBytes(buffer, length);

  Serial.println("Buffer: '" + String(buffer) + "'.");

  if ( length > 0 ) {
      Serial.println("currentValue: '" + String(buffer[0]) + "', lastValue '" + String(last_value) + "' , nextKey('" + String(keycode[index]) + "').");

    if (buffer[0] != last_value) { // Check to see if it is the same value
      if (buffer[0] == keycode[index]) {
        Serial.println("Fuckyeah!: '" + String(buffer[0]) + "'.");
        index++;
        if (index == KEYCODE_SIZE) { 
          Serial.println("Code complete!");
         
          // Lock / Unlock door
          if (lock_state) {
            //LockTheDoor();
          } else {
            //UnlockTheDoor();
          }
          lock_state = !lock_state;
          //Serial.write(lock_state);

          index = 0;
        }
      } else {
        index = 0;
      }
    }
    last_value = buffer[0];
  } else {
    // no data available, must have been awakened for connection change.. send current state. 
    lock_state = digitalRead(SW1);
    
    //Serial.write(lock_state);
  }
  
  if (lock_state) {
    Bean.setLed(255,0,0);
  } else {
    Bean.setLed(0,255,0);
  }   
  
  Bean.sleep(0xFFFFFFFF); // Sleep until a serial message wakes us up
}
/*************************************************************************/
void move(int speed, int direction) {
  //Move motor at speed and direction
  //speed: 0 is off, and 255 is full speed
  //direction: 0 clockwise, 1 counter-clockwise

  boolean inPin1 = LOW;
  boolean inPin2 = HIGH;

  if (direction == 1) {
    inPin1 = HIGH;
    inPin2 = LOW;
  }
  digitalWrite(AIN1, inPin1);
  digitalWrite(AIN2, inPin2);
  analogWrite(PWMA, speed);
}

/*************************************************************************/
void LockTheDoor(void) {
  if (digitalRead(SW1) == HIGH) {
    digitalWrite(STBY, HIGH); //disable standby
    move(255, 0);
    while (digitalRead(SW1) == HIGH);
    delay(LOCK_TIMEOUT_MS);
    move(0, 0);
    digitalWrite(STBY, LOW); //enable standby
  }
}
/*************************************************************************/
void UnlockTheDoor(void) {
  if (digitalRead(SW1) == LOW) {
    digitalWrite(STBY, HIGH); //disable
    move(255, 1);
    while (digitalRead(SW1) == LOW);
    delay(UNLOCK_TIMEOUT_MS);
    move(0, 1);
    digitalWrite(STBY, LOW); //enable standby
  }
}
/*************************************************************************/

