
package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import org.json.simple.JSONObject;
import Utilities.Resource;
import java.io.BufferedReader;
import jdk.internal.org.objectweb.asm.util.CheckAnnotationAdapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

public class clientCommands {
 
    //This function will receive the query list from 
    public void securequery(JSONObject command, BufferedReader input) throws IOException, ParseException {

        Integer size = -1;
        Integer resultSize;
        Long result;
        Boolean done = false;
        Integer c = 0;

        while (c < 3) {
            c += 1;
            String string = null;
            if ((string = input.readLine()) != null) {
                String serverResponse = string;
                JSONParser parser = new JSONParser();
                JSONObject response = (JSONObject) parser.parse(serverResponse);
                Client.debug("RECEIVE", response.toJSONString());

                size += 1;

                if (response.containsKey("resultSize")) {
                    result = (Long) response.get("resultSize");
                    resultSize = Integer.valueOf(result.intValue());
                    if (size == resultSize || resultSize == 0) {
                        done = true;
                    }
                }
            }

            if (done) {
                //     break;
            }

        }

    }

    public void fetch(JSONObject response, DataInputStream input) throws FileNotFoundException, IOException, ParseException {

        if (response.get("response").equals("error")) {
        } else {
            //if the resource received is a success
            String serverResponse = input.readUTF();
            JSONParser parser = new JSONParser();
            JSONObject resource = (JSONObject) parser.parse(serverResponse);
            Client.debug("RECEIVE", resource.toJSONString());

            //if the name is not provided, an automatic file name is created
            String filename = (String) resource.get("name");
            String uri = (String) resource.get("uri");
            
            String ext = uri.substring(uri.lastIndexOf(".") + 1).trim();
            
            if (filename.equals("")) {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH.mm.ss"));
                filename = "File_" + time;
            }
            
            // receive the file
            RandomAccessFile downloadingFile = new RandomAccessFile(filename+"."+ext,"rw");
            long fileSizeRemaining = (Long) resource.get("resourceSize");
            int chunkSize = 1024 * 1024;
            if (fileSizeRemaining < chunkSize) {
                chunkSize = (int) fileSizeRemaining;
            }
            byte[] receiveBuffer = new byte[chunkSize];
            int num;
            Client.debug("INFO", "Ready to receive file");
            Client.debug("INFO", "File size:" + fileSizeRemaining);
            while ((num = input.read(receiveBuffer)) > 0) {
                downloadingFile.write(Arrays.copyOf(receiveBuffer, num));
                fileSizeRemaining -= num;
                chunkSize = 1024 * 1024;
                if (fileSizeRemaining < chunkSize) {
                    chunkSize = (int) fileSizeRemaining;
                    receiveBuffer = new byte[chunkSize];
                    if (fileSizeRemaining == 0) {
                        break;
                    }
                }
            }
            Client.debug("INFO", "file is downloaded");
            downloadingFile.close();
            serverResponse = input.readUTF();
            response = (JSONObject) parser.parse(serverResponse);
            Client.debug("RECEIVE", response.toJSONString());

        }
    }

    //This function will receive the query list from 
    public void query(JSONObject command, DataInputStream input) throws IOException, ParseException {

        Integer size = -1;
        Integer resultSize;
        Long result;
        Boolean done = false;
        Integer c = 0;

        while (c < 3) {
            c += 1;
            if (input.available() > 0) {
                String serverResponse = input.readUTF();
                JSONParser parser = new JSONParser();
                JSONObject response = (JSONObject) parser.parse(serverResponse);
                Client.debug("RECEIVE", response.toJSONString());

                size += 1;

                if (response.containsKey("resultSize")) {
                    result = (Long) response.get("resultSize");
                    resultSize = Integer.valueOf(result.intValue());
                    if (size == resultSize || resultSize == 0) {
                        done = true;
                    }
                }
            }

            if (done) {
                //     break;
            }

        }

    }

}
