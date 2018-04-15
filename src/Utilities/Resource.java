package Utilities;
import EZShare.Client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*; 
import org.json.simple.JSONObject;
import org.json.simple.JSONArray; 
import java.net.URI;
import java.net.URISyntaxException;
import EZShare.Server;

public class Resource {
    private String name;
    private String description;
    private List<String> tags;
    private URI uri;
    private String channel;
    private String owner;
    private String server;
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public String getDescription() {
        return description;
    }
 
    public void setDescription(String description) {
        this.description = description;
    }
 
    public List<String> getTags() {
        return tags;
    }
 
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
 
    public URI getUri() {  
        return uri;
    }
 
    public void setUri(URI uri) {
        this.uri = uri;
    }
 
    public String getChannel() {
        return channel;
    }
 
    public void setChannel(String channel) {
        this.channel = channel;
    }
 
    public String getOwner() {
        return owner;
    }
 
    public void setOwner(String owner) {
        this.owner = owner;
    }
 
    public String getServer() {
        return server;
    }
 
    public void setServer(String server) {
        this.server = server;
    }
    
    /*function to convert CLI to JSON string 
    
    1 -  must not contain the null character "\0"  
    2 -  must not start or end with white space. (DONE, but not with tags)
    3 -  As well, the Owner cannot be the character "*". (DONE)
    */
   
public static JSONObject inputToJSON(String commandname,int id,String name , String owner, String description, String channel, String uri, List<String> tags, String ezserver, String secret, Boolean relay,  String servers ) throws IOException, URISyntaxException{

		String[] serverList = servers.split(",");
		JSONArray serverArray = new JSONArray();

		for (int i = 0; i < serverList.length; i++) {
			JSONObject serverObject = new JSONObject();
			String hostname = serverList[i].split(":")[0].trim();
			try { 
				int port = Integer.valueOf(serverList[i].split(":")[1].trim());
				serverObject.put("hostname", hostname);
				serverObject.put("port", port);
				serverArray.add(serverObject); 
			}catch ( Exception e){
				serverObject.put("hostname", hostname);
				serverObject.put("port", "invalid");
				serverArray.add(serverObject); 
			}     

		}


		/*

            Trimming white space silently

		 */
		name = name.trim();
		owner = owner.trim();
		description = description.trim();
		channel = channel.trim();
		ezserver = ezserver.trim();         
		uri = uri.trim(); 



		/*

            Dealing with owner asterisk
            Dealing with null values silently

		 */            
		owner = owner.replace("*", "");             
		owner = owner.replace("\0", ""); 
		channel = channel.replace("\0", ""); 
		name = name.replace("\0", ""); 
		description = description.replace("\0", "");           
		uri = uri.replace("\0", ""); 
		ezserver = ezserver.replace("\0", ""); 

		Resource resource = new Resource();
		resource.setName(name);
		try{
			resource.setUri(new URI(uri));
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		resource.setDescription(description);
		resource.setChannel(channel);
		resource.setOwner(owner);
		resource.setTags(tags);
		resource.setServer(ezserver);
		JSONObject resourceObj = Resource.toJson(resource);
		JSONObject request = new JSONObject();

		request.put("command", commandname);

		switch(commandname){
		case "PUBLISH":
		case "REMOVE":
		{  request.put("resource", resourceObj);
		break;}

		case "SHARE":
		{   request.put("secret", secret);
		request.put("resource", resourceObj); 
		break;
		}
		case "QUERY":
		{   request.put("relay", relay);
		request.put("resourceTemplate", resourceObj);
		break;
		} 
		case "FETCH":
		{   request.put("resourceTemplate", resourceObj); 
		break;
		}
		case "EXCHANGE":
		{   request.put("serverList", serverArray);
		break;
		}
		case "SUBSCRIBE":
		{
			//request.put("command","SUBSCRIBE");
			request.put("relay",true);
			request.put("id", id+"");
			request.put("resourceTemplate",resourceObj);
		}
		default:
			// no need for message:  Client.debug("INFO" , "command name is invalid");  
		}  
		return (request);
	}

     
    
    //Function to convert Resource object to JSON Object
    public static JSONObject toJson(Resource resource){
        JSONObject jsonObject=new JSONObject();
        JSONArray tagArray=new JSONArray();
        
        if (resource.getTags()!=null){
            resource.getTags().forEach((String tag) -> {
                tagArray.add(tag);
            });
        }
        String server = Server.host + ":" +  Integer.toString(Server.port);
        jsonObject.put("name",resource.getName()==null?"":resource.getName());
        jsonObject.put("description",resource.getDescription()==null?"":resource.getDescription());
        jsonObject.put("uri",resource.getUri()==null?"":resource.getUri().toString());
        jsonObject.put("channel",resource.getChannel()==null?"":resource.getChannel());
        jsonObject.put("owner",resource.getOwner()==null?"":resource.getOwner());
        jsonObject.put("ezserver",server);
        jsonObject.put("tags",tagArray);

        return jsonObject;
    }
 
    
    
    
    //takes in JSON Object ---  converts to Resource object
    public static Resource parseJson(JSONObject resourceObject) throws URISyntaxException{
      
        String name=(String) resourceObject.get("name");
        String description=(String) resourceObject.get("description");
        String uriString=(String) resourceObject.get("uri");
        URI uri;
        uri = new URI(uriString);
        
        String owner=(String) resourceObject.get("owner");
        String channel=(String) resourceObject.get("channel");
        String ezServerString= (String)resourceObject.get("ezserver");
        String server=null;
        
        /*
        I COMMENTED THIS BECAUSE I CHANGED THE TYPE OF THE SERVER PARAMETER FROM SERVER TO STRING
        if (!ezServerString.equals("")){
            String host = ezServerString.split(":")[0];
            int port = Integer.parseInt(ezServerString.split(":")[1]);
            server = new Server(host, port);
        }
        */
        JSONArray tagArray=(JSONArray) resourceObject.get("tags");
        List<String> tagList=new ArrayList<>();
       
        for(int i=0;i<tagList.size();i++)
        {
            tagList.add((String)tagArray.get(i)); 
        }
        
        Resource resource=new Resource();
        resource.setName(name);
        resource.setChannel(channel);
        resource.setDescription(description);
        resource.setOwner(owner);
        resource.setUri(uri);
        resource.setTags(tagList);
        resource.setServer(server);
       
        return resource;
    }
}