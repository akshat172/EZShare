/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EZShare;

/**
 *
 * @author alisha
 */
//package client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Utilities.Resource;


public class SubscribeCommandConnection {
	static Integer size = -1;
	static Integer resultSize;
	static Long result;
	static Boolean done = false;
	static Integer c = 0;
	static int flag=0;
	public static void establishPersistentConnection(Boolean secure,int port,String ip,int id,JSONObject unsubscribJsonObject,String commandname,String name , String owner, String description, String channel, String uri, List<String> tags, String ezserver, String secret, Boolean relay,  String servers) throws URISyntaxException {
		//secure = false;
		try {
                        System.out.print(port+ip);
			Socket socket = null;
			if (secure) {

				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				socket = (SSLSocket) sslsocketfactory.createSocket(ip, port);
			} else {
				socket=new Socket(ip,port);
			}
			BufferedReader Reader = new BufferedReader(new InputStreamReader(System.in));
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			DataInputStream input = new DataInputStream(socket.getInputStream());

			JSONObject command = new JSONObject();
			Resource resource = new Resource();
			command = resource.inputToJSON(commandname,id, name, owner, description, channel, uri, tags, ezserver, secret, relay, servers);

			output.writeUTF(command.toJSONString());
			Client.debug("SEND" ,  command.toJSONString());
			//output.writeUTF(command.toJSONString());
			new Thread(new Runnable() {
				@Override
				public void run() {
					String string = null;
					try {
						while (true/*(string = input.readUTF()) != null*/) {
							String serverResponse = input.readUTF();
							JSONParser parser = new JSONParser();
							JSONObject response = (JSONObject) parser.parse(serverResponse);
							Client.debug("RECEIVE", response.toJSONString());
							//System.out.println(serverResponse);			 
							//							if((string = Reader.readLine()) != null){
							//								if(onKeyPressed(output,string,unsubscribJsonObject)) break;
							//							}
							if(flag ==1){
								break;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();

			new Thread(new Runnable() {
				@Override
				public void run() {
					String string = null;
					try {
						while ((string = Reader.readLine()) != null) {
							flag=1;
							if(onKeyPressed(output,string,unsubscribJsonObject)) break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean onKeyPressed(DataOutputStream sslOut, String string,JSONObject unsubscribJsonObject) {
		try {
			sslOut.writeUTF(unsubscribJsonObject.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}


	/*interface MessageListener {
		boolean onMessageReceived(Message message);
	}*/

}
