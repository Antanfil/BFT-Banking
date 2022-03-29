package pt.tecnico.sec.server;

import java.util.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import org.apache.commons.lang3.tuple.Pair;
import pt.tecnico.sec.server.grpc.*;
import pt.tecnico.sec.server.grpc.Server.*;


public class ServerFrontend {

    ManagedChannel channel;
    ServerServiceGrpc.ServerServiceBlockingStub stub;


    public ServerFrontend(String host, int port) {

        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = ServerServiceGrpc.newBlockingStub(channel);


    }

    public String Ping(String input) {
        try {
            PingRequest pingreq = PingRequest.newBuilder().setInput(input).build();
            PingResponse pingresp = PingResponse.newBuilder().build();

            pingresp = stub.ping(pingreq);
            return pingresp.getOutput();
        } catch (StatusRuntimeException e) {
            return "Caught error with description: " + e.getStatus().getDescription();
        }

    }

    public String exchange( String pbKey ){
        try {
            MessageRequest messageReq = MessageRequest.newBuilder().setMessage(pbKey).setHash("").build();
            MessageResponse messageResp = MessageResponse.newBuilder().build();

            messageResp = stub.send(messageReq);
            return messageResp.getMessage();

        } catch (StatusRuntimeException e) {
            return "Caught error with description: " + e.getStatus().getDescription();
        }
    }

    public List<String> send( String message , String signature){
        MessageRequest messageReq = MessageRequest.newBuilder().setMessage(message).setHash(signature).build();
        MessageResponse messageResp = MessageResponse.newBuilder().build();

        messageResp = stub.send(messageReq);

        List<String> list = new ArrayList<String>();
        list.set(0 , messageResp.getMessage() );
        list.set(1 , messageResp.getHash() );

        return list;

    }
/*
    public int Balance(String name) {
        try {
            BalanceRequest breq = BalanceRequest.newBuilder().setName(name).build();
            BalanceResponse bresp = BalanceResponse.newBuilder().build();
            bresp = stub.getbalance(breq);
            return bresp.getBalance();
        } catch (StatusRuntimeException e) {
            System.out.println("Caught error with description: " + e.getStatus().getDescription());
            return -1;
        }
    }
*/
}
