package bgu.spl.net.srv;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ConnectionsImpl<T> implements Connections<T>{

    public Map<Integer,ConnectionHandler<T>> CHPerClient = new HashMap<Integer,ConnectionHandler<T>>();
    public LinkedList<Integer> connectedClients = new LinkedList<Integer>();
    public Map<String, String> userDetails = new HashMap<String,String>();
    public Map<String,LinkedList<int[]>> topicMap = new HashMap<String,LinkedList<int[]>>();

    @Override
    public boolean send(int connectionId, T msg) {
        // TODO Auto-generated method stub
        try{
            CHPerClient.get(connectionId).send(msg);
            return true;
        }
        catch(Exception x){
            return false;
        }
    }

    @Override
    public Map<String, String> getUserDetails(){
        return userDetails;
    }

    @Override
    public void send(String channel, T msg) {
        for (Map.Entry<Integer,ConnectionHandler<T>> entry : CHPerClient.entrySet()){
            send(entry.getKey(), msg);
        }   
    }

    @Override
    public void disconnect(int connectionId) {
        for (Map.Entry<String,LinkedList<int[]>> entry : topicMap.entrySet()){
            for (int j = 0; j<topicMap.get(entry.getKey()).size(); j++){
                if (topicMap.get(entry.getKey()).get(j)[0] == connectionId){
                    topicMap.get(entry.getKey()).remove(j);
                }
            }     
        } 
        CHPerClient.remove(connectionId);
        connectedClients.remove((Integer)connectionId);

    }
    
    @Override
    public void newCH(int connectionId, ConnectionHandler<T> CH) {
        CHPerClient.put(connectionId, CH);
    }

    @Override
    public void connect(int connectionId) {
        if (!connectedClients.contains(connectionId)){
            connectedClients.add(connectionId);
        }
    }

    @Override
    public boolean addName(String userName, String password){
        if (!userDetails.containsKey(userName)){
            userDetails.put(userName, password);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, LinkedList<int[]>> getTopicMap() {
        return topicMap;
    }

    @Override
    public void subscribe(String topic, int connectionId, int subscriptionID) {
        LinkedList<int[]> subscriber = topicMap.get(topic);
        boolean isSubscribe = false;
        for (int i = 0; i<subscriber.size() && !isSubscribe; i++){
            if (subscriber.get(i)[0] == connectionId){
                isSubscribe = true;
            }
        }
        if (!isSubscribe){
            int[] subscriberID = {connectionId, subscriptionID};
            topicMap.get(topic).add(subscriberID);
        }
    }

    @Override
    public void unsubscribe(int connectionId, int subscriptionID) {
        for (Map.Entry<String,LinkedList<int[]>> entry : topicMap.entrySet()){
            for (int j = 0; j<topicMap.get(entry.getKey()).size(); j++){
                if (topicMap.get(entry.getKey()).get(j)[0] == connectionId && topicMap.get(entry.getKey()).get(j)[1] == subscriptionID){
                    topicMap.get(entry.getKey()).remove(j);
                }
            }     
        }  
    }
}
