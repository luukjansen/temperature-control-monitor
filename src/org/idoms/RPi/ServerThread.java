package org.idoms.RPi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.SystemInfo;
import com.sun.javafx.beans.annotations.NonNull;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.idoms.RPi.exceptions.SensorException;
import org.idoms.RPi.sensors.DS18b20;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Luuk on 26/01/15.
 */
public class ServerThread implements Runnable {

    private static int sleepTime = 30;

    private InetAddress address;
    private boolean keepRunning = true;
    private boolean debug = false;
    private boolean statusLed = true;

    public ServerThread(InetAddress address) {
        this.address = address;
    }

    public ServerThread(InetAddress address, boolean debug, boolean statusLed) {
        this.address = address;
        this.statusLed = statusLed;
        this.debug = debug;
    }

    public void stopThread() {
        keepRunning = false;
        System.out.println("Will shutdown on next iteration...");
    }

    @Override
    public void run() {
        // Makes it easier
        if (debug) sleepTime = 5;
        GpioInterface gpio = GpioInterface.getInstance(debug);
        ServerConnection serverConnection = new ServerConnection(address, debug, statusLed);

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
