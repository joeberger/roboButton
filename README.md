# ArduinoButton

### This project is a ‘case study’ implementation of Bluetooth geo-fencing.  It’s open-source and freely available.  The project consists of Android and [Arduino](http://en.wikipedia.org/wiki/Arduino) code and some hardware readily available online.  The major hardware components include a set of [Estimote](http://estimote.com/) Bluetooth LE beacons, an [Arduino Inventor’s Kit](https://www.sparkfun.com/products/12060) , a [Bluetooth Classic IC](https://www.sparkfun.com/products/12576), and a [Solid State Relay](https://www.sparkfun.com/products/10684), and an Android phone (4.3 API Level 18 or above) capable of operating as a Bluetooth LE client or ‘central device’.

### It's a work-in-progress.  At present, it's a simple demonstration:  An Android phone running the ArduinoButton application in the background will consume very little power as it monitors for nearby beacons (provided by Estimote).
### Once it detects a beacon, it then scans for any nearby 'buttons' (which you have to build with the aforementioned parts).  If a button is found, the application then initiates a more costly 'Bluetooth Classic' serial connection.  
### While connected, the application presents the user with a simple UI button.  Toggling this button on the phone will toggle the state of the relay on the remote button using the Bluetooth Classic serial connection.  This switch on the button can handle line-current; so you can power on and off an electric lamp, as an example.
### The application includes an 'auto' mode:  when a button is found, it is immediately turned on.  When a beacon is 'lost', it immediately turns off the button.  This auto-mode works because the range of the Beacon Classic is much greater than the configured range of the beacon.

### With all this, you can have a light turn on and off automatically as you enter and leave a room.



## Setup

*Note: You'll need a little skill with a soldering iron for this project (but even that you can make up as you going along)*

1. Purchase a pack of Estimote Beacons





```
cd my-new-project
mvn clean test
```

