/**
 * GPIOLedsDemoMIDlet.java
 * This file is part of GPIOLedsDemo
 * Copyright 2014 Gabriel Cuvillier
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.gabrielcuvillier.jmedemos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.midlet.MIDlet;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

/**
 * GPIOLedsDemo MIDlet. <p>
 * 
 * This demonstration program lights up a sequence of LEDs connected to device GPIO 
 * pins. At fixed frequency, one LED is activated while the others are set to off, 
 * starting from the first one in the sequence, iterating over at each step, and 
 * restarting from beginning when its end is reached. <p>
 *
 * The sequence of LEDs is created at application startup using the MIDlet attribute
 * "LEDPins" that define a sequence of GPIO port/pins corresponding to each LED. The 
 * format of this attribute is: "Port,Pin|Port,Pin|Port,Pin|[...]". 
 * Note that a GPIO pin may be present several time in the sequence.<p>
 *
 * Target platform is Emulator or Raspberry Pi with some LEDs connected to GPIO, 
 * but it could be any other platform supporting GPIO. <p>
 * 
 * The default project configuration is for Emulator. "LEDPins" attribute is configured
 * using GPIOPins corresponding to logical LEDs of the Emulator (LED 1, LED 2, etc.).
 * A second project configuration is for the Raspberry Pi (RPI). "LEDPins" attribute is 
 * configured using the following GPIO pins on the board: 17,18,27,22,23,24,25. <p>
 *
 * On the RPi, the program have been tested using the "7_LEDs" or "16_LEDs" board 
 * from BitWizard ( http://bitwizard.nl/wiki/index.php/16_LEDs ) which is convenient 
 * to use for this project. A default LED/GPIOPin configuration for the RPi may be 
 * found in the MIDlet attribute "LEDPinsRaspi" using 7 LEDs. <p>
 * 
 * In all cases, be sure to correctly set the "LEDPins" attribute according to your
 * platform LED/GPIOPin configuration and desired light pattern. The idea is to 
 * try to achieve some interesting pattern, such as a progress bar effect. <p>
 * <br>
 * <br>
 * Implementation overview:<p>
 *   
 * - LEDs are represented as GPIOPin devices, and the sequence of LEDs as an array 
 * of GPIOPin.<p>
 *   
 * - A Timer object is used to run a TimerTask at fixed frequency. This task implements 
 * the LED Lighting algorithm (LEDLightingTimerTaskImpl class): its run method iterates 
 * over the LEDs sequence at each call to light up the current LED and light down 
 * the previous one. The current LED index is then incremented for next iteration, 
 * or reset to beginning at end of sequence.<p>
 *    
 * - At start of application (startApp() method), the "LEDPins" MIDlet attribute 
 * is parsed to extract GPIO pin configurations (parseLEDsPinConfigs() method), and 
 * each configuration is opened to create the LEDs sequence. Then, the "TimerInterval" 
 * MIDlet attribute is parsed, and the Timer object is finally created with the LED 
 * lighting task scheduled.<p>
 *    
 * - At end of application (destroyApp() method), the Timer is canceled, and each 
 * GPIOPin is closed. See method implementation for possible concurrency issue here. <p>
 * 
 * @author Gabriel Cuvillier
 */
public class GPIOLedsDemoMIDlet extends MIDlet {

    /** LED Sequence, as an array of GPIOPins. */
    private GPIOPin[] _LEDSequence;

    /** LED Lighting Timer, that will schedule the LED Lighting TimerTask.
     * See LEDLightingTimerTaskImpl */
    private Timer _LEDLightingTimer;
    
    /** MIDlet attribute name to define GPIOPins of the LEDs sequence. */
    public static final String LED_PINS_ATTRIBUTE = "LEDPins";
    /** MIDlet attribute name to define Timer Interval. */
    public static final String TIMER_INTERVAL_ATTRIBUTE = "TimerInterval";
    /** Default timer interval. */
    public static final int DEFAULT_TIMER_INTERVAL = 100; // 100ms (10 hertz)
    
