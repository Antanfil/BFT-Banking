package pt.tecnico.sec.client;

import com.google.protobuf.ByteString;
import io.grpc.*;
import pt.tecnico.sec.server.grpc.Server.*;
import pt.tecnico.sec.server.grpc.ServerServiceGrpc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ServerFrontend {

    ManagedChannel channel;
    ServerServiceGrpc.ServerServiceBlockingStub stub;
    final String host;
    final int port;
    final int replicasNo;
    List<ManagedChannel> channelList = new ArrayList<ManagedChannel>();


    public ServerFrontend(String host, int port , int replicas) {

        this.host = host;
        this.port = port;

        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = ServerServiceGrpc.newBlockingStub(channel);
        replicasNo = replicas;

    }

    public List<String> exchange(String pbKey ){
        try {
            MessageRequest messageReq = MessageRequest.newBuilder().setMessage(pbKey).build();
            MessageResponse messageResp = MessageResponse.newBuilder().build();


            return handleSyncForExchange(messageReq);


        } catch (StatusRuntimeException e) {
            return null;
            //return "Caught error with description: " + e.getStatus().getDescription();
        }
    }

    public String connect(String message , byte[] signature, List<PublicKey> serverPublicKey){


        ByteString signHash = ByteString.copyFrom(signature);
        MessageRequest messageReq = MessageRequest.newBuilder().setMessage(message).setHash(signHash).build();
        MessageResponse messageResp;
        String messageResponse = "";
        String SSID = "-1";
        int inc = 0;

        boolean signatureOK = false;

        while( !signatureOK ) {
            if(inc == 20){
                System.out.println("Overload");
                return "-1";
            }
            try{
                inc ++;
                messageResp = handleSynchronization(messageReq);

            } catch (StatusRuntimeException e) {

                if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    return "-2";
                }
                return "-1";
            }

            String[] params = messageResp.getMessage().split(";");
            SSID = params[0];


            messageResponse = messageResp.getMessage();
            ByteString signHashResponse = messageResp.getHash();

            byte[] signatureResponse = signHashResponse.toByteArray();

            if (verifySignature(messageResponse, signatureResponse, serverPublicKey)) {
                signatureOK = true;
            }
            else{
                signatureOK = false;
            }

        }

        return SSID;

    }

    public String send(String message , byte[] signature, List<PublicKey> serverPublicKey, int sid , int seqNo){


        ByteString signHash = ByteString.copyFrom(signature);
        MessageRequest messageReq = MessageRequest.newBuilder().setMessage(message).setHash(signHash).build();
        MessageResponse messageResp = MessageResponse.newBuilder().build();
        String messageResponse = "";
        String SSID;
        String SeqNo;

        boolean responseOK = false;
        boolean signatureOK = false;

        while( !signatureOK || !responseOK ) {

            try{
                messageResp = handleSynchronization(messageReq);
                //messageResp = stub.withDeadlineAfter(500000, TimeUnit.MILLISECONDS).send(messageReq);

            } catch (StatusRuntimeException e) {

                if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    return "-2";
                }
                return "-1";
            }

            String[] params = messageResp.getMessage().split(";");
            SSID = params[0];
            SeqNo = params[1];

            System.out.println(SSID);
            System.out.println(seqNo);


            messageResponse = messageResp.getMessage();
            ByteString signHashResponse = messageResp.getHash();

            byte[] signatureResponse = signHashResponse.toByteArray();
            System.out.println("Verifying authenticity of incoming message ...");
            if (verifySignature(messageResponse, signatureResponse, serverPublicKey)) {
                signatureOK = true;
            }
            else{
                signatureOK = false;
            }
            if ( Integer.parseInt(SSID) == sid && Integer.parseInt(SeqNo) == seqNo ) {
                responseOK = true;
            }
            else{
                responseOK = false;
            }


        }
        System.out.println("Authenticity verified ...");
        return messageResponse;

    }


    public MessageResponse handleSynchronization( MessageRequest messageReq){

        List<MessageResponse> messageResponses = new ArrayList<>();
        MessageObserver<MessageResponse> MessageObserver = new MessageObserver<MessageResponse>() ;


        try{

            List<Integer> ports = new ArrayList<Integer>();
            for(int i = 0 ; i<replicasNo; i++ ){
                ports.add(i);
            }
            Context ctx = Context.current().fork();
            synchronized(MessageObserver){
                ctx.run ( () -> {

                    for (int i: ports) {
                        ManagedChannel tmp_channel = ManagedChannelBuilder.forAddress(host, 8080+i ).usePlaintext().build();
                        ServerServiceGrpc.newStub(channel);
                        ServerServiceGrpc.ServerServiceStub tmp_stub = ServerServiceGrpc.newStub(tmp_channel);
                        channelList.add(tmp_channel);
                        tmp_stub.withDeadlineAfter(500000, TimeUnit.MILLISECONDS).send(messageReq , MessageObserver);
                    }

                });
                ports.clear();
                while(  MessageObserver.getTotalResponses() != replicasNo ) {

                    MessageObserver.wait(500);
                }
                for (ManagedChannel mCh : channelList) {
                    mCh.shutdown();
                }
                channelList.clear();
                messageResponses.addAll( MessageObserver.updatedValue() );
                MessageObserver.clean();

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return messageResponses.get(0);
    }

    public List<String> handleSyncForExchange(MessageRequest messageReq) {

        List<MessageResponse> messageResponses = new ArrayList<>();
        MessageObserver<MessageResponse> MessageObserver = new MessageObserver<MessageResponse>() ;
        List<String> serversKeys = new ArrayList<>();


        try{

            List<Integer> ports = new ArrayList<Integer>();
            for(int i = 0 ; i < replicasNo; i++ ){
                ports.add(i);
            }
            Context ctx = Context.current().fork();

            synchronized(MessageObserver){
                ctx.run ( () -> {

                    for (int i: ports) {
                        ManagedChannel tmp_channel = ManagedChannelBuilder.forAddress(host, 8080+i ).usePlaintext().build();
                        ServerServiceGrpc.newStub(channel);
                        ServerServiceGrpc.ServerServiceStub tmp_stub = ServerServiceGrpc.newStub(tmp_channel);
                        channelList.add(tmp_channel);
                        tmp_stub.send(messageReq , MessageObserver);
                    }

                });
                ports.clear();
                while(  MessageObserver.getTotalResponses() != replicasNo ) {

                    MessageObserver.wait(500);
                }
                for (ManagedChannel mCh : channelList) {
                    mCh.shutdown();
                }
                channelList.clear();
                messageResponses.addAll( MessageObserver.updatedValue() );
                MessageObserver.clean();

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(MessageResponse rep : messageResponses){
            serversKeys.add(rep.getMessage());
        }
        return serversKeys ;

    }
    public boolean verifySignature(String message, byte[] encryptedMessageHash, List<PublicKey> publicKey) {


        byte[] decryptedMessageHash = null;
        Cipher cipher = null;
        int total;

        for(PublicKey pk : publicKey) {
            try {

                cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, pk);
                decryptedMessageHash = cipher.doFinal(encryptedMessageHash);

            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

            MessageDigest md = null;

            try {

                md = MessageDigest.getInstance("SHA-256");

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            byte[] newMessageHash = md.digest(message.getBytes(StandardCharsets.UTF_8));

            if( Arrays.equals(decryptedMessageHash, newMessageHash) ){
                return true;
            }
        }
        return false;
    }

    public void shutDownChannel() {
        channel.shutdown();
    }
}
