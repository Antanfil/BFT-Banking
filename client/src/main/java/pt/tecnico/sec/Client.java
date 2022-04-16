package pt.tecnico.sec.client;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class Client {

    transient ServerFrontend _frontend;
    private int id;
    KeyStore  keyStore;
    ArrayList<Integer> usedSids = new ArrayList<Integer>();


    private int SSID;
    int SeqNo = 0;
    List<PublicKey> serverPK = new ArrayList<>();


    public Client( ServerFrontend frontend , int id){
        _frontend = frontend;
        this.id = id;
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

    public List<PublicKey> getServerPK() {
        return serverPK;
    }

    public int exchangeKeys() {

        String publicKeyClient = this.getPublicKey("client_"+this.id);

        String message = "0;"+id+";"+publicKeyClient;
        List<String> serverKey = _frontend.exchange(message);


        PublicKey publicKey = null;
        byte[] byte_Serverpubkey;
        for(String s : serverKey){
            try {
                byte_Serverpubkey = Base64.getDecoder().decode( s );
                KeyFactory keyFactory = null;
                keyFactory = KeyFactory.getInstance("RSA");
                serverPK.add( keyFactory.generatePublic(new X509EncodedKeySpec(byte_Serverpubkey)) );

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return -1;
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
                return -1;
            }
        }
            return 0;

    }

    public int connect( int iter , String password ) {
        if(iter >= 10){
            return -2;
        }

        Random random = new Random();
        int x = random.nextInt(100);
        while( usedSids.contains(x) ){
            random = new Random();
            x = random.nextInt(100);
        }
        usedSids.add(x);

        byte[] signature = null;
        signature = getSignature("client_"+id ,"SYN;"+id+";"+x , password);
        String messageResponse = _frontend.connect("SYN;"+id+";"+x, signature, serverPK );
        if(messageResponse == "-2"){
            return connect( iter +1, password);
        }
        SSID =  x ;
        SeqNo = 1;

        return 0;
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

    public int openAccount( String accountAlias , int iter, String password) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "1;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;
        byte[] signature = null;
        signature = getSignature("client_"+id , message, password);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return openAccount( accountAlias , iter +1 , password );
        }
        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200"))
            return 0;
        else if(status.equals("403"))
            return -3;

        else
            return -1;
    }

    public int sendAmount(String sourceAlias, String destinationAlias, int amount, int iter, String password ) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String sourcePK = this.getPublicKey(sourceAlias);
        String destPK = this.getPublicKey(destinationAlias);
        String message = "2;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+sourcePK+";"+destPK+";"+Integer.toString(amount);
        byte[] signature = null;
        signature = getSignature(sourceAlias , message, password);
        String messageResponse = _frontend.send(message, signature , serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            sendAmount( sourceAlias , destinationAlias , amount, iter+1, password);
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

    public String checkAccount(String accountAlias, int iter, String password ) {
        if(iter >= 10){
            return "-2";
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "3;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;
        byte[] signature = null;
        signature = getSignature(accountAlias , message, password);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return checkAccount( accountAlias , iter +1, password);
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
                result = result + "\n"+params[i] ;
            }
            return result;
        }
        else
            return "-1";
    }

    public int receiveAmount(String accountAlias, int iter, String password ) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "4;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;

        byte[] signature = null;
        signature = getSignature(accountAlias , message, password);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return receiveAmount( accountAlias , iter +1, password );
        }

        String[] params = messageResponse.split(";");
        String status = params[2];

        incSeqNo();
        if(status.equals("200"))
            return 0;
        else
            return -1;
    }

    public String audit(String accountAlias, int iter, String password ) {
        if(iter >= 10){
            return "-2";
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);

        String message = "5;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient;
        byte[] signature = null;
        signature = getSignature(accountAlias , message , password);
        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo);
        if(messageResponse == "-2"){
            return audit( accountAlias , iter +1, password);
        }

        String result="";

        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200")) {
                return "2";
        }
        else if(status.equals("201")){
            for (int i = 3; i < params.length ; i++ ) {
                result = result +  "\n"+params[i] ;
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
        signature = getSignature( "client_"+id ,message, "password");
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

    public byte[] getSignature(String alias, String message, String password) {
        System.out.println("Signing outgoing message ...");
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
        System.out.println("Message Signed !!");
        return digitalSignature;
    }
}
