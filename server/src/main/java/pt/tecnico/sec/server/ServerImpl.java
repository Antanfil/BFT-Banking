package pt.tecnico.sec.server;

import com.google.protobuf.ByteString;
import pt.tecnico.sec.server.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.sec.server.grpc.*;
import pt.tecnico.sec.server.grpc.Server.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static io.grpc.Status.INVALID_ARGUMENT;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

public class ServerImpl extends ServerServiceGrpc.ServerServiceImplBase {


    Server _server;


    public ServerImpl(Server server) {
        _server = server;
    }

    /*
    * FUNCTION WHERE A SERVER RECEIVES ALL ITS MESSAGES
    * */
    @Override
    public void send(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        String messageReq = request.getMessage();

        ByteString signHashResponse = request.getHash();
        byte[] signature = signHashResponse.toByteArray();

        String[] params = messageReq.split(";");

        /*
        * EXCHANGE OF KEYS WITH THE CLIENT
        */
        if (params[0].equals("0")) {
            System.out.println("Received exchange keys request from client\n -------- \n");

            String serverPublicKey = _server.exchangeKeys(params[1], params[2]);
            _server.saveState();
            MessageResponse resp = MessageResponse.newBuilder().setMessage(serverPublicKey).build();
            System.out.println("Public Key successfully sent. \n -------- \n");

            responseObserver.onNext(resp);
            responseObserver.onCompleted();


        }
        /*
        * CLIENT WANTS TO ESTABLISH A CONNECTION
        */
        else if (params[0].equals("SYN")) {
            System.out.println("Received connection request from client\n -------- \n");
            if (!verifyMessage(messageReq, signature, _server.getClientPublicKey(params[1])) ) {
                responseObserver.onError(null);
            }
            String status = _server.createConnection(params[1] , params[2]);
            ByteString signatureResp = ByteString.copyFrom( _server.getServerSignature(status) );

            _server.saveState();
            System.out.println("Operation finished. Returning the following message to client: \n"+ status +"\n -------- \n");
            MessageResponse resp = MessageResponse.newBuilder().setMessage(status).setHash(signatureResp).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();

        }
        /*
         * CLIENT WANTS TO TERMINATE CONNECTION
         */
        else if (params[0].equals("FIN")) {
            System.out.println("Received termination request from client\n -------- \n");

            if (!verifyMessage(messageReq, signature, _server.getClientPublicKey(params[1]))) {
                responseObserver.onError(null);
            }
            int validity = _server.verifySessionData(params[1], params[2], params[3]);
            if (validity == -2 ) {

                String repeated = _server.getLastMessage();
                ByteString repeatedResp = ByteString.copyFrom(_server.getServerSignature(repeated));

                _server.saveState();
                MessageResponse resp = MessageResponse.newBuilder().setMessage(repeated).setHash(repeatedResp).build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
            String messageResp = _server.closeConnection(params[1]);
            ByteString signatureResp = ByteString.copyFrom(_server.getServerSignature(messageResp));

            _server.saveState();
            System.out.println("Operation finished. Returning the following message to client: \n"+ messageResp +"\n -------- \n");
            MessageResponse resp = MessageResponse.newBuilder().setMessage(messageResp).setHash(signatureResp).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        }
        /*
         * OTHER REPLICAS WANT TO BROADCAST THEIR MESSAGES
         */
        else if (params[0].equals("BROADCAST")){

            System.out.println("Received Broadcast from other replicas \n -------- \n");

            verifyBroadcastMessage(messageReq , signature , _server.getOtherServersPks());
            _server.saveEchoMessage(messageReq , params[1] , signature );
            MessageResponse resp = MessageResponse.newBuilder().build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        }
        /*
         * OTHER REPLICAS SEND THEIR PK
         */
        else if (params[0].equals("INITIALIZATION")) {

            System.out.println("Received public keys from other replicas\n -------- \n");

            _server.receiverBroadcastPK(params[1]);
            MessageResponse resp = MessageResponse.newBuilder().build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        }
        /*
         * CLIENT WANTS TO PERFORM AN OPERATION
        */
        else {
            System.out.println("Received operation request from client\n -------- \n");
            if (params[0].equals("1")) {
                if (!verifyMessage(messageReq, signature, _server.getClientPublicKey(params[1]))) {
                    responseObserver.onError(null);
                }
            }
            else {
                if (!verifyMessage(messageReq, signature, _server.stringToKey(params[4]))) {
                    responseObserver.onError(null);
                }
            }

            int validity = _server.verifySessionData(params[1], params[2], params[3]);
            if (validity == -2 ) {

                String repeated = _server.getLastMessage();
                System.out.println("Sequence number is old, returning last message in memory to client\n \n"+repeated+ "\n -------- \n" );
                ByteString repeatedResp = ByteString.copyFrom(_server.getServerSignature(repeated));

                _server.saveState();
                MessageResponse resp = MessageResponse.newBuilder().setMessage(repeated).setHash(repeatedResp).build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
            if(validity == -1){
                responseObserver.onError(null);
            }

            String messageResp = _server.handleMessage(messageReq, signature);

            if (messageResp.equals("-1")) {
                responseObserver.onError(null);
            }

            ByteString signatureResp = ByteString.copyFrom(_server.getServerSignature(messageResp));

            _server.saveState();
            System.out.println("Operation finished. Returning the following message to client: \n"+ messageResp +"\n -------- \n");
            MessageResponse resp = MessageResponse.newBuilder().setMessage(messageResp).setHash(signatureResp).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        }

    }

    /*
    * VERIFIES AUTHENTICITY OF A CLIENTS MESSAGE
    * */
    public boolean verifyMessage(String message, byte[] encryptedMessageHash, PublicKey publicKey) {

        byte[] decryptedMessageHash = null;
        Cipher cipher = null;

        try {

            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
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

        return Arrays.equals(decryptedMessageHash, newMessageHash);

    }

    /*
     * VERIFIES AUTHENTICITY OF A BROADCAST MESSAGE
     * */
    public boolean verifyBroadcastMessage(String message, byte[] encryptedMessageHash, List<PublicKey> publicKeys ){
        byte[] decryptedMessageHash = null;
        Cipher cipher = null;
        int total;

        for(PublicKey pk : publicKeys) {
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

}