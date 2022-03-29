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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

public class Client {

    transient ServerFrontend _frontend;
    private int id;
    KeyStore  keyStore;


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

    public int getId() {
        return id;
    }

    public PublicKey exchangeKeys() {
        Certificate certificate = null;
        PublicKey publicKeyClient = null;
        try {
            certificate = keyStore.getCertificate("client_"+this.id);
            publicKeyClient = certificate.getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        byte[] byte_pubkey = publicKeyClient.getEncoded();
        String pbKey =  Base64.getEncoder().encodeToString(byte_pubkey);
        String serverKey = _frontend.exchange(pbKey);
        PublicKey publicKey = null;
        byte[] byte_Serverpubkey;

        try {
            byte_Serverpubkey = Base64.getDecoder().decode(serverKey);
            KeyFactory keyFactory = null;
            keyFactory = KeyFactory.getInstance("DSA");
            publicKey =  keyFactory.generatePublic(new X509EncodedKeySpec(byte_Serverpubkey));

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


    public void loadKeyStore( String password ){
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream("client_"+this.id+".p12"), password.toCharArray());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

    }
}
