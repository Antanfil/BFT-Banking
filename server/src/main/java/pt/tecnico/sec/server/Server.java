package pt.tecnico.sec.server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class Server implements Serializable {

    PublicKey key = null;
    HashMap<String , ClientS> clients = new HashMap<String , ClientS>();
    ArrayList<Integer> usedSids = new ArrayList<Integer>();
    transient KeyStore keyStore;

    public Server() {
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream("server.p12"), "password".toCharArray());

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

    public String exchangeKeys(String id, String clientKey) {
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream("server.p12"), "password".toCharArray());

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

        if( !clients.containsKey(id) ){
            ClientS client = new ClientS(id, stringToKey(clientKey));
            clients.put(id , client );
        }

        Certificate certificate = null;
        PublicKey publicKeyClient = null;
        try {
            certificate = keyStore.getCertificate("server");
            publicKeyClient = certificate.getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        byte[] byte_pubkey = publicKeyClient.getEncoded();
        String pbKey =  Base64.getEncoder().encodeToString(byte_pubkey);

        return pbKey;
    }

    public PublicKey stringToKey(String publicKey) {
        PublicKey newPublicKey = null;

        byte[] byteServerPubKey = Base64.getDecoder().decode(publicKey);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            newPublicKey =  keyFactory.generatePublic(new X509EncodedKeySpec(byteServerPubKey));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return newPublicKey ;
    }

    public PublicKey getClientPublicKey(String id) {
        ClientS client = clients.get( id );
        return client.getClientPK() ;
    }

    public String createConnection( String id) {
        ClientS client = clients.get( id );
        boolean exists = false;
        Random random = new Random();
        int x = random.nextInt(100);
        while( usedSids.contains(x) ){
            random = new Random();
            x = random.nextInt(100);
        }
        usedSids.add(x);
        client.setSID(x);
        client.setSeqNo(0);
        return Integer.toString(x);

    }

    public byte[] getServerSignature( String message) {

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
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey("server", "password".toCharArray() ));
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

    public boolean verifySessionData(String id, String sid , String seqNo) {
        ClientS client = clients.get( id );

        if( Integer.toString(client.getSID()).equals(sid) &&
                                        Integer.toString(client.getSeqNo() + 1).equals(seqNo) ){
            client.setSeqNo( Integer.parseInt(seqNo) );
            return true;
        }
        return false;

    }

    public String handleMessage(String messageReq) {

        String[] params = messageReq.split(";");
        String msg= "404";

        ClientS client = clients.get( params[1] );
        int transactionId = Integer.parseInt(params[2]) * Integer.parseInt(params[3]) ;
        switch(params[0]) {
            case "1":
                msg = openAccount( client , params[4] );
                break;
            case "2":
                msg = sendAmount( client , params[4] , params[5] , Integer.parseInt(params[6]) , transactionId );
                break;
            case "3":
                msg = checkAccount( client , params[4] );
                break;
            case "4":
                msg = receiveAmount( client , params[4] );
                break;
            case "5":
                msg = auditAccount( client , params[4] );
                break;
        }

        return params[2]+";"+params[3]+";"+msg ;
    }

    private String auditAccount(ClientS client, String accountPK) {
        return client.getHistory( stringToKey(accountPK) );
    }

    private String receiveAmount( ClientS client, String accountPK ) {
        client.receiveAmount( stringToKey(accountPK) );
        return "200";
    }

    private String checkAccount(ClientS client, String accountPK ) {
        return client.checkAccount( stringToKey(accountPK) );

    }

    private String sendAmount(ClientS client , String sourceAccount, String destAccount, int amount , int tid) {
        client.sendAmount(stringToKey( sourceAccount ) , stringToKey( destAccount ) , amount , tid );

        return "200";
    }

    public String openAccount( ClientS client , String accountPublicKey){

        PublicKey aPK = stringToKey(accountPublicKey);
        int status = client.createAccount( aPK );
        if(status == -1){
            return "403";
        }
        else
            return "200";

    }

    public String closeConnection(String id) {
        ClientS client = clients.get( id );
        String msg = client.getSID()+";"+client.getSeqNo()+";200";
        client.setSID(-1);
        client.setSeqNo(0);
        return msg;
    }

    public void shutDown() {
        Set<String> ids = clients.keySet();
        for (String id : ids ) {
            closeConnection( id );
        }
        saveState();

    }

    public void saveState(){
        try {
            FileOutputStream fos = new FileOutputStream("server.txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            fos.close();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}