    /** Start of Application. */
    @Override
    public void startApp() {
        System.out.println("Starting GPIOLedsDemo application");

        // Parse the "LEDPins" MIDlet attribute to have a list of GPIOPinConfig
        String LEDsPinsStr = getAppProperty(LED_PINS_ATTRIBUTE);
        if (LEDsPinsStr == null) {
            throw new RuntimeException("\"LEDPins\" MIDlet attribute not found");
        }
        ArrayList<GPIOPinConfig> GPIOPinConfigList = parseLEDPinConfigs(LEDsPinsStr);

        // If there is at least one configuration
        if (GPIOPinConfigList.size() > 0) {
            System.out.println("Opening LED pins");
            // Create the LEDs sequence by opening each GPIOPin according to parsed pin 
            // configurations
            try {
                // As we may have the same GPIOPin may present several times in the sequence, 
                // maintain uniqueness of opened devices
                HashMap<GPIOPinConfig, GPIOPin> OpenedDevices = new HashMap();
                
                _LEDSequence = new GPIOPin[GPIOPinConfigList.size()];
                int nIdx = 0;
                // For each PinConfig
                for (GPIOPinConfig currLEDConfig : GPIOPinConfigList) {
                    // Check if it is already opened
                    if (OpenedDevices.containsKey(currLEDConfig)) {
                        // In that case, simply get the already opened GPIOPin
                        _LEDSequence[nIdx] = OpenedDevices.get(currLEDConfig);
                    }
                    else {
                        System.out.format("Open GPIOPin: %d\n", currLEDConfig.getPinNumber());
                        // Otherwise, open the GPIOPin
                        _LEDSequence[nIdx] = (GPIOPin) DeviceManager.open(currLEDConfig);
                        _LEDSequence[nIdx].setValue(false);
                        // And remember it as being already opened
                        OpenedDevices.put(currLEDConfig, _LEDSequence[nIdx]);
                    }
                    nIdx++;
                }
            } catch (IOException ex) {
                // In case of I/O error during a GPIOPin opening, stop application
                throw new RuntimeException("I/O Error while opening GPIOPin");
            }
        } else {
            // Otherwise, if there is no configuration at all, stop application
            throw new RuntimeException("No pin configuration found");
        }
        
        // Init the timer interval from MIDLet attribute
        int timerInterval;
        try {
            timerInterval = Integer.parseInt(getAppProperty(TIMER_INTERVAL_ATTRIBUTE));
        } catch (NumberFormatException nfe) {
            timerInterval = DEFAULT_TIMER_INTERVAL;
        }

        // Finally, create timer object and schedule the LED lighting Task using the LED sequence and timer interval
        _LEDLightingTimer = new Timer();
        System.out.println("Starting LED Timer");
        _LEDLightingTimer.scheduleAtFixedRate(new LEDLightingTimerTaskImpl(_LEDSequence), 0, timerInterval);
    }

    /** End of application.
     * @param unconditional */
    @Override
    public void destroyApp(boolean unconditional) {
        System.out.println("Stopping GPIOLedsDemo application");

        // Cancel timer
        if (_LEDLightingTimer != null) {
            _LEDLightingTimer.cancel();
            _LEDLightingTimer = null;
        }
        // Note that Timer.cancel() does not reliably work when called from a thread 
        // other than the Timer thread (here, destroyApp is called from the main 
        // MIDlet thread): it is not blocking if there is a currently running task, 
        // and so this last task will complete even after cancelation of timer.
        //
        // But this situation is handled correctly. The destroyApp method will 
        // first close all GPIOPins, hence triggering an IOException in Task thread,
        // that will cancel itself in this case

        // Close each LED GPIOPin device
        if (_LEDSequence != null) {
            for (GPIOPin currLED : _LEDSequence) {
                try {
                    if (currLED != null) {
                        currLED.setValue(false);
                        currLED.close();
                    }
                } catch (IOException ex) {
                    // Nothing to do in case of IO exception, simply continue
                }
            }

            _LEDSequence = null;
        }
    }

