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
#define KEYCODE_SIZE         4
#define DEBUG 0
#define UNLOCK_TIMEOUT_MS    300
#define LOCK_TIMEOUT_MS      275

/*************************************************************************/
/* Pin Defines */
/*************************************************************************/
// Motor Controller+
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
char lockCode[] = {'1', '2', '3', '4'};
char unlockCode[] = {'1', '2', '3', '4'};
char queryCode[] = {'1', '2', '3', '4'};


// our position in code guess
int guess_index = 0;
char *targetCode;

/*************************************************************************/
void setup() {
  Serial.begin(57600);
  Serial.setTimeout(25);

  // Motor Controller Setup
  pinMode(STBY, OUTPUT);
  pinMode(PWMA, OUTPUT);
  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);

  // Switch Setup
  pinMode(SW1, INPUT);
  digitalWrite(SW1, HIGH); // Enable internal pullup .. while switch is open (locked), pin will read HIGH
  digitalWrite(STBY, LOW); //enable standby
  
  Bean.attachChangeInterrupt(SW1, wakeUp); 
}
/*************************************************************************/
void loop() {

  // wake up serial input (bluetooth module is connect to arduino's STBY pin)

  size_t length = 10;
  char buffer[length];

  // our position in incoming data
  char incoming_index = 0;

  length = Serial.readBytes(buffer, length);
  if (length > 0) {
      if (DEBUG) Serial.println("Incoming bytes (" + String(length) + "): '" + String(buffer) + "'. (guess_index is '" + String(guess_index) + "')");
    
      for (incoming_index = 0; incoming_index < length; incoming_index++) {
          if (buffer[incoming_index] == 'Q') {
              if (DEBUG) Serial.println("Incoming Query Code!");
    
              targetCode = queryCode;
              guess_index = 0;
              continue;
          } else if (buffer[incoming_index] == 'L') {
              if (DEBUG) Serial.println("Incoming Lock Code!");
    
              targetCode = lockCode;
              guess_index = 0;
              continue;
          } else if (buffer[incoming_index] == 'U') {
              if (DEBUG) Serial.println("Incoming Unlock Code!");
    
              targetCode = unlockCode;
              guess_index = 0;
              continue;
          }
    
          if (DEBUG) Serial.println("Checking '" + String(buffer[incoming_index]) + "' against '" + String(targetCode[guess_index]) + "'.");
          if (buffer[incoming_index] == targetCode[guess_index]) {
              if (DEBUG) Serial.println("Next code byte found!");
              guess_index++;
          }
    
          if (guess_index == KEYCODE_SIZE) {
              if (DEBUG) Serial.println("Code completed!");
              Bean.setLed(0, 0, 255);
    
              char locked = digitalRead(SW1);
    
              if (targetCode == lockCode) {
                  if (!locked) {
                      if (DEBUG) Serial.println("Locking!");
                      LockTheDoor();
                  } else {
                      if (DEBUG) Serial.println("Already Locked!");
                  }
                  sendLockState();
    
              } else if (targetCode == unlockCode) {
                  if (!locked) {
                      if (DEBUG) Serial.println("Already Unlocked!");
                  } else {
                      if (DEBUG) Serial.println("Unlocking!");
                      UnlockTheDoor();
                  }
                  sendLockState();
        
              } else if (targetCode == queryCode) {
                  if (DEBUG) {
                      Serial.print("Replying to Query!");
                  }
    
                  sendLockState();
                  delay(1000);
                  sendLockState();
              } 
              
              guess_index = 0;
          } // end if code completed
      } // done with current buffer
  } else {
      sendLockState(); 
  }

  if (DEBUG) Serial.println("Done. (guess_index is '" + String(guess_index) + "')");

  char locked = digitalRead(SW1);
  if (!locked) {
      Bean.setLed(0, 255, 0);
  } else {
      Bean.setLed(255, 0, 0);
  }

  Bean.sleep(0xFFFFFFFF); // Sleep until a serial message wakes us up
}

void wakeUp() {}

void sendLockState() {
  char locked = digitalRead(SW1);
  if (!locked) {
      Serial.print("unlocked");
   } else {
      Serial.print("locked");
   } 
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
  if(digitalRead(SW1) == LOW){
     digitalWrite(STBY, HIGH); //disable standby
     Bean.detachChangeInterrupt(SW1);
     move(255,0);
     while(digitalRead(SW1) == LOW);
     delay(LOCK_TIMEOUT_MS);
     move(0,0);
     digitalWrite(STBY, LOW); //enable standby
     Bean.attachChangeInterrupt(SW1, wakeUp); 
  }
}
/*************************************************************************/
void UnlockTheDoor(void) {
  if(digitalRead(SW1) == HIGH){
     digitalWrite(STBY, HIGH); //disable 
     Bean.detachChangeInterrupt(SW1);
     move(255,1);
     while(digitalRead(SW1) == HIGH);
     delay(UNLOCK_TIMEOUT_MS);
     move(0,1);
     digitalWrite(STBY, LOW); //enable standby
     Bean.attachChangeInterrupt(SW1, wakeUp); 
  }
}
/*************************************************************************/

