package pt.tecnico.sec;

import java.io.*;
import java.nio.charset.StandardCharsets;
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


    private int SSID;
    int SeqNo = 0;
    PublicKey serverPK = null;


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

    public int getSeqNo() {
        return SeqNo;
    }

    public int getSSID() {
        return SSID;
    }

    public String getPublicKey(String alias){
        Certificate certificate = null;
        PublicKey publicKeyClient = null;
        try {
            certificate = keyStore.getCertificate(alias);
            publicKeyClient = certificate.getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        byte[] byte_pubkey = publicKeyClient.getEncoded();
        String pbKey =  Base64.getEncoder().encodeToString(byte_pubkey);

        return pbKey;

    }

    public PublicKey getServerPK() {
        return serverPK;
    }

    public void exchangeKeys() {

        String publicKeyClient = this.getPublicKey("client_"+this.id);

        String message = "0;"+publicKeyClient;
        String serverKey = _frontend.exchange(message);
        PublicKey publicKey = null;
        byte[] byte_Serverpubkey;

        try {
            byte_Serverpubkey = Base64.getDecoder().decode(serverKey);
            KeyFactory keyFactory = null;
            keyFactory = KeyFactory.getInstance("DSA");
            serverPK =  keyFactory.generatePublic(new X509EncodedKeySpec(byte_Serverpubkey));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }


    }

    public void connect() {

        byte[] signature = null;
        //signature = getSignature("SYN")
        String messageResponse = _frontend.send("SYN", signature, serverPK, SSID , SeqNo);
        SSID =  Integer.parseInt(messageResponse);

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

    public int openAccount( String accountAlias ) {
        loadKeyStore("password");

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "1;"+publicKeyClient+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo);
        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        String[] params = messageResponse.split(";");

        String status = params[2];

        SeqNo ++;
        if(status.equals("200"))
            return 0;
        else
            return -1;
    }

    public int sendAmount(String sourceAlias, String destinationAlias, int amount ) {
        loadKeyStore("password");

        String sourcePK = this.getPublicKey(sourceAlias);
        String destPK = this.getPublicKey(destinationAlias);
        String message = "2;"+sourcePK+";"+destPK+";"+Integer.toString(amount)+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo);
        byte[] signature = null;
        signature = getSignature(sourceAlias , message);
        String messageResponse = _frontend.send(message, signature , serverPK, SSID , SeqNo);
        SeqNo ++;
        String[] params = messageResponse.split(";");

        String status = params[2];

        SeqNo ++;
        if(status.equals("200"))
            return 0;
        else if(status.equals("400"))
            return 1;
        else
            return -1;

    }

    public String checkAccount(String accountAlias ) {
        loadKeyStore("password");

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "3;"+publicKeyClient+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo);
        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        SeqNo ++;
        String[] params = messageResponse.split(";");

        String status = params[2];
        String balance = params[3];

        String result;

        SeqNo ++;
        if(status.equals("200"))
            return balance;
        else if(status.equals("201") ){
            result = balance;
            for (int i = 4; i < params.length ; i++ ) {
                result.concat( "\n"+params[i] );
            }
            return result;
        }
        else
            return "-1";
    }

    public int receiveAmount(String accountAlias ) {
        loadKeyStore("password");

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "4;"+publicKeyClient+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo);

        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        SeqNo ++;

        String[] params = messageResponse.split(";");
        String status = params[2];

        SeqNo ++;
        if(status.equals("200"))
            return 0;
        else
            return -1;
    }

    public String audit(String accountAlias ) {
        loadKeyStore("password");

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "5;"+publicKeyClient+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo);
        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        SeqNo ++;
        String result="";

        String[] params = messageResponse.split(";");

        String status = params[2];

        SeqNo ++;
        if(status.equals("200")){
            for (int i = 3; i < params.length ; i++ ) {
                result.concat( "\n"+params[i] );
            }
            return result;
        }
        else
            return "-1";
    }

    public int closeConnection( ) {
        byte[] signature = null;

        signature = getSignature( "FIN" , "client_"+this.id );
        String messageResponse = _frontend.send("FIN", signature, serverPK, SSID , SeqNo);

        String[] params = messageResponse.split(";");

        String status = params[2];

        SeqNo ++;
        if(status.equals("200"))
            return 0;
        else
            return 1;

    }

    public byte[] getSignature(String alias, String message) {

        byte[] digitalSignature = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] messageHash = md.digest(message.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = null;

        try {

            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(alias, "password".toCharArray() ));
            digitalSignature = cipher.doFinal(messageHash);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        return digitalSignature;
    }
}
