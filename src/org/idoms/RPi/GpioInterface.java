package org.idoms.RPi;

import com.pi4j.io.gpio.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Luuk on 19/01/15.
 */
public class GpioInterface {

    private static GpioInterface instance = null;
    private final GpioController gpio = GpioFactory.getInstance();

    public List<Pin> usedControlPins = new ArrayList<Pin>();

    public static final Pin STATUS_PIN = RaspiPin.GPIO_01;

    public static GpioInterface getInstance() {
        if(instance == null) {
           instance = new GpioInterface();
        }
        return instance;
    }

    private GpioInterface() {

    }

    public void stop(){
         gpio.shutdown();
    }

    private GpioPinDigitalOutput getPin(Pin pin){
        GpioPinDigitalOutput outputPin = null;
        for (GpioPin provPin : gpio.getProvisionedPins()){
            if(provPin.getPin().equals(pin)){
                outputPin = (GpioPinDigitalOutput) provPin;
                break;
            }
        }

        if(outputPin == null)outputPin = gpio.provisionDigitalOutputPin(pin);
        return outputPin;
    }

    public void releasePin(Pin pin){
        gpio.unprovisionPin(getPin(pin));
    }

    public void switchOn(Pin pin){
        getPin(pin).high();
    }

    public void switchOff(Pin pin){
        getPin(pin).low();
    }

    public void flash(Pin pin, int time){
        getPin(pin).blink(time);
    }

    public void flash(Pin pin, int time, int duration){
        getPin(pin).blink(time, duration);
    }

    public boolean switchStatus(Pin pin){
        return getPin(pin).isHigh();
    }

    public void turnAllActivePinsOff(){
        GpioInterface gpio = GpioInterface.getInstance();
        // Set all pins to off
        for(Pin usedPin: gpio.usedControlPins){
            gpio.switchOff(usedPin);
        }
    }

    public static void unProvisionAllPins(){
        GpioController gpio = GpioFactory.getInstance();
        List<GpioPin> list = new ArrayList<GpioPin>(gpio.getProvisionedPins());

        while(list.size() > 0) {
            gpio.unprovisionPin(list.get(0));
            list = new ArrayList<GpioPin>(gpio.getProvisionedPins());
        }

    }

}
