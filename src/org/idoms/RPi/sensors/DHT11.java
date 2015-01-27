package org.idoms.RPi.sensors;

/**
 * Created by Luuk on 18/01/15.
 */
import java.util.concurrent.TimeUnit;

import com.pi4j.component.ObserveableComponentBase;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class DHT11 extends ObserveableComponentBase {

    private static final Pin DEFAULT_PIN = RaspiPin.GPIO_07;
    private static final int MAXTIMINGS = 85;
    private int[] dht11_dat = { 0, 0, 0, 0, 0 };
    private GpioPinDigitalMultipurpose dht11Pin;
    //private static final Logger LOGGER = LogManager.getLogger(DHT11.class.getName());

    public DHT11() {
        final GpioController gpio = GpioFactory.getInstance();
        dht11Pin = gpio.provisionDigitalMultipurposePin(DEFAULT_PIN,
                PinMode.DIGITAL_INPUT, PinPullResistance.PULL_UP);
    }
    /*
    public DHT11(int pin) {
        final GpioController gpio = GpioFactory.getInstance();
        dht11Pin = gpio.provisionDigitalMultipurposePin(Libpin.getPin(pin),
                PinMode.DIGITAL_INPUT, PinPullResistance.PULL_UP);
    }
    */

    public double getTemperature() {
        PinState laststate = PinState.HIGH;
        int j = 0;
        dht11_dat[0] = dht11_dat[1] = dht11_dat[2] = dht11_dat[3] = dht11_dat[4] = 0;
        StringBuilder value = new StringBuilder();
        try {

            dht11Pin.setMode(PinMode.DIGITAL_OUTPUT);
            dht11Pin.low();
            Thread.sleep(18);
            dht11Pin.high();
            TimeUnit.MICROSECONDS.sleep(40);
            dht11Pin.setMode(PinMode.DIGITAL_INPUT);

            for (int i = 0; i < MAXTIMINGS; i++) {
                int counter = 0;
                while (dht11Pin.getState() == laststate) {
                    counter++;
                    TimeUnit.MICROSECONDS.sleep(1);
                    if (counter == 255) {
                        break;
                    }
                }

                laststate = dht11Pin.getState();

                if (counter == 255) {
                    break;
                }

                /* ignore first 3 transitions */
                if ((i >= 4) && (i % 2 == 0)) {
                    /* shove each bit into the storage bytes */
                    dht11_dat[j / 8] <<= 1;
                    if (counter > 16) {
                        dht11_dat[j / 8] |= 1;
                    }
                    j++;
                }
            }
            // check we read 40 bits (8bit x 5 ) + verify checksum in the last
            // byte
            if ((j >= 40) && checkParity()) {
                value.append(dht11_dat[2]).append(".").append(dht11_dat[3]);
                System.out.println("temperature value readed: " + value.toString());
                //LOGGER.info("temperature value readed: " + value.toString());
            }

        } catch (InterruptedException e) {
            System.out.println("InterruptedException: " + e.getMessage());
            //LOGGER.error("InterruptedException: " + e.getMessage(), e);
        }
        if (value.toString().isEmpty()) {
            value.append(-1);
        }
        return Double.parseDouble(value.toString());
    }

    private boolean checkParity() {
        return (dht11_dat[4] == ((dht11_dat[0] + dht11_dat[1] + dht11_dat[2] + dht11_dat[3]) & 0xFF));
    }

}
