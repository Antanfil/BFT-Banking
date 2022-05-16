package pt.tecnico.sec.server;

import com.google.protobuf.ByteString;
import io.grpc.*;
import pt.tecnico.sec.server.grpc.Server;
import pt.tecnico.sec.server.grpc.ServerServiceGrpc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import pt.tecnico.sec.client.*;



public class FrontendBroadcast implements Serializable {



    final String host;
    final int port;


    final int replicasNo;
    List<ManagedChannel> channelList = new ArrayList<ManagedChannel>();
    int ownPort;


    public int getReplicasNo() {
        return replicasNo;
    }
    public int getOwnPort() {
        return ownPort;
    }

    public FrontendBroadcast(int thisPort , String host, int port , int replicas) {

        this.host = host;
        this.port = port;
        replicasNo = replicas;
        ownPort = thisPort;


    }

    public void broadcastPK(String pbKey ){
        try {
            System.out.println("Propagating Public keys");
            Server.MessageRequest messageReq = Server.MessageRequest.newBuilder().setMessage("INITIALIZATION;"+pbKey).build();
            Server.MessageResponse messageResp = Server.MessageResponse.newBuilder().build();

            handleBroadcastSynchronization(messageReq);


        } catch (StatusRuntimeException e) {
            return;
            //return "Caught error with description: " + e.getStatus().getDescription();
        }
    }

    public void Broadcast(String message , byte[] signature){

        ByteString signHash = ByteString.copyFrom(signature);
        Server.MessageRequest messageReq = Server.MessageRequest.newBuilder().setMessage(message).setHash(signHash).build();

            try{
                handleBroadcastSynchronization(messageReq );
                //messageResp = stub.withDeadlineAfter(500000, TimeUnit.MILLISECONDS).send(messageReq);

            } catch (StatusRuntimeException e) {
            }


    }

    public void handleBroadcastSynchronization(Server.MessageRequest messageReq){


        List<Integer> ports = new ArrayList<Integer>();
        System.out.println("replicasNo: "+replicasNo);
        for(int i = 0 ; i<replicasNo; i++ ){
            ports.add(i);
        }



        for (int i = 0 ; i < replicasNo; i++ ) {
            if( !((8080+i) == ownPort) ){

                System.out.println("Sending to PORT- "+ (8080+i)+"\n --------- \n" );
                ManagedChannel tmp_channel = ManagedChannelBuilder.forAddress(host, (8080+i) ).usePlaintext().build();
                ServerServiceGrpc.ServerServiceBlockingStub tmp_stub = ServerServiceGrpc.newBlockingStub(tmp_channel);
                channelList.add(tmp_channel);
                tmp_stub.send(messageReq );
            }
        }

        for (ManagedChannel mCh : channelList) {
            mCh.shutdown();
        }

        channelList.clear();
        ports.clear();

    }



}
