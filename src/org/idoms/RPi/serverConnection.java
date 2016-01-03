package org.idoms.RPi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.SystemInfo;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Luuk on 30/01/15.
 */
public class ServerConnection {

    private static final String encryptionKey = "TTksVT7gRCnXnpXvE4xstrK5JyNZwjLIQ9t9axlr";

    private InetAddress address;

    public ServerConnection(InetAddress address){
        this.address = address;
    }

    public void contactServer() throws Exception {
        contactServer(new HashMap<>());
    }

    public void contactServer(HashMap<String, Float> sensors) throws Exception {
        GpioInterface gpio = GpioInterface.getInstance();
        try {
            if(Main.statusLed) gpio.switchOn(GpioInterface.STATUS_PIN);
            // Sent to server

            // Connect to HTTP
            RequestConfig config = RequestConfig.custom().setConnectTimeout(5000).build();
            CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

            String data = getDataJsonString(sensors);
            //if (debug) System.out.println(data);
            HttpGet httpget = new HttpGet("http://" + address.getHostAddress() + Main.port + "/api/sensorData?data=" + Base64.encodeBase64URLSafeString(encrypt("{" + data + "}").getBytes()));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 400) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };

            String responseBody = httpclient.execute(httpget, responseHandler);

            if (Main.debug) System.out.println("Response: " + responseBody);

            // general method, same as with data binding
            ObjectMapper listMapper = new ObjectMapper();
            JsonNode listRootNode = listMapper.readValue(responseBody, JsonNode.class); // src can be a File, URL, InputStream etc

            if (!listRootNode.get("status").asText().equalsIgnoreCase("SUCCESS"))
                throw new Exception("Server returned: " + listRootNode.get("status").asText());

            for (JsonNode node : listRootNode.get("actions")) {
                if (!node.has("action")) {
                    System.out.println("Problem, there seems an action without the action element...");
                    continue;
                }

                String action = node.get("action").asText();
                if (action.equalsIgnoreCase("setLow") || action.equalsIgnoreCase("setHigh")) {
                    Pin pin = getPinFromNumber(node.get("pin").asInt());

                    if (!gpio.usedControlPins.contains(pin)) {
                        gpio.usedControlPins.add(pin);
                        if (Main.debug) System.out.println("Logging pin " + pin + " as used");
                    }

                    if (action.equalsIgnoreCase("setHigh")) {
                        gpio.switchOn(pin);
                        gpio.releasePin(pin);
                    } else if (action.equalsIgnoreCase("setLow")) {
                        gpio.switchOff(pin);
                        gpio.releasePin(pin);
                    } else {
                        throw new Exception("Unknown action: " + action);
                    }

                } else if (action.equalsIgnoreCase("turnOnDisplay")) {
                    Main.turnDisplayOn();
                } else if (action.equalsIgnoreCase("turnOffDisplay")) {
                    Main.turnDisplayOff();
                } else if (action.equalsIgnoreCase("switchDisplay")) {
                    Main.switchDisplay();
                }
            }
            gpio.switchOff(GpioInterface.STATUS_PIN);
        } catch (IOException ioe) {
            gpio.turnAllActivePinsOff();

            System.out.println("Problem with the connection: " + ioe.getMessage());
            ioe.printStackTrace(System.out);

            // Try to get an IP request again. (maybe this should be selected more intelligent, could have all kinds of reasons)
            FindUDPServer udpFinder = new FindUDPServer();
            address = udpFinder.searchForServer();

            if (address == null) {
                System.out.println("Problem finding the server with unrecoverable error, please look at the log!");
                System.exit(-1);
            }
        } catch (SensorException ex){
            throw (ex);
        } catch (Exception ex) {
            System.out.println("Problem processing in/after connection with " + address.getHostAddress() + ": " + ex.getMessage());
            ex.printStackTrace(System.out);
            gpio.flash(GpioInterface.STATUS_PIN, 0);
            gpio.flash(GpioInterface.STATUS_PIN, 250);
        }
    }

    private String getDataJsonString() throws Exception {
        return getDataJsonString(new HashMap<String, Float>());
    }

    private String getDataJsonString(HashMap<String, Float> sensorList) throws Exception {
        File sensorDir = new File("/sys/bus/w1/devices/");

        if (sensorDir.exists()) {
            for (File file : sensorDir.listFiles()) {
                if (file.isDirectory()) {
                    if (file.getName().startsWith("28-")) {
                        // This is a sensor.
                        String serial = file.getName().substring(3);

                        float temp = DS18b20.readTemp(sensorDir.getAbsolutePath() + "/28-" + serial + "/w1_slave");
                        if (Main.debug) System.out.println("Temp: " + temp + " for sensor " + serial);
                        sensorList.put(serial, temp);
                    }
                }
            }

        } else {
            throw new Exception("Cannot find the sensor prob dir. Have 'modprobe w1-gpio' and 'sudo modprobe w1-therm' be ran?");
        }

        String JSON = "\"status\":\"OK\"" +
                ", \"sensors\":[";

        boolean firstSensor = true;
        for (Map.Entry<String, Float> entry : sensorList.entrySet()) {
            if (!firstSensor) JSON += ",";
            JSON += "{\"sensor\":\"" + entry.getKey() + "\",\"value\":" + entry.getValue() + "}";
            firstSensor = false;
        }

        JSON += "], \"serial\":\"" + SystemInfo.getSerial() + "\"";

        return JSON;
    }

    private static String decrypt(String encrypted) throws Exception {
        DESKeySpec keySpec = new DESKeySpec(encryptionKey.getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);

        BASE64Decoder base64decoder = new BASE64Decoder();
        byte[] encrypedBytes = base64decoder.decodeBuffer(encrypted);

        // Decrypt cipher
        Cipher cipher = Cipher.getInstance("DES");// cipher is not thread safe
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = (cipher.doFinal(encrypedBytes));

        return new String(plainText);
    }

    private static String encrypt(String data) throws Exception {
        DESKeySpec keySpec = new DESKeySpec(encryptionKey.getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);
        //BASE64Encoder base64encoder = new BASE64Encoder();

        // ENCODE plainTextPassword String
        byte[] cleartext = data.getBytes("UTF8");

        Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encodeBase64String(cipher.doFinal(cleartext));
    }

    private static Pin getPinFromNumber(int pinNr) throws Exception {
        switch (pinNr) {
            case 0:
                return RaspiPin.GPIO_00;
            case 1:
                return RaspiPin.GPIO_01;
            case 2:
                return RaspiPin.GPIO_02;
            case 3:
                return RaspiPin.GPIO_03;
            case 4:
                return RaspiPin.GPIO_04;
            case 5:
                return RaspiPin.GPIO_05;
            case 6:
                return RaspiPin.GPIO_06;
            case 7:
                return RaspiPin.GPIO_07;
            case 8:
                return RaspiPin.GPIO_08;
            case 9:
                return RaspiPin.GPIO_09;
            case 10:
                return RaspiPin.GPIO_10;
            case 11:
                return RaspiPin.GPIO_11;
            case 12:
                return RaspiPin.GPIO_12;
            case 13:
                return RaspiPin.GPIO_13;
            case 14:
                return RaspiPin.GPIO_14;
            case 15:
                return RaspiPin.GPIO_15;
            case 16:
                return RaspiPin.GPIO_16;
            case 17:
                return RaspiPin.GPIO_17;
            case 18:
                return RaspiPin.GPIO_18;
            case 19:
                return RaspiPin.GPIO_19;
            case 20:
                return RaspiPin.GPIO_20;
            default:
                throw new Exception("Unknown pin number!");
        }
    }
}
