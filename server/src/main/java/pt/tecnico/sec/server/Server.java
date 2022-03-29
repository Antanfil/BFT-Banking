package pt.tecnico.sec.server;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;
import java.lang.Math;

public class Server {

    PublicKey key = null;
    String clientKey;

    public Server() {
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("DSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.initialize(1024 );
        KeyPair KeyPair = keyGen.generateKeyPair();
        key = KeyPair.getPublic();

    }

    public String getKey() {
       // System.out.println(key.toString());
        byte[] byte_pubkey = key.getEncoded();
        String pbKey =  Base64.getEncoder().encodeToString(byte_pubkey);
        return pbKey;
    }

    public void receiveKey( String key ){
        clientKey = key;
    }
}