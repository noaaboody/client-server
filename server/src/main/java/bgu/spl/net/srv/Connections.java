package bgu.spl.net.srv;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public interface Connections<T> {
    boolean send(int connectionId, T msg);
    
    Map<String, String> getUserDetails();

    void send(String channel, T msg);

    void disconnect(int connectionId);

    void connect(int connectionId);
    
    public void newCH(int connectionId, ConnectionHandler<T> CH);

    public boolean addName(String userName, String password);

    Map<String, LinkedList<int[]>> getTopicMap();

    void subscribe(String string, int connectionId, int parseInt);

    void unsubscribe(int connectionId, int parseInt);
}
