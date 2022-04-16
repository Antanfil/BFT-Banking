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


    @Override
    public void send(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        String messageReq = request.getMessage();
        System.out.println(messageReq);
        ByteString signHashResponse = request.getHash();
        byte[] signature = signHashResponse.toByteArray();

        String[] params = messageReq.split(";");
        if (params[0].equals("0")) {
            String serverPublicKey = _server.exchangeKeys(params[1], params[2]);
            MessageResponse resp = MessageResponse.newBuilder().setMessage(serverPublicKey).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();

            _server.saveState();

        } else if (params[0].equals("SYN")) {
            if (!verifyMessage(messageReq, signature, _server.getClientPublicKey(params[1])) ) {
                responseObserver.onError(null);
            }
            String sid = _server.createConnection(params[1]);
            ByteString signatureResp = ByteString.copyFrom( _server.getServerSignature(sid) );

            MessageResponse resp = MessageResponse.newBuilder().setMessage(sid).setHash(signatureResp).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();

            _server.saveState();

        } else if (params[0].equals("FIN")) {


            if (!verifyMessage(messageReq, signature, _server.getClientPublicKey(params[1]))) {
                responseObserver.onError(null);
            }
            int validity = _server.verifySessionData(params[1], params[2], params[3]);
            if (validity == -2 ) {

                String repeated = _server.getLastMessage();
                ByteString repeatedResp = ByteString.copyFrom(_server.getServerSignature(repeated));

                MessageResponse resp = MessageResponse.newBuilder().setMessage(repeated).setHash(repeatedResp).build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
                _server.saveState();
            }
            String messageResp = _server.closeConnection(params[1]);
            ByteString signatureResp = ByteString.copyFrom(_server.getServerSignature(messageResp));

            MessageResponse resp = MessageResponse.newBuilder().setMessage(messageResp).setHash(signatureResp).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
            _server.saveState();
        } else {
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
                ByteString repeatedResp = ByteString.copyFrom(_server.getServerSignature(repeated));

                MessageResponse resp = MessageResponse.newBuilder().setMessage(repeated).setHash(repeatedResp).build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
                _server.saveState();
            }
            if(validity == -1){
                responseObserver.onError(null);
            }

            String messageResp = _server.handleMessage(messageReq, signature);
            ByteString signatureResp = ByteString.copyFrom(_server.getServerSignature(messageResp));

            MessageResponse resp = MessageResponse.newBuilder().setMessage(messageResp).setHash(signatureResp).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
            _server.saveState();
        }

    }

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


}