This project is a ‘case study’ implementation of Bluetooth geo-fencing.  It’s open-source and freely available.  The project consists of Android and [Arduino](http://en.wikipedia.org/wiki/Arduino) code and some hardware readily available online.

See my [2015 AnDevCon Presentation](https://drive.google.com/file/d/0BxujY_Gv8MOgTkFzZHJQTnQwcGM/view?usp=sharing) on this project.

There are two different 'RobotButtons' that I've created.  The first employs Bluetooth Classic technology and the second uses Bluetooth LE (BLE).  Both use BLE as an iBeacon for proximal context.

It's a work-in-progress - meant to be used purely for inspiration and experimentation.  At present, it's a simple demonstration:  An Android phone running the RoboButton application in the background will consume very little power as it monitors for nearby beacons (provided by Estimote and configured as described below).
Once the phone detects a beacon, it then scans for any nearby 'buttons' (which you have to build with the parts described below).  If a button is found, the application then initiates a more costly 'Bluetooth Classic' serial connection or a low-power BLE connection.

While connected, the application presents the user with a simple UI button.  In the Bluetooth Classic RoboButton, toggling this button on the phone will toggle the state of the relay on the remote button using the Bluetooth Classic serial connection.  This relay can handle line-current; so you can power on and off an electric lamp, as an example.  In the BLE RoboButton, toggling this button will open and close a dead-bolt lock.

The application includes an 'auto' mode:  when a button is found, it is immediately turned on.  When a beacon is 'lost', it immediately turns off the button.  This auto-mode works because the range of the RoboButton is much greater than the configured range of the beacon.  The application considers itself 'out of range' of the beacon well before actual communications with the beacon is lost.

This application also uses notifications in conjunction with a background service to provide the user with the ability to control the button completely from the Android Notification Bar --> without explicitly launching the application.

## Hardware

For either RoboButton, you'll need to buy an [Estimote](http://estimote.com/) Bluetooth LE beacons Devlopers Kit.

For the Classic RoboButton, the major hardware components include an [Arduino RedBoard](https://www.sparkfun.com/products/12757), a [Bluetooth Classic IC](https://www.sparkfun.com/products/12576), a [Solid State Relay](https://www.sparkfun.com/products/10684), a 12V power supply, an extension cord you don't mind cutting up, and an Android phone (4.3 API Level 18 or above) capable of operating as a Bluetooth LE client or ‘central device’.  With this RoboButton, you can have a light turn on and off automatically as you enter and leave a room - all the while having your phone asleep in your pocket.

For the BLE RoboButton, the major hardware components include a [Light Blue Bean BLE/Microprocessor](http://punchthrough.myshopify.com/products/bean), an [H-Bridge Motor Controller](https://www.sparkfun.com/products/9457), a keyfob-controlled [remote controlled dead bolt](http://www.amazon.com/gp/product/B000FBU2KW/ref=oh_aui_detailpage_o01_s00?ie=UTF8&psc=1) (don't worry, we're going to rip out any existing RF circuitry) and an Android phone (4.3 API Level 18 or above) capable of operating as a Bluetooth LE client or ‘central device’.  With this RoboButton, you can open and close a dead bolt automatically as you enter and leave a room - all the while having your phone asleep in your pocket.  This button was derived from one of the many great example projects from [PunchThrough Bean Examples](https://punchthrough.com/bean/examples/)



## Setup

*Note: You'll need a little skill with a soldering iron for this project (but even that you can make up as you go along)*

### Configure Estimote Beacon

1. Purchase a pack of [Estimote](http://estimote.com/) Bluetooth LE Beacons.  They're not cheap - $99.  Download the [Esimote App](https://play.google.com/store/apps/details?id=com.estimote.apps.main&hl=en) from the Google Play Store.  Launch the app and select the 'Beacons' item from the [opening screen](https://drive.google.com/file/d/0BxujY_Gv8MOgb0kxMlFWdHN6RU0/view?usp=sharing).
2. You should [see your beacon in range](https://drive.google.com/file/d/0BxujY_Gv8MOgVzc4a1dac3diZzg/view?usp=sharing).  It will be the little 'estimote' shaped icon (duh).  Click on the beacon to edit.
3. Once in the [beacon edit screen](https://drive.google.com/file/d/0BxujY_Gv8MOgOHotMTU0NVB4U0E/view?usp=sharing), wait for the text below the beacon near the top to say 'Connected'.  Then edit the 'UUID' value to '**b9407f30f5f8466e211225556b57fe6d**'.
4. By assigning the UUID value to this beacon, you've created a specific 'region' (by default, all Estimote beacons have the same UUID).  Whenver the app detects this region, it will begin to look for a button.

### Build the Classic Robo Button

I call this the 'Purple' button throughout the code because I printed its chassis with Purple PLA plastic.  Download [OpenScad](http://www.openscad.org/) and open the [3D Printer design](media/purpleButtonChassis.scad) for this button.

Here is the [schematic diagram](media/purpleButtonSchematic.pdf) for the purple button.


### Build the BLE Robo Button


I call this the 'Blue' button throughout the code because I printed its chassis with Blue PLA plastic.  Download [OpenScad](http://www.openscad.org/) and open the [3D Printer design](media/blueButtonChassis.scad) for this button.

Here is the [schematic diagram](media/blueButtonSchematic.png) for the blue button.



## World Domination 

Now turn everyting on!  You don't need to pair these bluetooth devices with your device; the application will take care of this for you.

This application uses very few features of the Arduino processor.  You can provide all sorts of additional functionality! Woot!

## Software Design

The MonitoringService runs in the background and consumes very little power as it listen for BT LE devices.  When it detects a beacon with the appropriately defined 'Region', it then begins to search for instances of the Robo Button.  Communications with the button is established and maintained by the ButtonCommunicator.  Although the MonitoringService and the ButtonCommunicator(s) are tightly coupled, they communicate relevant state changes (e.g. 'Button Found', 'Button State Change Detected', etc.) to the rest of the application's components using the Otto Message Button (an example of an Observer pattern).

The MainControllerActivity is a very light weight activity which observes incoming Otto Events (e.g. 'Button Found', 'Button Lost') and adds or removes instances of RoboButtonFragment when appropriate.

* Notes:  Although most of the application has been designed to handle the possibility of controlling multiple buttons at once, I don't support this behavior at this time as it makes the code more cumbersome and this is meant to be a case study (and it would really be silly to control more than one button at a time, really).

