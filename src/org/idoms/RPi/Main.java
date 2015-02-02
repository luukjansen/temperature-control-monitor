package org.idoms.RPi;

import com.pi4j.io.gpio.RaspiPin;

import java.net.*;

/**
 * GPIO Port usage
 *
 * GPIO 18 (pin 12) - PI4J 1 (reserved for button)
 * GPIO 21 (pin 13) - PI4J 2 (reserved for button)
 * GPIO 22 (pin 15) - PI4J 3 (reserved for button)
 * GPIO 23 (pin 16) - PI4J 4 (reserved for button)
 *
 * GPIO 0? (pin 11) - PI4J 0 (status light)
 *
 * GPIO 7? (pin 7)  - PI4J 7 (sensor)
 * GPIO 5? (pin 18) - PI4J 5 (sensor)
 * GPIO 6? (pin 22) - PI4J 6 (sensor)
 *
 */

public class Main {

    // Flash on communication, can be disabled if annoying (but will flash on error)
    private static boolean statusLed = true;
    private static boolean debug = false;

    public static InetAddress address = null;

    public static void main(String[] args) {
        // Hook to catch the shutdown, so we can stop the actions
        try {
            Runtime.getRuntime().addShutdownHook(new ShutdownThread());
            System.out.println("[Main thread] Shutdown hook added");
        } catch (Throwable t) {
            // we get here when the program is run with java
            // version 1.2.2 or older
            System.out.println("[Main thread] Could not add Shutdown hook");
        }


        // We can now start the normal procedure.
        try {
            String debugProp = System.getProperty("debug");
            if (debugProp != null) {
                if (debugProp.equalsIgnoreCase("1")) {
                    System.out.println("(In debug mode)");
                    debug = true;
                }
            }

            String statusLedProp = System.getProperty("statusLed");
            if (statusLedProp != null) {
                if (statusLedProp.equalsIgnoreCase("0")) {
                    statusLed = false;
                }
            }

            // Find server IP
            FindUDPServer udpFinder = new FindUDPServer();
            address = udpFinder.searchForServer(debug);

            if (address == null) {
                System.out.println("Problem finding the server with unrecoverable error, please look at the log!");
                System.exit(-1);
            }

            System.out.println("IP address of server is " + address.getHostAddress());

            Thread t = new Thread(new ServerThread(address, debug, statusLed));
            t.start();

            // Add button listeners
            GpioInterface gpio = GpioInterface.getInstance(debug);
            gpio.addButtonListener(RaspiPin.GPIO_01);
            gpio.addButtonListener(RaspiPin.GPIO_02);
            gpio.addButtonListener(RaspiPin.GPIO_03);
            gpio.addButtonListener(RaspiPin.GPIO_04);
        } catch (Exception e) {
            System.out.println("Exception during main procedure and quiting: " + e.getLocalizedMessage());
        }
    }

}

