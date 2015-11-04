package org.idoms.RPi;

import com.pi4j.io.gpio.Pin;

/**
 * Created by Luuk on 26/01/15.
 */

// The ShutdownThread is the thread we pass to the
// addShutdownHook method
class ShutdownThread extends Thread {

    public ShutdownThread() {
        super();
    }

    public void run() {
        System.out.println("[Shutdown thread] Shutting down");

        GpioInterface gpio = GpioInterface.getInstance();
        gpio.turnAllActivePinsOff();
        gpio.switchOff(GpioInterface.STATUS_PIN);
        gpio.releasePin(GpioInterface.STATUS_PIN);

        System.out.println("[Shutdown thread] Shutdown complete");
    }
}
