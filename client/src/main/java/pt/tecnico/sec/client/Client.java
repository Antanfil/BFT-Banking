package pt.tecnico.sec.client;

import org.apache.commons.lang3.tuple.Pair;
import pt.tecnico.sec.client.ServerFrontend;

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
import java.util.*;

public class Client {

    transient ServerFrontend _frontend;
    private int id;
    private int SSID;
    int SeqNo = 0;
    KeyStore  keyStore;


    ArrayList<Integer> usedSids = new ArrayList<Integer>();
    HashMap< String ,  Integer> accountsWTS = new HashMap<>();
    HashMap< String ,  Integer> accountsRTS = new HashMap<>();
    List<PublicKey> serverPK = new ArrayList<>();


    public Client( ServerFrontend frontend , int id){
        _frontend = frontend;
        this.id = id;
    }




    /*
    * GETTERS AND SETTERS =================================
    */
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
            if(certificate == null){
                return "-4";
            }
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





    /*
    * INITIALIZATION OPERATIONS =================================
    */

    public int exchangeKeys() {

        String publicKeyClient = this.getPublicKey("client_"+this.id);

        String message = "0;"+id+";"+publicKeyClient;
        List<String> serverKey = _frontend.exchange(message);

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
        System.out.println("Server keys received - "+serverPK.size()+"\n----------\n");
        return 0;

    }

    public int connect( int iter , String password ) {
        if(iter >= 10){
            return -2;
        }
        int x;

        while(true){
            Random random = new Random();
            x = random.nextInt(100);
            while( usedSids.contains(x) ){
                random = new Random();
                x = random.nextInt(100);
            }
            usedSids.add(x);

            byte[] signature = null;
            signature = getSignature("client_"+id ,"SYN;"+id+";"+x , password);


            String messageResponse = _frontend.connect("SYN;"+id+";"+x, signature, serverPK);

            String[] params = messageResponse.split(";");
            System.out.println(messageResponse);
            if(params[0].equals("200")){
                for(int i = 6 ; i<params.length ; i = i+3){
                    accountsRTS.put( params[i] , Integer.parseInt(params[i+2]) );
                    accountsWTS.put( params[i] , Integer.parseInt(params[i+1]) );
                }
                break;
            }
            if(params[0].equals("-2")){
                return this.connect( iter +1, password);
            }
        }
        SSID =  x ;
        SeqNo = 1;

        return 0;
    }




    /*
    * PROGRAM MAIN OPERATIONS =================================
    */
    public int openAccount( String accountAlias , int iter, String password) {
        if(iter >= 10){
            return -2;
        }
        loadKeyStore(password);

        String publicKeyAccount = this.getPublicKey(accountAlias);
        if (publicKeyAccount.equals("-4")){
            return -4;
        }

        String message = "1;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyAccount+";0";
        byte[] signature = null;
        signature = getSignature("client_"+id , message, password);


        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo, 0);


        if(messageResponse == "-2"){
            return openAccount( accountAlias , iter +1 , password );
        }
        String[] params = messageResponse.split(";");

        String status = params[2];

        incSeqNo();
        if(status.equals("200")){
            accountsWTS.put(publicKeyAccount , 1);
            accountsRTS.put(publicKeyAccount , 1);
            return 0;
        }
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
        if (sourcePK.equals("-4")){
            return -4;
        }
        String destPK = this.getPublicKey(destinationAlias);
        if (destPK.equals("-4")){
            return -5;
        }

        incrementWTS(sourcePK);
        int wts = getWTSforAccount(sourcePK);

        String message = "2;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+sourcePK+";"+destPK+";"+Integer.toString(amount)+";"+Integer.toString(wts);
        byte[] signature = null;
        signature = getSignature(sourceAlias , message, password);


        String messageResponse = _frontend.send(message, signature , serverPK, SSID , SeqNo, wts);

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
        else if(status.equals("401"))
            return 2;
        else if(status.equals("402"))
            return 3;
        else
            return -1;

    }

    public String checkAccount(String accountAlias, int iter, String password ) {
        if(iter >= 10){
            return "-2";
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);
        if (publicKeyClient.equals("-4")){
            return "-4";
        }

        incrementRTS(publicKeyClient);
        int rts = getRTSforAccount(publicKeyClient);

        String message = "3;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient+";"+Integer.toString(rts);
        byte[] signature = null;
        signature = getSignature(accountAlias , message, password);


        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo, rts);
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
        if (publicKeyClient.equals("-4")){
            return -4;
        }
        
        incrementWTS(publicKeyClient);
        int wts = getWTSforAccount(publicKeyClient);

        String message = "4;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient+";"+Integer.toString(wts);

        byte[] signature = null;
        signature = getSignature(accountAlias , message, password);


        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo, wts);
        if(messageResponse == "-2"){
            return receiveAmount( accountAlias , iter +1, password );
        }



        String[] params = messageResponse.split(";");
        String status = params[2];

        incSeqNo();
        if(status.equals("200"))
            return 0;
        //else
        return -1;
    }

    public String audit(String accountAlias, int iter, String password ) {
        if(iter >= 10){
            return "-2";
        }
        loadKeyStore(password);

        String publicKeyClient = this.getPublicKey(accountAlias);
        if (publicKeyClient.equals("-4")){
            return "-4";
        }

        incrementRTS(publicKeyClient);
        int rts = getRTSforAccount(publicKeyClient);

        String message = "5;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo)+";"+publicKeyClient+";"+Integer.toString(rts);
        byte[] signature = null;
        signature = getSignature(accountAlias , message , password);


        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo, rts);
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

        //else
        return "-1";
    }





    /*
    *TERMINATION OPERATIONS ====================================
    */
    public int closeConnection( int iter ) {
        if(iter >= 10){
            return -2;
        }
        byte[] signature = null;

        String message = "FIN;"+id+";"+Integer.toString(SSID)+";"+Integer.toString(SeqNo) ;
        signature = getSignature( "client_"+id ,message, "password");

        int timestamp = 0; //TODO timestamp

        String messageResponse = _frontend.send(message, signature, serverPK, SSID , SeqNo, timestamp);
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





    /*
    * CRYPTOGRAPHY FUNCTIONS
    */
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




    /*
    * AUXILIARY FUNCTIONS =======================================
    */
    private void incrementRTS(String accPK) {

        int rts = accountsRTS.get(accPK) ;
        accountsRTS.replace( accPK , rts+1);

    }

    private void incrementWTS(String accPK) { // TODO
        int wts = accountsWTS.get(accPK) ;
        accountsWTS.replace( accPK , wts+1);
    }

    private int getWTSforAccount(String accPK) {
        return accountsWTS.get(accPK) ;
    }

    private int getRTSforAccount(String accPK) {
        return accountsRTS.get(accPK) ;
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

    public void incSeqNo(){
        SeqNo ++;
    }
}
