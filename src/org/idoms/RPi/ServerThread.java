package org.idoms.RPi;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Luuk on 26/01/15.
 */
public class ServerThread implements Runnable {

    private static int sleepTime = 30;

    private InetAddress address;
    private boolean keepRunning = true;

    public ServerThread(InetAddress address) {
        this.address = address;
    }

    public void stopThread() {
        keepRunning = false;
        System.out.println("Will shutdown on next iteration...");
    }

    @Override
    public void run() {
        // Makes it easier
        if (Main.debug) sleepTime = 10;
        GpioInterface gpio = GpioInterface.getInstance();
        ServerConnection serverConnection = new ServerConnection(address);

        try {
            while (keepRunning) {
                Calendar calendar = Calendar.getInstance();
                try {
                    serverConnection.contactServer();
                } finally {
                    gpio.releasePin(GpioInterface.STATUS_PIN);
                    calendar.add(Calendar.SECOND, sleepTime);
                    long waitTime = calendar.getTimeInMillis() - new Date().getTime();
                    if (waitTime > 0) Thread.sleep(waitTime);
                    gpio.flash(GpioInterface.STATUS_PIN, 0);
                }
            }
        } catch (Exception e) {
            gpio.turnAllActivePinsOff();
            gpio.flash(GpioInterface.STATUS_PIN, 250);
            System.out.println("The server sync thread has closed/crashed... This is not good: " + e.getLocalizedMessage());
        }
    }

}
