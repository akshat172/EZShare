
package EZShare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser; 
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class serverInteractions {

public void secureExchange() throws IOException {
        //IF the server contains itself in the serverrecords list, then remove it
            JSONObject serverTraverser = new JSONObject();
            for (int i = 0; i < Server.secureServerRecords.size(); i++) {
                serverTraverser = (JSONObject) Server.secureServerRecords.get(i);
                if (serverTraverser.get("hostname").equals(Server.host) )
                {
                    if(serverTraverser.get("port").equals(Server.port) || serverTraverser.get("port").equals(Server.sport))
                    { Server.secureServerRecords.remove(i);} 
                    
                }
            }
            
            
            
        if (Server.secureServerRecords.isEmpty()) {
            Server.debug("SECURE-INTERVAL-INFO", "server exchange initiated: empty server record list.");
        } else {
            
            
            
            //Pick a random server to connect from the list
            Server.debug("SECURE-INTERVAL-INFO", "server exchange initiated: randomly selecting a server");
            Random r = new Random();
            JSONObject randomServer = new JSONObject();
            int size = Integer.valueOf(Server.secureServerRecords.size());
            int index = Integer.valueOf(r.nextInt(size));
            randomServer = (JSONObject) Server.secureServerRecords.get(index);
            String connect_host = randomServer.get("hostname").toString();
            int connect_port = ((Long) randomServer.get("port")).intValue();

             
                 
 
            // Create connection with the selected server from the serverlist
            
                
                SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                
                  Boolean checkSecure = false;
             try (SSLSocket socket = (SSLSocket) sslsocketfactory.createSocket(connect_host, connect_port)) {
                  
                //Create buffered writer to send data to the server
                OutputStream serverOutput = socket.getOutputStream();
                OutputStreamWriter outputstreamwriter = new OutputStreamWriter(serverOutput);
                BufferedWriter bufferedwriter = new BufferedWriter(outputstreamwriter);

                
                /*

                6 - Sending the list to the randomly selected server

                 */

                JSONObject listToRandomServer = new JSONObject();
                listToRandomServer.put("command", "EXCHANGE");
                listToRandomServer.put("serverList", Server.secureServerRecords);
                 socket.setSoTimeout(2000);
                secureOutput(listToRandomServer, bufferedwriter);
                checkSecure = true;
                
                //Create buffered reader to read input  
                InputStream inputFromRandom = socket.getInputStream();
                InputStreamReader inputstreamreader = new InputStreamReader(inputFromRandom);
                BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
                String message = bufferedreader.readLine();
                Server.debug("SECURE-INTERVAL-RECEIVE",  message);
                
                
                // Display distinct servers in the serverRecords
                Set<String> setWithUniqueValues = new HashSet<>(Server.secureServerRecords);
                ArrayList<String> listWithUniqueValues = new ArrayList<>(setWithUniqueValues);
                Server.secureServerRecords = listWithUniqueValues;
                socket.close();
                Server.debug("SECURE-INTERVAL-INFO", "exchange with " + connect_host + ":" + connect_port + " on a secure channel is successful.");

            } catch (Exception e) {
                // If the connection with the random server is not established
                Server.debug("SECURE-INTERVAL-INFO","connection with server " + connect_host + ":" + connect_port + " was not successful. ");
                Server.debug("SECURE-INTERVAL-INFO","error message : "  + e);
               
                serverTraverser = new JSONObject();

                for (int i = 0; i < Server.secureServerRecords.size(); i++) {
                    serverTraverser = (JSONObject) Server.secureServerRecords.get(i);
                    if (serverTraverser.get("hostname").equals(connect_host) && serverTraverser.get("port").equals(connect_port)) {
                        Server.secureServerRecords.remove(i);
                Server.debug("SECURE-INTERVAL-INFO","server has been removed");
               
                    }
                }
            } finally {

            }
        }
    }
public void exchange() throws IOException {
        //IF the server contains itself in the serverrecords list, then remove it
            JSONObject serverTraverser = new JSONObject();
            for (int i = 0; i < Server.serverRecords.size(); i++) {
                serverTraverser = (JSONObject) Server.serverRecords.get(i);
                if (serverTraverser.get("hostname").equals(Server.host) )
                {
                    if(serverTraverser.get("port").equals(Server.port))
                    { Server.serverRecords.remove(i);} 
                    
                }
            }
            
            
            
        if (Server.serverRecords.isEmpty()) {
            Server.debug("INTERVAL-INFO", "server exchange initiated: empty server record list.");
        } else {
            
            
            
            //Pick a random server to connect from the list
            Server.debug("INTERVAL-INFO", "server exchange initiated: randomly selecting a server");
            Random r = new Random();
            JSONObject randomServer = new JSONObject();
            int size = Integer.valueOf(Server.serverRecords.size());
            int index = Integer.valueOf(r.nextInt(size));
            randomServer = (JSONObject) Server.serverRecords.get(index);
            String connect_host = randomServer.get("hostname").toString();
            int connect_port = ((Long) randomServer.get("port")).intValue();

            // Create connection with the selected server from the serverlist
            try {
                Socket socket = null;
                socket = new Socket(connect_host, connect_port);
               
                // Sending the list to the randomly selected server
                // Receiving ackowledgement (success)/(error)                
                JSONObject listToRandomServer = new JSONObject();
                listToRandomServer.put("command", "EXCHANGE");
                listToRandomServer.put("serverList", Server.serverRecords);
                DataOutputStream serverOutput = new DataOutputStream(socket.getOutputStream());
                serverOutput.writeUTF(listToRandomServer.toJSONString());
                Server.debug("INTERVAL-SEND", listToRandomServer.toJSONString());
                DataInputStream serverInput = new DataInputStream(socket.getInputStream());
                String message =  serverInput.readUTF();
                JSONParser parser = new JSONParser();
                JSONObject JSONresponse = (JSONObject) parser.parse(message);
                Server.debug("INTERVAL-RECEIVE",  JSONresponse.toJSONString());
                
                
                // Display distinct servers in the serverRecords
                Set<String> setWithUniqueValues = new HashSet<>(Server.serverRecords);
                ArrayList<String> listWithUniqueValues = new ArrayList<>(setWithUniqueValues);
                Server.serverRecords = listWithUniqueValues;
                socket.close();
                 String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                System.out.println(time + " - [INFO] - exchange with " + connect_host + ":" + connect_port + " is successful.");
                
            } catch (Exception e) {
                // If the connection with the random server is not established
                Server.debug("INTERVAL-INFO","connection with server " + connect_host + ":" + connect_port + " was not successful. ");
               
                serverTraverser = new JSONObject();

                for (int i = 0; i < Server.serverRecords.size(); i++) {
                    serverTraverser = (JSONObject) Server.serverRecords.get(i);
                    if (serverTraverser.get("hostname").equals(connect_host) && serverTraverser.get("port").equals(connect_port)) {
                        Server.serverRecords.remove(i);
                Server.debug("INTERVAL-INFO","server has been removed");
               
                    }
                }
            } finally {

            }
        }
    }
 private void secureOutput(JSONObject response, BufferedWriter output) throws IOException {

        output.write(response.toJSONString() + '\n');
        output.flush();
        Server.debug("SECURE-INTERVAL-SEND", response.toJSONString());
    }

 
}
