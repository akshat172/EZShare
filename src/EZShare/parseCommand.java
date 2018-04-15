package EZShare;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import java.io.BufferedOutputStream;

public class parseCommand {

    private static final Logger LOGGER = Logger.getLogger(parseCommand.class.getName());
    public static void parseSecureCommand(JSONObject command, BufferedWriter output, int exchangeInterval, BufferedOutputStream x) throws IOException, URISyntaxException, ParseException {

        JSONObject response = new JSONObject();

        if (command.containsKey("command")) {
            Server.debug("INFO" , command.get("command") + " received.");
            Server.debug("INFO" , "command message:" + command.toJSONString());
              
            // THE COMMANDS GET PARSED IN serverCommands
            secureServerCommands cmd = new secureServerCommands();
            switch ((String) command.get("command")) {
                case "EXCHANGE":
                    cmd.exchange(command, output, exchangeInterval);
                    break;
                case "FETCH":
                    cmd.fetch(command,output,  x);
                    break;
                case "PUBLISH":
                    cmd.publish(command, output);
                    break;
                case "QUERY":
                    cmd.query(command, output);
                    break;
                case "REMOVE":
                    cmd.remove(command, output);
                    break;
                case "SHARE":
                    cmd.share(command, output);
                    break;
                 case "SUBSCRIBE":
		    cmd.subscribe(command, output);
		    break;
		case "UNSUBSCRIBE":
		    cmd.unsubscribe(command,output);
		    synchronized(Server.unsubscribe){
			Server.unsubscribe.notifyAll();
		    }
		    break;
                default: { 
                    response.put("response", "error");
                    response.put("errorMessage", "invalid command");
                    output.write(response.toJSONString() + '\n');
                    output.flush();
                    Server.debug("SEND", response.toJSONString());
                }
            }
        } else {
            //if the command is missing 
            response.put("response", "error");
            response.put("errorMessage", "missing or incorrect type for command");
            output.write(response.toJSONString()+ '\n');
                    output.flush();
            Server.debug("SEND", response.toJSONString());
                    
        }

    }

    public static void parseCommand(JSONObject command, DataOutputStream output, int exchangeInterval) throws IOException, URISyntaxException, ParseException {

        JSONObject response = new JSONObject();

        if (command.containsKey("command")) {
            Server.debug("INFO" , command.get("command") + " received.");
            Server.debug("INFO" , "command message:" + command.toJSONString());
           
             
            // THE COMMANDS GET PARSED IN serverCommands
            serverCommands cmd = new serverCommands();
            switch ((String) command.get("command")) {
                case "EXCHANGE":
                    cmd.exchange(command, output, exchangeInterval);
                    break;
                case "FETCH":
                    cmd.fetch(command, output);
                    break;
                case "PUBLISH":
                    cmd.publish(command, output);
                    break;
                case "QUERY":
                    cmd.query(command, output);
                    break;
                case "REMOVE":
                    cmd.remove(command, output);
                    break;
                case "SHARE":
                    cmd.share(command, output);
                    break;
                case "SUBSCRIBE":
		    cmd.subscribe(command, output);
		    break;
		case "UNSUBSCRIBE":
		    cmd.unsubscribe(command,output);
		    synchronized(Server.unsubscribe){
		    Server.unsubscribe.notifyAll();
		    }
		    break;
                default: { 
                    response.put("response", "error");
                    response.put("errorMessage", "invalid command");
                    output.writeUTF(response.toJSONString());
                    Server.debug("SEND", response.toJSONString());      
                }
            }
        } else {
            //if the command is missing 
            response.put("response", "error");
            response.put("errorMessage", "missing or incorrect type for command");
            output.writeUTF(response.toJSONString());
            Server.debug("SEND", response.toJSONString());        
        }
    }
}
