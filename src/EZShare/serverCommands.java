package EZShare;

import Utilities.Resource;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import static java.lang.Math.random;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Collections.list;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
public class serverCommands {

    private static final Logger LOGGER = Logger.getLogger(serverCommands.class.getName());
public static boolean valid=true;
	public static Socket socket = null;
	//private static final Logger LOGGER = Logger.getLogger(serverCommands.class.getName());
	Resource resourceTemp = null;
    JSONObject response = new JSONObject();
    JSONObject resource = new JSONObject();
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

    
    
    
    public void exchange(JSONObject command, DataOutputStream output, int exchangeInterval) throws IOException {

        /*
        1 - Copy all in serverList (from the command) to the server's server.serverRecords

        2 - If the server is included in the list, remove it (because it may connect to itself)

        3a - Enforce rules:  check if list of servers received is empty (missing or invalid server list)
        3b - Enforce rules:  check if server record received is wrong (missing resourcetemplate )

        4 - Pick a random server

        5 - Attemp connect to the randomly selected server

        6 - Sending the list to the randomly selected server

        7 - Display distinct servers in the serverRecords

        8 - Filter out only unique records from serverRecords

        9 - If the connection with the random server is not established remove serverRecord

         */
        resource = (JSONObject) command.get("resource");
        JSONObject response = new JSONObject();
        JSONArray serverArray = new JSONArray();
        serverArray = (JSONArray) command.get("serverList");
        /*

        1 - Copy all in serverList (from the command) to the server's server.serverRecords

         */
        for (int i = 0; i < serverArray.size(); i++) {
            synchronized (Server.serverRecords) {
				if(!Server.serverRecords.contains(serverArray.get(i))){
					Server.serverRecords.add(serverArray.get(i));
					Server.serverRecords.notifyAll();
				}
			}
        }
        /*

        2 - IF the server contains itself in the serverrecords list, then remove it

        */
        JSONObject serverTraverser = new JSONObject();
        for (int i = 0; i < Server.serverRecords.size(); i++) {
          serverTraverser = (JSONObject) Server.serverRecords.get(i);
          if (serverTraverser.get("hostname").equals(Server.host)) {
                if (serverTraverser.get("port").toString().equals(Integer.toString(Server.port))||serverTraverser.get("port").toString().equals(Integer.toString(Server.sport))) {
                    Server.serverRecords.remove(i);
                }
            }
        }
        /*
        3a -  Enforce rules:  check if list of servers received is empty (missing or invalid server list)
         */
        if (Server.serverRecords.isEmpty()) {
            response.put("response", "error");
            response.put("errorMessage", "missing or invalid server list");
            output(response, output);
            return;
        } else {
            /*

         3b -  Enforce rules:  check if server record received is wrong (missing resourcetemplate )
             */
            for (int i = 0; i < Server.serverRecords.size(); i++) {
                serverTraverser = (JSONObject) Server.serverRecords.get(i);
                if (serverTraverser.get("port").equals("invalid")) {
                    response.put("response", "error");
                    response.put("errorMessage", "missing resourceTemplate");
                    output(response, output);
                    return;
                }
            }

            /*

                4 - Pick a random server to connect from the list

             */
            Random r = new Random();
            JSONObject randomServer = new JSONObject();
            int size = Integer.valueOf(Server.serverRecords.size());
            int index = Integer.valueOf(r.nextInt(size));
            randomServer = (JSONObject) Server.serverRecords.get(index);

            /*

                5 - Attemp connect to the randomly selected server

             */
            String connect_host = randomServer.get("hostname").toString();
            int connect_port = ((Long) randomServer.get("port")).intValue();

            try {
                Socket socket = null;
                socket = new Socket(connect_host, connect_port);
                socket.setSoTimeout(exchangeInterval);
               // Server.debug("INFO","exchange with " + connect_host + ":" + connect_port + " is successful.");
                /*

                6 - Sending the list to the randomly selected server

                 */

                 /*

                8 - Filter out only unique records from serverRecords

                 *//*
                Set<String> setWithUniqueValues = new HashSet<>(Server.serverRecords);
                ArrayList<String> listWithUniqueValues = new ArrayList<>(setWithUniqueValues);
                Server.serverRecords = listWithUniqueValues;
                
                */
                JSONObject listToRandomServer = new JSONObject();
                listToRandomServer.put("command", "EXCHANGE");
                listToRandomServer.put("serverList", Server.serverRecords);
                DataOutputStream serverOutput = new DataOutputStream(socket.getOutputStream());
                output(listToRandomServer, serverOutput);

                /*

                7 - Display distinct servers in the serverRecords

                 */
                
                DataInputStream serverInput = new DataInputStream(socket.getInputStream());
                String message = serverInput.readUTF();
                JSONParser parser = new JSONParser();
                JSONObject JSONresponse = (JSONObject) parser.parse(message);
                Server.debug("RECEIVE", "insecure exchange: " + JSONresponse.toJSONString());

               

            } catch (Exception e) {
                
                /*

                9 - If the connection with the random server is not established remove serverRecord

                 */
                Server.debug("INFO","connection with server " + connect_host + ":" + connect_port + " was not successful: " + e);
                serverTraverser = new JSONObject();
                for (int i = 0; i < Server.serverRecords.size(); i++) {
                    serverTraverser = (JSONObject) Server.serverRecords.get(i);
                    if (serverTraverser.get("hostname").equals(connect_host) && serverTraverser.get("port").equals(connect_port)) {
                        Server.serverRecords.remove(i);

                    }
                }
              
            } finally {
                response.put("response", "success");
                output(response, output);
            }

        }

    }

