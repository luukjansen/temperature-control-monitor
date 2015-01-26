package org.idoms.RPi;

import java.net.*;

public class Main {

    private static boolean debug = true;

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

            // Find server IP
            FindUDPServer udpFinder = new FindUDPServer();
            InetAddress address = udpFinder.searchForServer(debug);

            if (address == null) {
                System.out.println("Problem finding the server with unrecoverable error, please look at the log!");
                System.exit(-1);
            }

            System.out.println("IP address of server is " + address.getHostAddress());

            Thread t = new Thread(new ServerThread(address, debug));
            t.start();
        } catch (Exception e) {
            System.out.println("Exception during main procedure and quiting: " + e.getLocalizedMessage());
        }
    }

}