    /**
    * Utility method that parse a list of GPIOPinConfig from a string formated as 
    * following: "Port,Pin|Port,Pin|[...]". <p>
    * Note: created configurations are Output pins. 
    */
    private static ArrayList<GPIOPinConfig> parseLEDPinConfigs(String str) {

        // Final list of Pin configurations
        ArrayList<GPIOPinConfig> LEDPinConfigList = new ArrayList<>();

        // Split the input string using the "|" separator to have a list of "Port,Pin" 
        // strings
        StringTokenizer LEDsTokenizer = new StringTokenizer(str, "|");

        // For each of these strings
        while (LEDsTokenizer.hasMoreTokens()) {
            String currentLED = LEDsTokenizer.nextToken();

            // Split it using the "," separator to have the Port and Pin 
            // numbers
            StringTokenizer PortPinToken = new StringTokenizer(currentLED, ",");

            try {
                String PortStr = PortPinToken.nextToken();
                String PinStr = PortPinToken.nextToken();

                int port = Integer.parseInt(PortStr);
                int pin = Integer.parseInt(PinStr);

                // Using these parsed numbers, create a GPIOPinConfig as output pin
                LEDPinConfigList.add(new GPIOPinConfig(
                        port, pin,
                        GPIOPinConfig.DIR_OUTPUT_ONLY,
                        GPIOPinConfig.MODE_OUTPUT_PUSH_PULL,
                        GPIOPinConfig.TRIGGER_BOTH_EDGES,
                        false));
                System.out.format("parsed pin config: %d,%d\n", port, pin);
            } catch (NoSuchElementException | NumberFormatException ex) {
                System.out.println("Error while parsing pin configuration");
                // In case of parse error, skip the current config and continue to next
            }
        }

        return LEDPinConfigList;
    }

    /**
     * LED Lighting TimerTask implementation. <br>
     * The run method of this class implements the LED Lighting algorithm. It will be 
     * called by the LED Timer at each scheduled interval.
     */
    private static class LEDLightingTimerTaskImpl extends TimerTask {

        /** LEDs sequence, as an array of GPIOPin */
        private final GPIOPin[] _LEDSequence;
        
        /** Current LED to be activated (index in the LEDs sequence).
         * This value is incremented by the run method at each call, and reset to zero when 
         * the end of the sequence is reached. */
        private int _CurrentLEDIndex;
        /** Previous LED index, to be deactivated. */
        private int _PreviousLEDIndex;
        
        /** LED lighting task constructor. 
         * @param leds
         */
        public LEDLightingTimerTaskImpl(GPIOPin[] leds) {
            
            if (leds == null) {
                throw new NullPointerException();
            }
            if (leds.length == 0) {
                throw new IllegalArgumentException();
            }
            
            // Do a local copy of the sequence, this will prevent handling eventual 
            // concurrent access
            _LEDSequence = Arrays.copyOf(leds, leds.length);
            _CurrentLEDIndex = 0;
            _PreviousLEDIndex = -1;
        }

        /** run method implementation. */
        @Override
        public void run() {
            
            try {
                // Light down the previous LED
                if (_PreviousLEDIndex != -1) {
                    _LEDSequence[_PreviousLEDIndex].setValue(false);
                }
                // Light up the current one
                _LEDSequence[_CurrentLEDIndex].setValue(true);

                // Update previous index
                _PreviousLEDIndex = _CurrentLEDIndex;

                // Update current index
                // If we are not at the end of the array
                if (_CurrentLEDIndex < _LEDSequence.length - 1) {
                    // Increment the LED index so that the next LED of the array will 
                    // be activated at next run of the task
                    _CurrentLEDIndex++;
                } else {
                    // Otherwise, restart from begining at next run
                    _CurrentLEDIndex = 0;
                }
            } catch (IOException ex) {
                // In case of IO Error, cancel this timer task
                this.cancel();

                // This is a possible situation, for example when the application is 
                // destroyed: all GPIOPin are closed and an IO Exception will be 
                // thrown when Pin value will be set.
                System.out.println("IO Error while setting value to GPIOPin. Stopping task.");
            }
        }
    }
}