    public void fetch(JSONObject command, DataOutputStream output) throws IOException, URISyntaxException {
        //initiating response (same for all functions)
        JSONObject response = new JSONObject();
        resource = (JSONObject) command.get("resourceTemplate");
        Resource resourcePublish = Resource.parseJson(resource);

        URI URI = new URI("");

        // Check if the server has the file with the input channel and URI
        Resource fetchResource = new Resource();
        for (int i = 0; i < Server.serverResources.size(); i++) {
            fetchResource = (Resource) Server.serverResources.get(i);
            if (resourcePublish.getChannel().equals(fetchResource.getChannel())
                    && resourcePublish.getUri().toString().equals(fetchResource.getUri().toString())) {
                URI = fetchResource.getUri();
            }
        }

        File f = new File(URI.getPath());

        //check if the file exists
        if (f.exists()) {
            Boolean isFile = URI.getScheme().equals("file");
            // check if URI is empty and URI follows the correct scheme
            if (URI.equals("") && !isFile) {
                // Response if URI is invalid
                response.put("response", "error");
                response.put("errorMessage", "invalid resource.");
                output(response, output);
            } else {
               // Response if it is a success
                System.out.println("Server: File " + URI + " exists.");
            resource.put("resourceSize", f.length());
                response.put("response", "success");
                output(response, output);
                output(resource, output);
                try {
                    //read file as random access file
                    RandomAccessFile byteFile = new RandomAccessFile(f, "r");
                    byte[] sendingBuffer = new byte[1024 * 1024];
                    int num;
                    while ((num = byteFile.read(sendingBuffer)) > 0) {
                        output.write(Arrays.copyOf(sendingBuffer, num));
                    }
                    byteFile.close();
                    response.remove("response","success");
                    response.put("resultSize", "1");
                    output(response, output);

                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        } else {
           // Response if the file is not there on server
            response.put("response", "error");
            response.put("errorMessage", "Missing resource.");
            output(response, output);
        }
    }

    public void publish(JSONObject command, DataOutputStream output) throws IOException, URISyntaxException {

        /*
        enforced rules
        0 - the resource fields is present
        1 - if the URI field is present
        2 - if the URI field is not empty
        3 - is a web
        4 - is absolute 
        5 - is primary (not event Channel/URI different Owner combo)
        (COMMENTED) 6 - The server sets its own host and port on the resource
        7 - resource is committed to server resources
         */
        
        valid=true;
        // 0 -  if the resource field was not given
        JSONObject response = new JSONObject();
        if (!command.containsKey("resource")) {
            response.put("response", "error");
            response.put("errorMessage", "missing resource");
            output(response, output);
            return;
        }

        // 1 - if the URI field is present (i.e. not missing) 
        resource = (JSONObject) command.get("resource");
        Resource resourcePublish = Resource.parseJson(resource);
        String link = (String) resource.get("uri");
        URI uri = new URI(link);
        if (!resource.containsKey("uri")) {
            response.put("response", "error");
            response.put("errorMessage", "missing resource");
            output(response, output);
            return;
        }

        // 2 - if the URI field is not empty
        if (link.equals("")) {
            response.put("response", "error");
            response.put("errorMessage", "cannot publish resource");
            output(response, output);
            return;
        }

        // 3 - is a web 
        try {
            Boolean isWeb = uri.getScheme().equals("http") || uri.getScheme().equals("https");
            if (!isWeb) {
                response.put("response", "error");
                response.put("errorMessage", "invalid resource");
                output(response, output);
                return;
            }
        } catch (Exception e) {
            response.put("response", "error");
            response.put("errorMessage", "invalid resource");
            output(response, output);
            return;

        }

        // 4 - is absolute
        try {
            Boolean isAbsolute = uri.isAbsolute();
            if (!isAbsolute) {
                //if URI isn't absolute
                response.put("response", "error");
                response.put("errorMessage", "cannot publish resource");
                output(response, output);
                return;
            }
        } catch (Exception e) { 
            response.put("response", "error");
            response.put("errorMessage", "cannot publish resource");
            output(response, output);
            return;

        }

        //5 - is primary (not event Channel/URI different Owner combo)
		/*Boolean isPrimaryKey = primaryKeyCheckAndRemove(resourcePublish);
        if (isPrimaryKey) {

        } else {
            //if the resource provided is same Channel / URI but different Owner
            response.put("response", "error");
            response.put("errorMessage", "cannot publish resource");
            output(response, output);
            return;
        }*/
		for(int i=0;i<Server.serverResources.size();i++)
		{
			Resource queryRes = Server.serverResources.get(i);
			if(queryRes.getChannel().equals(resourcePublish.getChannel())&&queryRes.getUri().equals(resourcePublish.getUri())&&
					!queryRes.getOwner().equals(resourcePublish.getOwner()))
			{

				valid=false;
				System.out.println(valid);
			}
			if(queryRes.getChannel().equals(resourcePublish.getChannel())&&queryRes.getUri().equals(resourcePublish.getUri())&&
					queryRes.getOwner().equals(resourcePublish.getOwner()))
			{
				synchronized (Server.serverResources) {
					Server.serverResources.set(i, resourcePublish);
					Server.serverResources.notifyAll();
					response.put("response", "success");
					output(response, output);
					return;
				}

				//overWriteRes(queryRes, resourcePublish,i);
			}
		}

		// 6 - The server sets its own host and port on the resource
		// resourcePublish.setServer(Server.host + ":" + Server.port);

		// 7 - commit the resource to server's resources
		if(valid)
		{
			synchronized (Server.serverResources) {

				Server.serverResources.add(resourcePublish);
				Server.serverResources.notifyAll();
			}
			response.put("response", "success");
			output(response, output);
			return;

		}
		else{
			response.put("response", "error");
			response.put("errorMessage", "cannot publish resource");
			output(response, output);
		}
    }

    
    private boolean primaryKeyCheckAndRemove(Resource resource) {
        //get resource object
        //check for primary keys
        //if it exists, remove it
        Resource queryResource = new Resource();
        Boolean isPrimary = true;
        for (int i = 0; i < Server.serverResources.size(); i++) {
            queryResource = (Resource) Server.serverResources.get(i);
            if (resource.getChannel().equals(queryResource.getChannel())
                    && resource.getUri().toString().equals(queryResource.getUri().toString())
                    && resource.getOwner().equals(queryResource.getOwner())) {
                Server.serverResources.remove(i);

            }
            //if CHANNEL AND URI the same but OWNER is different, returns error
            if (resource.getChannel().equals(queryResource.getChannel())
                    && resource.getUri().toString().equals(queryResource.getUri().toString())
                    && !resource.getOwner().equals(queryResource.getOwner())) {
                isPrimary = false;
                break;
            }
        }
        return isPrimary;
    }

    public void query(JSONObject command, DataOutputStream output) throws URISyntaxException, IOException, ParseException {
                       
        resource = (JSONObject) command.get("resourceTemplate");
        Boolean relay = Boolean.valueOf(command.get("relay").toString());
        Resource resourceObject = (Resource) Resource.parseJson(resource);
        ArrayList queryResult = new ArrayList();
        JSONObject response = new JSONObject();

        /* IF RELAY IS TRUE

        go through servers
            if it's online, send request
            receive the response
            add to queryResult

         */
        if (relay) {
            if (Server.serverRecords.isEmpty()) {
                //If the server Records list is empty
                //Just don't do anything
               Server.debug("RELAY INFO","server list is empty");
            } else {

                 Server.debug("RELAY INFO","server querying initiated");
                String connect_host = "";
                int connect_port;
                JSONObject hostPort = new JSONObject(); 
                JSONObject request = new JSONObject();
                
                // JSONArray to receive query results from each server 
                request.put("command", "QUERY");
                request.put("relay", "false");
                resource.put("channel", "");
                resource.put("owner", "");
                request.put("resourceTemplate", resource);
                //traverse through servers 

                for (int i = 0; i < Server.serverRecords.size(); i++) {
                    /*
                    if ONLINE
                    1- connect
                    2- query for results
                    3- store them in queryResults

                     */

                    Server.debug("RELAY INFO","connecting to server " + Server.serverRecords.get(i));

                    hostPort = (JSONObject) Server.serverRecords.get(i);
                    connect_host = hostPort.get("hostname").toString();
                 //   Server.debug("RELAY INFO","hostname:" + connect_host);
                    connect_port = Integer.parseInt(hostPort.get("port").toString()); 
                  //  Server.debug("RELAY INFO","port:" + connect_port);

                    //if the server isn't relaying to itself
                    if (!(connect_host == Server.host && connect_port == Server.port)) {
                        try {
                            // Create connection with the selected server from the serverlist
                            Socket socket = null;
                            socket = new Socket(connect_host, connect_port);
                            DataOutputStream serverOutput = new DataOutputStream(socket.getOutputStream());
                           // Server.debug("RELAY INFO","Connecting to server " + Server.serverRecords.get(i));
                            output(request, serverOutput); 
                            DataInputStream input = new DataInputStream(socket.getInputStream());
                            // If there are query results:
                            if (!receiveQuery(input).isEmpty()) {
                                queryResult.addAll(receiveQuery(input));
                                 Server.debug("RECEIVE", queryResult.toString());
                           
                            }

                            // for each in Query results
                            socket.close();
                        } catch (Exception e) {
                            Server.serverRecords.remove(i);
                        }
                    }
                }

            }

        }

        if (Server.serverResources.size() > 0) {
            for (int i = 0; i < Server.serverResources.size(); i++) {

                Resource queryResource = (Resource) Server.serverResources.get(i);

                if (resourceObject.getChannel().equals(queryResource.getChannel())
                        && (resourceObject.getUri().toString().equals("") || (resourceObject.getUri().toString().equals(queryResource.getUri().toString())))
                        && (resourceObject.getOwner().toString().equals("") || resourceObject.getOwner().equals(queryResource.getOwner()))
                        && (resourceObject.getTags().size() == 0 || resourceObject.getTags().stream().anyMatch(tag -> queryResource.getTags().contains(tag)))
                        && ((resourceObject.getDescription().equals("") && resourceObject.getName().equals(""))
                        || queryResource.getName().contains(resourceObject.getName())
                        || queryResource.getDescription().contains(resourceObject.getDescription()))) {

                    if (!(queryResource.getOwner().equals(""))) {
                        queryResource.setOwner("*");
                    }
                    queryResult.add(queryResource);
                }
            }
            // the owner and channel information in the original query are both set to "" in the forwarded query �
            // relay field is set to false

            if (queryResult.size() > 0) {

                response.put("response", "success");
                output(response, output);
                JSONObject resource = new JSONObject();
                Resource r = new Resource();

                for (int i = 0; i < queryResult.size(); i++) {

                    r = (Resource) queryResult.get(i);
                    resource = Resource.toJson(r);

                    output(resource, output);
                }

                response.put("resultSize", queryResult.size());

            } else {
                //This means that the resource doesn't match any of the server list
                response.put("response", "error");
                response.put("errorMessage", "invalid resourceTemplate");

            }

        } else {
            //if there is no resource
            response.put("response", "success");
            response.put("resultSize", "0");

        }
        output(response, output);

    }

    public void remove(JSONObject command, DataOutputStream output) throws URISyntaxException, IOException {

        if (command.containsKey("resource")) {
            JSONObject resourceObject = (JSONObject) command.get("resource");
            Resource resource = Resource.parseJson(resourceObject);
            Resource queryResource = new Resource();

            Boolean resourceExists = false;
            for (int i = 0; i < Server.serverResources.size(); i++) {
                queryResource = (Resource) Server.serverResources.get(i);
                if (resource.getChannel().equals(queryResource.getChannel())
                        && resource.getUri().toString().equals(queryResource.getUri().toString())
                        && resource.getOwner().equals(queryResource.getOwner())) {
                    Server.serverResources.remove(i);
                    response.put("response", "success");
                    resourceExists = true;
                    break;
                }
            }
            if (!resourceExists) {
                response.put("response", "error");
                response.put("errorMessage", "cannot remove resource");

            }
        } else {
            response.put("response", "error");
            response.put("errorMessage", "missing resource");
        }
        output(response, output);

    }

    public void share(JSONObject command, DataOutputStream output) throws IOException, URISyntaxException {

        JSONObject response = new JSONObject();
        resource = (JSONObject) command.get("resource");
        Resource resourceShare = Resource.parseJson(resource);
        if (command.containsKey("secret") && resource.containsKey("uri")) {

            String secret = (String) command.get("secret");

            String link = (String) resource.get("uri");

            URI uri = new URI(link);
            Boolean isFile = uri.getScheme().equals("file");
            Boolean isAbsolute = uri.isAbsolute();
            String present = (String) resource.get("uri");
            Boolean isPresent = present.equals("");
            File f = new File(uri.getPath());

            if (secret.equals(Server.secret)) {
                //if the secret is correct
                synchronized (Server.serverResources) {
					Server.serverResources.add(resourceShare);
					Server.serverResources.notifyAll();
				}
                response.put("response", "success");

                Boolean isPrimaryKey = primaryKeyCheckAndRemove(resourceShare);

                /*
        enforced rules
        1- is a file
        2- is absolute
        3- is present
        4- is primary (not event Channel/URI different Owner combo)
        5- is not authortitave
        6- is present on the machine
                 */
                if (f.exists()) {
                    if (isPrimaryKey) {
                        if (!isPresent) {
                            if (isFile) {
                                if (isAbsolute) {

                                    Server.serverResources.add(resourceShare);
                                    response.put("response", "success");

                                } else {
                                    //if URI isn't absolute
                                    response.put("response", "error");
                                    response.put("errorMessage", "invalid resource");

                                }
                            } else {
                                //if the URI isn't a file
                                response.put("response", "error");
                                response.put("errorMessage", "invalid resource");

                            }
                        } else {
                            //if the URI is not present
                            response.put("response", "error");
                            response.put("errorMessage", "cannot share resource (URI not present)");

                        }
                    } else {
                        //if the resource provided is same Channel / URI but different Owner
                        response.put("response", "error");
                        response.put("errorMessage", "cannot share resource (owner is different)");
                    }
                } else {
                    //if the file is not on the server
                    response.put("response", "error");
                    response.put("errorMessage", "cannot share resource (file is not on server)");
                }
            } else {
                //if the secret is invalid
                response.put("response", "error");
                response.put("errorMessage", "incorrect secret");
            }
        } else { //if the secret or the URI aren't provided
            response.put("response", "error");
            response.put("errorMessage", "missing resource and/or secret");

        }

        output(response, output);

    }

    /*

    This function will be called whenever relay is set to true
    It will receive the list of results from each server
    then return each

     */
    public ArrayList receiveQuery(DataInputStream input) throws IOException, ParseException, URISyntaxException {

        //READ RESPONSE (error or success)
        String firstResponse = input.readUTF();
        JSONParser parser = new JSONParser();
        JSONObject JSONresponse = (JSONObject) parser.parse(firstResponse);
        
       // Server.debug("[RECEIVE] " + JSONresponse.get("response"));

        Integer size = -1;
        Integer resultSize;
        Long result = 0L;
        ArrayList query = new ArrayList();
        Boolean done = false;
        //Resource object to hold the JSON object retrieved for each resource
        Resource resource = new Resource();
        if (JSONresponse.get("response") == "success") {

            while (true) {

                if (input.available() > 0) {
                    String serverResponse = input.readUTF();
                    JSONObject response = (JSONObject) parser.parse(serverResponse);
                    //juST TO make sure we're receiving a resource
                    if (response.containsKey("URI")) {
                        resource = Resource.parseJson(response);
                        query.add(resource);
                    }
                    size += 1;

                    if (response.containsKey("resultSize") || result == 0L) {
                        result = (Long) response.get("resultSize");
                        resultSize = Integer.valueOf(result.intValue());

                        if (size == resultSize) {
                            done = true;
                        }
                    }
                }

                if (done) {
                    break;
                }

            }
        }
        return query;
    }

    private void output(JSONObject response, DataOutputStream output) throws IOException {
           Server.debug("SEND",response.toJSONString());
        output.writeUTF(response.toJSONString());

    }
      private void secureoutput(JSONObject response, BufferedWriter output) throws IOException {
           Server.debug("SECURE SEND",response.toJSONString());
             output.write(response.toJSONString());

    }
      public void unsubscribe(JSONObject command, DataOutputStream output) throws IOException{
		int id;
		try{

			id = Integer.parseInt((String) command.get("id"));
		}catch(Exception e){
			LOGGER.info("missing id");
			return;
		}
		for(int i=0;i<Server.Subscriber.size();i++){
			if(Server.Subscriber.get(i).equals(id)){
				Server.Subscriber.remove(i);
			}
		}

		response.put("response", "success");
		response.put("resultSize", Server.resultSize);
		Server.resultSize=0;
		output(response,output);
	}
	public void subscribe(JSONObject command, DataOutputStream output) throws IOException {
		// TODO Auto-generated method stub

		String id = (String) command.get("id");
		if(Server.Subscriber.contains(id)){
			response.put("response", "error message");
			response.put("error message", "Already Subscribed");
			output.writeUTF(response.toJSONString());
			return;
		}
		else{
			Server.Subscriber.add(id);
			response.put("response", "success");
			response.put("id", id);
			output(response,output);
		}
		try{
			JSONObject template=(JSONObject) command.get("resourceTemplate");
			resourceTemp = (Resource) Resource.parseJson(template);
		}
		catch (Exception e) {
			// TODO: handle exception
			response.put("response", "error");
			response.put("errorMessage", "invalid resourceTemplate");
		}
		boolean relay = (boolean) command.get("relay");
		new Thread(new Runnable() {

			@Override
			public void run() {

				for(int i=0;i< Server.serverResources.size();i++){
					Resource queryResource = (Resource) Server.serverResources.get(i);
					if(templateMatch(queryResource,resourceTemp)){
						String owner =queryResource.getOwner();
						queryResource.setOwner("*");
						//res.setEzerver(Server.getAdvertisedHostName());
						Server.resultSize++;
						try {
							output.writeUTF(Resource.toJson(queryResource).toString());
							queryResource.setOwner(owner);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(relay){
					//final String tempId = id;

					Thread relaySub=new Thread(new Runnable() {

						public void run() {
							//createSubscriptions(serverResources, id, obj);
							SubscribeRelay(command,id,output);
						}
					});

					relaySub.start();
				}
				synchronized (Server.serverResources) {
					// Check if relay flag is set

					//boolean relay = obj.getBoolean("relay");

					while(Server.Subscriber.contains(id)){
						try {
							Server.serverResources.wait();
							if(Server.Subscriber.contains(id)){
								Resource recent = Server.serverResources.get(Server.serverResources.size()-1);
								if(templateMatch(recent,resourceTemp)){
									String owner =recent.getOwner();
									recent.setOwner("*");
									//recent.setEzerver(Server.getAdvertisedHostName());
									Server.resultSize++;
									output.writeUTF(Resource.toJson(recent).toString());
									recent.setOwner(owner);

								}
							}else{
								break;
							}
						} catch (InterruptedException e) {
							Server.debug("INFO","interrupted");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}).start(); 
	}
	public static boolean templateMatch(Resource resourceObject,Resource queryResource){
		if (resourceObject.getChannel().equals(queryResource.getChannel())
				&& (resourceObject.getUri().toString().equals("") || (resourceObject.getUri().toString().equals(queryResource.getUri().toString())))
				&& (resourceObject.getOwner().toString().equals("") || resourceObject.getOwner().equals(queryResource.getOwner()))
				&& (resourceObject.getTags().size() == 0 || resourceObject.getTags().stream().anyMatch(tag -> queryResource.getTags().contains(tag)))
				&& ((resourceObject.getDescription().equals("") && resourceObject.getName().equals(""))
						|| resourceObject.getName().contains(queryResource.getName())
						|| resourceObject.getDescription().contains(queryResource.getDescription()))) 
		{
			return true;
		}
		else
		{
			return false;
		}
		//return true;

	}
	public static void SubscribeRelay(JSONObject command,String id,DataOutputStream output)
	{
		ArrayList RelayList = (ArrayList) Server.serverRecords.clone();
		JSONObject randomServer = new JSONObject();
		for(int index=0;index<RelayList.size();index++)
		{
			randomServer = (JSONObject) RelayList.get(index);
			String connect_host = randomServer.get("hostname").toString();
			int connect_port = ((Long) randomServer.get("port")).intValue();

			Thread sendSub=new Thread(new Runnable() {
				public void run() {
                                    try {
                                        CreateConnectionRelay(connect_host,connect_port,id,command,output);
                                    } catch (IOException ex) {
                                        Logger.getLogger(serverCommands.class.getName()).log(Level.SEVERE, null, ex);
                                    }
				}
			});
			sendSub.start();			
		}
		Thread serverListMonitor=new Thread(new Runnable() {
			public void run() {
				while(Server.Subscriber.contains(id)){
					synchronized (Server.serverRecords) {
						try {
							Server.serverRecords.wait();
							JSONObject randomServerNew = new JSONObject();
							for(int i=0;i<Server.serverRecords.size();i++){
								if(!RelayList.contains(Server.serverRecords.get(i))){
									RelayList.add(Server.serverRecords.get(i));
									randomServerNew = (JSONObject) Server.serverRecords.get(i);
									String connect_host = randomServerNew.get("hostname").toString();
									int connect_port = ((Long) randomServerNew.get("port")).intValue();
									Thread sendSub=new Thread(new Runnable() {
										public void run() {
                                                                                    try {
                                                                                        CreateConnectionRelay(connect_host,connect_port,id,command,output);
                                                                                    } catch (IOException ex) {
                                                                                        Logger.getLogger(serverCommands.class.getName()).log(Level.SEVERE, null, ex);
                                                                                    }
										}
									});
									sendSub.start();
								}
							}
						} catch (InterruptedException e) {
							Server.debug("INFO","control of resource interrupted");
						}
					}
				}
			}
		});
		serverListMonitor.start();
	}

	@SuppressWarnings("unchecked")
	public static void CreateConnectionRelay(String connect_host, int connect_port, String id, JSONObject command,DataOutputStream output) throws IOException {
		// TODO Auto-generated method stub
		while(Server.Subscriber.contains(id)){
			try{
				boolean secure = false;

				if (secure) {
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					socket = (SSLSocket) sslsocketfactory.createSocket(connect_host, connect_port);
				} 
				else 
				{
					socket=new Socket(connect_host,connect_port);
				}
				DataInputStream relayInput = new DataInputStream(socket.getInputStream());
				DataOutputStream relayOutput = new DataOutputStream(socket.getOutputStream());
				JSONObject commandRelay=new JSONObject();
				commandRelay = command;
				commandRelay.put("relay", false);
				relayOutput.writeUTF(commandRelay.toString());
				Thread t=new Thread(new Runnable() {
					public void run() {
						while(true){
							synchronized(Server.unsubscribe){
								try {
									Server.unsubscribe.wait();
									if(!Server.Subscriber.contains(id)){
										JSONObject unsubscribJsonObject=new JSONObject();
										unsubscribJsonObject.put("command","UNSUBSCRIBE");
										unsubscribJsonObject.put("id",id+"");
										relayOutput.writeUTF(unsubscribJsonObject.toJSONString());
										socket.close();
									}
								} catch (IOException e) {
									Server.debug("INFO","Cannot close the connection");
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									Server.debug("INFO","Connection Interrupted");;
								}
							}
						}
					}
				});
				t.start();
				while(Server.Subscriber.contains(id)){
					try{
						while(relayInput.available()>0){
							/*JSONObject template = (JSONObject) command.get("resourceTemplate");
						Resource Restemplate = (Resource) Resource.parseJson(template);
						String reso = Resource.toJson(Restemplate).toString();*/
							String serverResponseRelay = relayInput.readUTF();
							JSONParser parser = new JSONParser();
							JSONObject relayResponse = (JSONObject) parser.parse(serverResponseRelay);
							Server.debug("RECEIVE", relayResponse.toJSONString());
							if(!serverResponseRelay.contains("response")/*serverResponseRelay.equals(reso)*/){
								//if(Server.debug){
								Server.debug("RECEIVE", relayResponse.toString());
								Server.resultSize += 1;
								output.writeUTF(serverResponseRelay.toString());
								//}
							}
						}
					}catch(Exception e){

						Server.debug("INFO" ,e.getMessage());
						break;
					}
				}
			}catch(Exception e){return;}
		}
	}		
}
