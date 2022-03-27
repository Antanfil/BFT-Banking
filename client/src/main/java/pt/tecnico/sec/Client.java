package pt.tecnico.sec;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import java.security.*;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import pt.tecnico.sec.server.ServerFrontend;

public class Client {

    ServerFrontend _frontend;
    private int id;
    ClientKeyStore  clientKeyStore;


    public Client( ServerFrontend frontend , int id){
        _frontend = frontend;
        this.id = id;
    }


    public boolean pingWorking( ){
        String input = "App";

        String s = _frontend.Ping( input );
        System.out.println( s );
        return true;
    }

    public void createClientKeyStore() {

        try {
            clientKeyStore = new ClientKeyStore();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public PublicKey exchangeKeys() {
        byte[] byte_pubkey = clientKeyStore.getPublicKey().getEncoded();
        String pbKey =  Base64.getEncoder().encodeToString(byte_pubkey);
        String serverKey = _frontend.exchange(pbKey);
        PublicKey publicKey = null;
        byte[] byte_Serverpubkey;

        try {
            byte_Serverpubkey = Base64.getDecoder().decode(serverKey);
            //X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(serverKey));
            KeyFactory keyFactory = null;
            keyFactory = KeyFactory.getInstance("DSA");
            publicKey =  keyFactory.generatePublic(new X509EncodedKeySpec(byte_Serverpubkey));
            //publicKey = keyFactory.generatePublic(pubKeySpec);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return publicKey;

    }

    public int connect() {

        return 1;
    }

}
