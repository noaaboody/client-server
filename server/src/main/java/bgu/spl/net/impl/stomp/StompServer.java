package bgu.spl.net.impl.stomp;
import bgu.spl.net.srv.Server;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.impl.stomp.StompMessagingProtocolImpl;


public class StompServer {

    public static void main(String[] args) {
        String protocolType = args[1];
        int port=Integer.parseInt(args[0]);
        if (protocolType.equals("tpc")){
            Server.threadPerClient(
                port, //port
                () -> new StompMessagingProtocolImpl(), //protocol factory
                StompEncoderDecoder::new //message encoder decoder factory
        ).serve();
        }
        else{
            Server.reactor(
            Runtime.getRuntime().availableProcessors(),
            port, //port
            () -> new StompMessagingProtocolImpl(), //protocol factory
            StompEncoderDecoder::new //message encoder decoder factory
        ).serve();
        }
    }
}