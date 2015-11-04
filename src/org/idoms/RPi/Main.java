package org.idoms.RPi;

import com.pi4j.io.gpio.RaspiPin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
 * GPIO 5? (pin 18) - PI4J 5 (used if TFT is used)
 * GPIO 6? (pin 22) - PI4J 6 (used if TFT is used)
 *
 */

public class Main {

    // Flash on communication, can be disabled if annoying (but will flash on error)
    public static boolean statusLed = true;
    public static boolean debug = false;
    public static boolean display = false;
    public static String port = "";

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

            String portProp = System.getProperty("port");
            if (portProp != null) {
                port = ":" + portProp;
            }


            String displayProp = System.getProperty("display");
            if (displayProp != null) {
                if (displayProp.equalsIgnoreCase("1")) {
                    try {
                        Runtime rt = Runtime.getRuntime();
                        String[] initCmd = {"/bin/sh", "-c", "echo 252 > /sys/class/gpio/export"};
                        Process pr = rt.exec(initCmd);

                        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                        String line;
                        while ((line = input.readLine()) != null) {
                            if (debug) System.out.println("Error:" + line);
                        }

                        pr.waitFor();
                        if (debug) System.out.println("Tried to init display: " + pr.exitValue());

                        String[] directionCmd = {"sh", "-c", "echo 'out' > /sys/class/gpio/gpio252/direction"};
                        pr = rt.exec(directionCmd);

                        input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

                        while ((line = input.readLine()) != null) {
                            System.out.println("Error: " + line);
                        }

                        pr.waitFor();
                        if (debug) System.out.println("Tried to init display out direction: " + pr.exitValue());

                        String[] cmd = {"sh", "-c", "echo '1' > /sys/class/gpio/gpio252/value"};
                        rt.exec(cmd);

                        String[] checkCmd = {"ls", "-l", "/sys/class/gpio"};
                        pr = rt.exec(checkCmd);

                        input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

                        while ((line = input.readLine()) != null) {
                            //if (debug) System.out.println("Check: " + line);
                            if (line.contains("gpio252")) {
                                display = true;
                                System.out.println("Display initialised!");
                                break;
                            }
                        }

                        if (!display) {
                            display = false;
                            System.out.println("WARNING: flag was set for display, but could not initialise it seems");
                        } else {
                            rt.exec("sh -c \"echo 'out' > /sys/class/gpio/gpio252/direction\"");
                            rt.exec("sh -c \"echo '1' > /sys/class/gpio/gpio252/value\"");
                        }
                    } catch (Exception e){
                        System.out.println("Error during display init: " + e.getLocalizedMessage());
                        display = false;
                    }
                }
            }

            // Find server IP
            FindUDPServer udpFinder = new FindUDPServer();
            address = udpFinder.searchForServer();

            if (address == null) {
                System.out.println("Problem finding the server with unrecoverable error, please look at the log!");
                System.exit(-1);
            }

            System.out.println("IP address of server is " + address.getHostAddress());

            Thread t = new Thread(new ServerThread(address));
            t.start();

            // Add button listeners
            GpioInterface gpio = GpioInterface.getInstance();
            gpio.addButtonListener(RaspiPin.GPIO_01);
            gpio.addButtonListener(RaspiPin.GPIO_02);
            gpio.addButtonListener(RaspiPin.GPIO_03);
            gpio.addButtonListener(RaspiPin.GPIO_04);
        } catch (Exception e) {
            System.out.println("Exception during main procedure and quiting: " + e.getLocalizedMessage());
        }
    }

}

