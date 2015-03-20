This project is a ‘case study’ implementation of Bluetooth geo-fencing.  It’s open-source and freely available.  The project consists of Android and [Arduino](http://en.wikipedia.org/wiki/Arduino) code and some hardware readily available online.  The major hardware components include a set of [Estimote](http://estimote.com/) Bluetooth LE beacons, an [Arduino RedBoard](https://www.sparkfun.com/products/12757) , a [Bluetooth Classic IC](https://www.sparkfun.com/products/12576), and a [Solid State Relay](https://www.sparkfun.com/products/10684), and an Android phone (4.3 API Level 18 or above) capable of operating as a Bluetooth LE client or ‘central device’.

It's a work-in-progress.  At present, it's a simple demonstration:  An Android phone running the RoboButton application in the background will consume very little power as it monitors for nearby beacons (provided by Estimote and configure as described below).
Once it detects a beacon, it then scans for any nearby 'buttons' (which you have to build with the aforementioned parts).  If a button is found, the application then initiates a more costly 'Bluetooth Classic' serial connection.
While connected, the application presents the user with a simple UI button.  Toggling this button on the phone will toggle the state of the relay on the remote button using the Bluetooth Classic serial connection.  This relay can handle line-current; so you can power on and off an electric lamp, as an example.
The application includes an 'auto' mode:  when a button is found, it is immediately turned on.  When a beacon is 'lost', it immediately turns off the button.  This auto-mode works because the range of the Beacon Classic is much greater than the configured range of the beacon.  Also, the application considers itself 'out of range' of the beacon well before actual communications with the beacon is lost.

With all this, you can have a light turn on and off automatically as you enter and leave a room - all the while having your phone asleep in your pocket.



## Setup

*Note: You'll need a little skill with a soldering iron for this project (but even that you can make up as you go along)*

### Configure Estimote Beacon

1. Purchase a pack of [Estimote](http://estimote.com/) Bluetooth LE Beacons.  They're not cheap - $99.  Download the [Esimote App](https://play.google.com/store/apps/details?id=com.estimote.apps.main&hl=en) from the Google Play Store.  Launch the app and select the 'Beacons' item from the [opening screen](https://drive.google.com/file/d/0BxujY_Gv8MOgb0kxMlFWdHN6RU0/view?usp=sharing).
2. You should [see your beacon in range](https://drive.google.com/file/d/0BxujY_Gv8MOgVzc4a1dac3diZzg/view?usp=sharing).  It will be the little 'estimote' shaped icon (duh).  Click on the beacon to edit.
3. Once in the [beacon edit screen](https://drive.google.com/file/d/0BxujY_Gv8MOgOHotMTU0NVB4U0E/view?usp=sharing), wait for the text below the beacon near the top to say 'Connected'.  Then edit the 'Major' value to 2112.
4. By assigning the Major value to this beacon, you've created a specific 'region'.  Whenver the app detects this region, it will begin to look for a button.

### Build the Robo Button

1. Here are the major parts you will need: [Arduino RedBoard](https://www.sparkfun.com/products/12757) , a [Bluetooth Classic IC](https://www.sparkfun.com/products/12576), and a [Solid State Relay](https://www.sparkfun.com/products/10684)
2. Here is [Schematic Diagram](https://drive.google.com/file/d/0BxujY_Gv8MOgYXVsNDBoQ3pzRU0/view?usp=sharing) of the Robo Button.
3. Here is my first [Robo Button Sample](https://drive.google.com/file/d/0BxujY_Gv8MOgdEpTU0xvclEtLTg/view?usp=sharing).  On the left is where you can plug in an appliance to be controlled by the button.  The blue object in the foreground is the Estimote beacon.  This particular beacon was cut open so I could replace the CR2477N battery.  It only lasted about 8 months.

## World Domination 

Now turn everyting on!  You don't need to pair these bluetooth devices with your device; the application will take care of this for you.

This application uses very few features of the Arduino processor.  You can provide all sorts of additional functionality! Woot!

## Software Design

The [MonitoringService](https://drive.google.com/file/d/0BxujY_Gv8MOgMUQzOW9qUVBiaEk/view?usp=sharing) runs in the background and consumes very little power as it listen for BT LE devices.  When it detects a beacon with the appropriately defined 'Region', it then begins to search for instances of the Robo Button.  Communications with the button is established and maintained by the [ButtonCommunicator](https://drive.google.com/file/d/0BxujY_Gv8MOgWU50ajl1Y2ZRN0k/view?usp=sharing).  Although the MonitoringService and the ButtonCommunicator(s) are tightly coupled, they communicate relevant state changes (e.g. 'Button Found', 'Button State Change Detected', etc.) to the rest of the application's components using the Otto Message Button (an example of an Observer pattern).

The [MainControllerActivity](https://drive.google.com/file/d/0BxujY_Gv8MOgNHZORHlOb2JYOVU/view?usp=sharing) is a very light weight activity which observes incoming Otto Events (e.g. 'Button Found', 'Button Lost') and adds or removes instances of RoboButtonFragment when appropriate.

* Notes:  Although most of the application has been designed to handle the possibility of controlling multiple buttons at once, I don't support this behavior at this time as it makes the code more cumbersome and this is meant to be a case study (and it would really be silly to control more than one button at a time, really).
* Notes:  In the real world, the 'costly connection' wouldn't be to a Bluetooth Classic device, but more likely would be a connection (such as a WebSocket) to a enterprise resource.  Again, this is meant to be a case study that can be easily thrown together with some solder and duct tape.

## Testing

I run all unit tests on a local VM (Robolectric) and employ Dependency Injection (Dagger) and Mock collaborators (Mockito).
