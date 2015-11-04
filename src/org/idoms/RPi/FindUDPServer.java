package org.idoms.RPi;

import java.net.*;
import java.util.Enumeration;

/**
 * Created by Luuk on 25/01/15.
 */
public class FindUDPServer {

    public InetAddress searchForServer() throws Exception {

        final GpioInterface gpio = GpioInterface.getInstance();
        gpio.switchOn(GpioInterface.STATUS_PIN);

        System.out.println("Trying to discover the server...");

        //pin.switchOff();
        //pin.flash(250, 2000);

        InetAddress address = null;
        gpio.flash(GpioInterface.STATUS_PIN, 0);
        gpio.flash(GpioInterface.STATUS_PIN, 250);
        while (address == null) {
            try {
                address = findServer();
            } catch (Exception ex) {
                System.out.println("Error in discovering server IP: " + ex.getLocalizedMessage());
            }

            if (address == null) {
                System.out.println("Trying again in 30 seconds to find the server");
                Thread.sleep(30000);
            }
        }

        gpio.flash(GpioInterface.STATUS_PIN, 0);
        gpio.releasePin(GpioInterface.STATUS_PIN);
        return address;

    }

    private InetAddress findServer() throws Exception {
        InetAddress serverAddress = null;

        //Open a random port to send the package
        DatagramSocket c = new DatagramSocket();
        c.setSoTimeout(10000);

        // Find the server using UDP broadcast
        try {
            c.setBroadcast(true);

            byte[] sendData = "DISCOVER_TEMPSERVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                c.send(sendPacket);
                if(Main.debug) System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
            }

            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                    } catch (Exception e) {
                    }

                    if(Main.debug)System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            if(Main.debug)System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.receive(receivePacket);

            //We have a response
            System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals("DISCOVER_TEMPSERVER_RESPONSE")) {
                //DO SOMETHING WITH THE SERVER'S IP (for example, store it in your controller)
                return receivePacket.getAddress();
            } else {
                throw new Exception("Could not detect the server IP for unknown reason???");
            }
        } finally {
            c.close();
        }
    }

}
