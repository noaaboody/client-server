package bgu.spl.net.impl.stomp;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

public class Frame {
    private String StompCommand;
    private HashMap<String, String> headers;
    private String body;
    private final String END = "\u0000";

    public Frame(String StompCommand, HashMap headers, String body){
        this.StompCommand = StompCommand;
        this.headers = headers;
        this.body = body;
    }

    public Frame(String message){
        String[] splitedMessage = message.split("\n");
        StompCommand = splitedMessage[0];
        headers = new HashMap<String, String>();
        int index = 1;

        while(!splitedMessage[index].equals(" ")){
            String[] headerParts = splitedMessage[index].split(":");
            headers.put(headerParts[0], headerParts[1]);
            index++;
        }
        body = "";
        index++;
        while (index < splitedMessage.length){
            if ( splitedMessage[index] != null){
                body = body + splitedMessage[index] + "\n";
                index++;
            }
        }
    }

    public String getStompCommand(){
        return StompCommand;
    }

    public HashMap<String, String> getHeaders(){
        return headers;
    }

    public String getBody(){
        return body;
    }

    public String ToString(){
        String output = StompCommand + "\n";
        for (HashMap.Entry<String,String> entry : headers.entrySet()){
            output += entry.getKey() + ":" + entry.getValue() + "\n";
        }      
        output += "\n" + body + "\n" + END;
        return output;
    }
}
