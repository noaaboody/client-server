package bgu.spl.net.impl.stomp;

import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.srv.Server;


public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate;

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.shouldTerminate = false;
    }

    @Override
    public void process(String message) {
        if(!message.equals("")){
            Frame messageFrame = new Frame(message);
            if(messageFrame.getStompCommand().equals("CONNECT")){
                caseConnect(connectionId, messageFrame);
            }
            else if(messageFrame.getStompCommand().equals("SEND")){
                caseSend(messageFrame);
            }
            else if(messageFrame.getStompCommand().equals("SUBSCRIBE")){
                caseSubcribe(messageFrame);
            }
            else if(messageFrame.getStompCommand().equals("UNSUBSCRIBE")){
                caseUnsubcribe(messageFrame);
            }
            else if(messageFrame.getStompCommand().equals("DISCONNECT")){
                caseDisconnect(messageFrame);
            }
        }        
    }

    public void caseConnect(int connectionId, Frame messageFrame){
        connections.connect(connectionId);
        String login = messageFrame.getHeaders().get("login");
        String passcode = messageFrame.getHeaders().get("passcode");
        boolean addName = connections.addName(login, passcode);

        // add new user
        if (addName){
            HashMap<String,String> connectedHeaders = new HashMap<String,String>();
            connectedHeaders.put("version", "1.2");
            Frame response = new Frame("CONNECTED", connectedHeaders, null);
            connections.send(connectionId, response.ToString());
        }
        // user already exists
        else{
            // already logged in ???
            // if(){
            //     HashMap<String,String> errorHeaders = new HashMap<String,String>();
            //     if (messageFrame.getHeaders().containsKey("receipt")){
            //         errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            //     }
            //     errorHeaders.put("message", "User already logged in");
            //     String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + login + " already logged in.";
            //     Frame errorFrame = new Frame("ERROR", errorHeaders, body);
            //     connections.send(connectionId, errorFrame.ToString());
            //     connections.disconnect(connectionId);
            //     shouldTerminate = true;
            // }

            // wrong password
            //else{
                if(passcode != connections.getUserDetails().get(login)){
                    HashMap<String,String> errorHeaders = new HashMap<String,String>();
                    if (messageFrame.getHeaders().containsKey("receipt")){
                        errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
                    }
                    errorHeaders.put("message", "Wrong password");
                    String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + " wrong password was entered for " + login;
                    Frame errorFrame = new Frame("ERROR", errorHeaders, body);
                    connections.send(connectionId, errorFrame.ToString());
                    connections.disconnect(connectionId);
                    //shouldTerminate = true;
                }
            //}
        }
    }

    public void caseSend(Frame messageFrame){
        if (validsend(messageFrame)){
            Map<String,LinkedList<int[]>> topicMap = connections.getTopicMap();
            LinkedList<int[]> subscribersTopic = topicMap.get(messageFrame.getHeaders().get("destination"));
            boolean found = false;
            for (int j=0; j<subscribersTopic.size() && !found; j++){
                if (subscribersTopic.get(j)[0] == connectionId){
                    found = true;
                }
            }
            if (found){
                for(int i=0; i<subscribersTopic.size(); i++){
                    HashMap<String,String> messageHeaders = new HashMap<String,String>();
                    messageHeaders.put("destination", messageFrame.getHeaders().get("destination"));
                    messageHeaders.put("message-id", "" + BaseServer.messageId++);                    
                    messageHeaders.put("subscription", "" + subscribersTopic.get(i)[1]);
                    Frame response = new Frame("MESSAGE", messageHeaders, messageFrame.getBody());
                    connections.send(subscribersTopic.get(i)[0], response.ToString());
                }
            }
            else{
                HashMap<String,String> errorHeaders = new HashMap<String,String>();
                if (messageFrame.getHeaders().containsKey("receipt")){
                    errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
                }
                errorHeaders.put("message", "unsubscribed client");
                String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "client " + connectionId + " is not subscribed to topic " + messageFrame.getHeaders().get("destination");
                Frame errorFrame = new Frame("ERROR", errorHeaders, body);
                
                connections.send(connectionId, errorFrame.ToString());
                connections.disconnect(connectionId);
                shouldTerminate = true;

            }
        }
    }

    public boolean validsend(Frame messageFrame){
        boolean isValid = true;
        HashMap<String,String> messageHeaders = messageFrame.getHeaders();
        if (messageHeaders.containsKey("destination")){
            Map<String,LinkedList<int[]>> topicMap = connections.getTopicMap();
            if(!topicMap.containsKey(messageHeaders.get("destination"))){
                isValid = false;
                HashMap<String,String> errorHeaders = new HashMap<String,String>();
                if (messageFrame.getHeaders().containsKey("receipt")){
                    errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
                }
                errorHeaders.put("message", "topic not found");
                String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "the topic " + messageFrame.getHeaders().get("destination") + " is not found.";
                Frame errorFrame = new Frame("ERROR", errorHeaders, body);
                
                connections.send(connectionId, errorFrame.ToString());
                connections.disconnect(connectionId);
                //shouldTerminate = true;

            }
        }
        else{
            isValid = false;
            HashMap<String,String> errorHeaders = new HashMap<String,String>();
            if (messageFrame.getHeaders().containsKey("receipt")){
                errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            }
            errorHeaders.put("message", "malformed frame received");
            String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "did not contain a destination header, which is required for SEND frame propagation.";
            Frame errorFrame = new Frame("ERROR", errorHeaders, body);
            
            connections.send(connectionId, errorFrame.ToString());
            connections.disconnect(connectionId);
            //shouldTerminate = true;

        }
        return isValid;
    }

    public void caseSubcribe(Frame messageFrame){
        if (validsubscribe(messageFrame)){
            HashMap<String,String> messageHeaders = messageFrame.getHeaders();
            Map<String,LinkedList<int[]>> topicMap = connections.getTopicMap();
            if(!topicMap.containsKey(messageHeaders.get("destination"))){
                topicMap.put(messageHeaders.get("destination"), new LinkedList<>());
            }
            connections.subscribe(messageFrame.getHeaders().get("destination") , connectionId, Integer.parseInt(messageFrame.getHeaders().get("id")));
            
            // create receipt frame 
            HashMap<String,String> receiptHeaders = new HashMap<String,String>();
            receiptHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            Frame response = new Frame("RECEIPT", receiptHeaders, null);
            connections.send(connectionId, response.ToString());
        }
    }

    public boolean validsubscribe(Frame messageFrame){
        boolean isValid = true;
        HashMap<String,String> messageHeaders = messageFrame.getHeaders();
        if (messageHeaders.containsKey("destination")){
            // Map<String,LinkedList<int[]>> topicMap = connections.getTopicMap();
            // if(topicMap.containsKey(messageHeaders.get("destination"))){
            //     LinkedList<int[]> subList = topicMap.get(messageHeaders.get("destination"));
            //     boolean found = false;
            //     HashMap<String,String> errorHeaders = new HashMap<String,String>();
            //     if (messageFrame.getHeaders().containsKey("receipt")){
            //         errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            //     }
            //     errorHeaders.put("message", "topic not found");
            //     String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "the topic " + messageFrame.getHeaders().get("destination") + " is not found.";
            //     Frame errorFrame = new Frame("ERROR", errorHeaders, body);
                
            //     connections.send(connectionId, errorFrame.ToString());
            //     connections.disconnect(connectionId);
            //     //shouldTerminate = true;

            
        }
        if (!messageHeaders.containsKey("destination")){
            isValid = false;
            HashMap<String,String> errorHeaders = new HashMap<String,String>();
            if (messageFrame.getHeaders().containsKey("receipt")){
                errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            }
            errorHeaders.put("message", "malformed frame received");
            String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "did not contain a destination header, which is required for SUBSCRIBE frame propagation.";
            Frame errorFrame = new Frame("ERROR", errorHeaders, body);
            
            connections.send(connectionId, errorFrame.ToString());
            connections.disconnect(connectionId);
            //shouldTerminate = true;

        }
        if (!messageHeaders.containsKey("id")){
            isValid = false;
            HashMap<String,String> errorHeaders = new HashMap<String,String>();
            if (messageFrame.getHeaders().containsKey("receipt")){
                errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            }
            errorHeaders.put("message", "malformed frame received");
            String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "did not contain an id header, which is required for SUBSCRIBE frame propagation.";
            Frame errorFrame = new Frame("ERROR", errorHeaders, body);
            
            connections.send(connectionId, errorFrame.ToString());
            connections.disconnect(connectionId);
            shouldTerminate = true;

        }
        return isValid;
    }

    public void caseUnsubcribe(Frame messageFrame){
        if(validUnsubscribe(messageFrame)){
            connections.unsubscribe(connectionId, Integer.parseInt(messageFrame.getHeaders().get("id")));
        }

        // create receipt frame 
        HashMap<String,String> receiptHeaders = new HashMap<String,String>();
        receiptHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
        Frame response = new Frame("RECEIPT", receiptHeaders, null);
        connections.send(connectionId, response.ToString());
    }

    public boolean validUnsubscribe(Frame messageFrame){
        boolean isValid = true;
        HashMap<String,String> messageHeaders = messageFrame.getHeaders();
        if (!messageHeaders.containsKey("id")){
            isValid = false;
            HashMap<String,String> errorHeaders = new HashMap<String,String>();
            if (messageFrame.getHeaders().containsKey("receipt")){
                errorHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
            }
            errorHeaders.put("message", "malformed frame received");
            String body = "The message : \n" + "-----\n" + messageFrame.ToString() + "-----\n" + "did not contain an id header, which is required for UNSUBSCRIBE frame propagation.";
            Frame errorFrame = new Frame("ERROR", errorHeaders, body);
            
            connections.send(connectionId, errorFrame.ToString());
            connections.disconnect(connectionId);
            shouldTerminate = true;

        }
        return isValid;
    }

    public void caseDisconnect(Frame messageFrame){
        HashMap<String,String> messageHeaders = new HashMap<String,String>();
        messageHeaders.put("receipt-id", messageFrame.getHeaders().get("receipt"));
        Frame response = new Frame("RECEIPT", messageHeaders, null);
        connections.send(connectionId, response.ToString());

        connections.disconnect(connectionId);
        //shouldTerminate = true;
        

    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
}
