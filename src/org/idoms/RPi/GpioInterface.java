package org.idoms.RPi;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.system.SystemInfo;
import com.sun.corba.se.spi.activation.Server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Luuk on 19/01/15.
 */
public class GpioInterface {

    private static GpioInterface instance = null;
    private final GpioController gpio = GpioFactory.getInstance();

    public List<Pin> usedControlPins = new ArrayList<Pin>();
    public List<GpioPinDigitalInput> registeredInputs = new ArrayList<GpioPinDigitalInput>();

    public static final Pin STATUS_PIN = RaspiPin.GPIO_00;

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

        if(outputPin == null) {
            outputPin = gpio.provisionDigitalOutputPin(pin);
            if(Main.debug) {
                System.out.println("Provisioning pin " + pin + " as it was not yet provisioned!");
                //Exception e = new Exception("Happened at.");
                //e.printStackTrace(System.out);
            }
        } else {
            if(!outputPin.isMode(PinMode.DIGITAL_OUTPUT)){
                outputPin.setMode(PinMode.DIGITAL_OUTPUT);
            } else {
                if (Main.debug) {
                    System.out.println("Pin " + pin + "  was already provisioned!");
                    //Exception e = new Exception("Happened at.");
                    //e.printStackTrace(System.out);
                }
            }
        }
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

    public void addButtonListener(Pin pin) {
        // provision gpio pin #02 as an input pin with its internal pull down resistor enabled
        final GpioPinDigitalInput button = gpio.provisionDigitalInputPin(pin, PinPullResistance.PULL_UP);

        // create and register gpio pin listener
        button.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                if(Main.debug) System.out.println("GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());

                // Sent to server.
                try {
                    if(Main.address == null) {
                        System.out.println("Could not sent action to the server. No address given...");
                        flash(STATUS_PIN, 250, 2000);
                        return;
                    }
                    ServerConnection connection = new ServerConnection(Main.address);

                    HashMap<String, Float> sensors = new HashMap<String, Float>();
                    sensors.put("action_" + SystemInfo.getSerial() + "_" + event.getPin().getName(), event.getState().isHigh()?1f:0f);

                    connection.contactServer(sensors);
                } catch (Exception e){
                    System.out.println("Error trying to contact the server after event on input " + event.getPin() + " message: " + e.getLocalizedMessage());
                }
            }

        });

        registeredInputs.add(button);
        if(Main.debug) System.out.println("Button registered for pin " + pin);
    }

}
