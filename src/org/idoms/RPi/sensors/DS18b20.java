package org.idoms.RPi.sensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

/**
 * Created by Luuk on 19/01/15.
 */
public class DS18b20 {

    public DS18b20 (){

    }

    public static float readTemp(String sensorPath) throws Exception {

        String line;
        String sTemp = "999";
        boolean firstLine = true;

        BufferedReader bufferedReader = new BufferedReader(new FileReader(sensorPath));
        while ((line = bufferedReader.readLine()) != null) {
            if (firstLine) {
                if (!line.contains("YES")) throw new Exception("Sensor fault: " + line);
                firstLine = false;
            } else {
                sTemp = line.substring(line.indexOf("t=") + 2);
            }
        }
        bufferedReader.close();
        return Float.parseFloat(sTemp) / 1000;

    }

}
