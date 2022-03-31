package pt.tecnico.sec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import org.apache.commons.lang3.tuple.Pair;
import pt.tecnico.sec.server.grpc.*;
import pt.tecnico.sec.server.grpc.Server.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class ServerFrontend {

    ManagedChannel channel;
    ServerServiceGrpc.ServerServiceBlockingStub stub;


    public ServerFrontend(String host, int port) {

        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = ServerServiceGrpc.newBlockingStub(channel);


    }

    public String exchange( String pbKey ){
        try {
            MessageRequest messageReq = MessageRequest.newBuilder().setMessage(pbKey).build();
            MessageResponse messageResp = MessageResponse.newBuilder().build();

            messageResp = stub.send(messageReq);
            return messageResp.getMessage();

        } catch (StatusRuntimeException e) {
            return "Caught error with description: " + e.getStatus().getDescription();
        }
    }

    public String connect(String message , byte[] signature, PublicKey serverPublicKey){


        ByteString signHash = ByteString.copyFrom(signature);
        MessageRequest messageReq = MessageRequest.newBuilder().setMessage(message).setHash(signHash).build();
        MessageResponse messageResp = MessageResponse.newBuilder().build();
        String messageResponse = "";
        String SSID = "-1";

        boolean signatureOK = false;

        while( !signatureOK ) {

            try{
                messageResp = stub.withDeadlineAfter(500000, TimeUnit.MILLISECONDS).send(messageReq);

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

    public String send(String message , byte[] signature, PublicKey serverPublicKey, int sid , int seqNo){


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
                messageResp = stub.withDeadlineAfter(500000, TimeUnit.MILLISECONDS).send(messageReq);

            } catch (StatusRuntimeException e) {

                if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    return "-2";
                }
                return "-1";
            }

            String[] params = messageResp.getMessage().split(";");
            SSID = params[0];
            SeqNo = params[1];


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

    public boolean verifySignature(String message, byte[] encryptedMessageHash, PublicKey publicKey) {

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

    public void shutDownChannel() {
        channel.shutdown();
    }
}
