package pt.tecnico.sec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import java.security.cert.CertificateException;

import javax.crypto.*;

public class Client {

    transient ServerFrontend _frontend;
    private int id;
    KeyStore  keyStore;
    private String password;


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

    public void incSeqNo(){
        SeqNo ++;
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

    public int exchangeKeys() {

        String publicKeyClient = this.getPublicKey("client_"+this.id);

        String message = "0;"+id+";"+publicKeyClient;
        String serverKey = _frontend.exchange(message);
        System.out.println(serverKey.toString());
        PublicKey publicKey = null;
        byte[] byte_Serverpubkey;

        try {
            byte_Serverpubkey = Base64.getDecoder().decode(serverKey);
            KeyFactory keyFactory = null;
            keyFactory = KeyFactory.getInstance("RSA");
            serverPK =  keyFactory.generatePublic(new X509EncodedKeySpec(byte_Serverpubkey));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return -1;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;


    }

    public int connect( int iter ) {
        if(iter >= 10){
            return -2;
        }

        byte[] signature = null;
        signature = getSignature("client_"+id ,"SYN;"+id);
        String messageResponse = _frontend.connect("SYN;"+id, signature, serverPK );
        if(messageResponse == "-2"){
            return connect( iter +1);
        }
        SSID =  Integer.parseInt(messageResponse);
        SeqNo = 1;

        System.out.println("\n\n\n\n"+SSID+"\n\n\n\n");
        return 0;
    }

    public void loadKeyStore( String password ){
        this.password = password;
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

    public int openAccount( String accountAlias , int iter) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "1;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;
        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return openAccount( accountAlias , iter +1);
        }
        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200"))
            return 0;
        else
            return -1;
    }

    public int sendAmount(String sourceAlias, String destinationAlias, int amount, int iter ) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String sourcePK = this.getPublicKey(sourceAlias);
        String destPK = this.getPublicKey(destinationAlias);
        String message = "2;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+sourcePK+";"+destPK+";"+Integer.toString(amount);
        byte[] signature = null;
        signature = getSignature(sourceAlias , message);
        String messageResponse = _frontend.send(message, signature , serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            sendAmount( sourceAlias , destinationAlias , amount, iter+1);
        }

        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200"))
            return 0;
        else if(status.equals("400"))
            return 1;
        else
            return -1;

    }

    public String checkAccount(String accountAlias, int iter ) {
        if(iter >= 10){
            return "-2";
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "3;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;
        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return checkAccount( accountAlias , iter +1);
        }

        String[] params = messageResponse.split(";");

        String status = params[2];
        String balance = params[3];

        String result;

        incSeqNo();
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

    public int receiveAmount(String accountAlias, int iter ) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "4;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;

        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return receiveAmount( accountAlias , iter +1);
        }

        String[] params = messageResponse.split(";");
        String status = params[2];

        incSeqNo();
        if(status.equals("200"))
            return 0;
        else
            return -1;
    }

    public String audit(String accountAlias, int iter ) {
        if(iter >= 10){
            return "-2";
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "5;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;
        byte[] signature = null;
        signature = getSignature(accountAlias , message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return audit( accountAlias , iter +1);
        }

        String result="";

        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200")){
            for (int i = 3; i < params.length ; i++ ) {
                result.concat( "\n"+params[i] );
            }
            return result;
        }
        else
            return "-1";
    }

    public int closeConnection( int iter ) {
        if(iter >= 10){
            return -2;
        }
        byte[] signature = null;

        String message = "FIN;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo) ;
        signature = getSignature( "client_"+id ,message);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return closeConnection( iter +1);
        }

        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200")) {
            _frontend.shutDownChannel();
            return 0;
        }
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
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(alias, password.toCharArray() ));
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
