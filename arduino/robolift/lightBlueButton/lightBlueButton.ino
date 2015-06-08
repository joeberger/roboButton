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
#define KEYCODE_SIZE         4
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
  digitalWrite(SW1, HIGH); // Enable internal pullup .. while switch is open (unlocked), pin will read HIGH
  digitalWrite(STBY, LOW); //enable standby
}
/*************************************************************************/
void loop() {

    // wake up serial input (bluetooth module is connect to arduino's STBY pin)
  
    size_t length = 10;
    char buffer[length];
    
    // our position in incoming data
    char incoming_index = 0;

    length = Serial.readBytes(buffer, length);

    Serial.println("Incoming bytes (" + String(length) + "): '" + String(buffer) + "'. (guess_index is '" + String(guess_index) + "')");

    for (incoming_index = 0; incoming_index < length; incoming_index++) {
      
        if (buffer[incoming_index] == 'Q') {
            Serial.println("Incoming Query Code!");

            targetCode = queryCode;
            guess_index = 0; 
            continue;        
        } else
        if (buffer[incoming_index] == 'X') {
            Serial.println("Incoming Action Code!");

            targetCode = lockCode;
            guess_index = 0;
            continue;         
        }

        Serial.println("Checking '" + String(buffer[incoming_index]) + "' against '" + String(targetCode[guess_index]) + "'.");
        if (buffer[incoming_index] == targetCode[guess_index]) {
            Serial.println("Next code byte found!");
            guess_index++;
        }  
             
        if (guess_index == KEYCODE_SIZE) {
            Serial.println("Code completed!");

            char unlocked = digitalRead(SW1);
          
            if (targetCode == lockCode) {
                if (unlocked) {
                    Serial.println("Locking!");

                    //LockTheDoor();
                } else {
                    Serial.println("Unlocking!");

                    //UnlockTheDoor();
                }
            } else 
            if (targetCode == queryCode) {
                Serial.println("Replying to Query!");

                //Serial.write(unlocked);
            }  
        
            guess_index=0;  
        } 
    }    

    Serial.println("Done. (guess_index is '" + String(guess_index) + "')");

    char unlocked = digitalRead(SW1);
    if (unlocked) {
        Bean.setLed(0, 255, 0);
    } else {
        Bean.setLed(255, 0, 0);
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

