jme-gpioleds-demo
=================
GPIOLedsDemo MIDlet

This demonstration program light a sequence of LEDs connected to device GPIO pins. At fixed frequency, one LED is activated while the others are set to off, starting from the first one in the sequence, iterating over at each step, and restarting from beginning when end is reached.

The sequence of LEDs is created at application startup using the MIDlet attribute "LEDPins" that define a sequence of GPIO port/pins corresponding to each LED. The format of this attribute is: "Port,Pin|Port,Pin|Port,Pin|[...]". Note that a GPIO pin may be present several time in the sequence.

Target platform is Emulator or Raspberry Pi with some LEDs connected to GPIO, but it could be any other platform supporting GPIO.

The default project configuration is for Emulator. "LEDPins" attribute is configured using GPIOPins corresponding to logical LEDs of the Emulator (LED 1, LED 2, etc.).
A second project configuration is for the Raspberry Pi (RPI). "LEDPins" attribute is configured using the following GPIO pins on the board: 17,18,27,22,23,24,25.

On the RPi, the program have been tested using the "7_LEDs" or "16_LEDs" board from BitWizard ( http://bitwizard.nl/wiki/index.php/16_LEDs ) which is convenient to use for this project. 

In all cases, be sure to correctly set the "LEDPins" attribute according to your platform configuration and desired light pattern. The idea is to
try to achieve some interesting pattern, such as a progress bar effect.
