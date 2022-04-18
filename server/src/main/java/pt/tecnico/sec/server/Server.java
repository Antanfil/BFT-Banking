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

    transient KeyStore keyStore;
    String lastMessage = "";
    transient ArrayList<MessageLog> logger = new ArrayList<MessageLog>();
    String serverName = "";


    public Server(String serverName) {
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            this.serverName = serverName;
            keyStore.load(new FileInputStream(this.serverName), "password".toCharArray());

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
    public String getLastMessage() {
        return lastMessage;
    }

    public String exchangeKeys(String id, String clientKey) {
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(this.serverName), "password".toCharArray());

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

    public String createConnection( String id , String SID) {
        ClientS client = clients.get( id );

        client.setSID(  Integer.parseInt(SID) );
        client.setSeqNo(0);
        return Integer.toString( 200);

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

    public int verifySessionData(String id, String sid , String seqNo) {
        ClientS client = clients.get( id );

        System.out.println("Provided: \n\n");
        System.out.println("Clid: "+ id + " SID: "+ sid + " SeqNo: " +seqNo);

        System.out.println("Existing: \n\n");
        System.out.println("Clid: "+ client.getId() + " SID: "+ client.getSID() + " SeqNo: " + client.getSeqNo());

        if( Integer.toString(client.getSID()).equals(sid) &&
                Integer.toString(client.getSeqNo() + 1).equals(seqNo) ){
            client.setSeqNo( Integer.parseInt(seqNo)) ;
            return 0;
        }
        if(Integer.toString(client.getSID()).equals(sid) &&
                client.getSeqNo() + 1 > Integer.parseInt(seqNo) ){
            return -2;
        }
        System.out.println("RETURN -1");
        return -1;

    }

    public String handleMessage(String messageReq, byte[] signature) {

        String[] params = messageReq.split(";");
        String msg= "404";

        ClientS client = clients.get( params[1] );
        int transactionId = Integer.parseInt(params[2]) * Integer.parseInt(params[3]) ;
        switch(params[0]) {
            case "1":
                msg = openAccount( client , params[4] );
                this.logMessage(messageReq, signature);
                break;
            case "2":
                msg = sendAmount( client , params[4] , params[5] , Integer.parseInt(params[6]) , transactionId );
                this.logMessage(messageReq, signature);
                break;
            case "3":
                msg = checkAccount( client , params[4] );
                break;
            case "4":
                msg = receiveAmount( client , params[4] );
                this.logMessage(messageReq, signature);
                break;
            case "5":
                msg = auditAccount( client , params[4] );
                break;
        }
        lastMessage = params[2]+";"+params[3]+";"+msg;

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
        if (sourceAccount.equals(destAccount))
            return "401";
        else if(amount <0)
            return "402";
        Transaction t = client.sendAmount(stringToKey( sourceAccount ) , stringToKey( destAccount ) , amount , tid );
        if (t == null)
            return "400";
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
        //saveState();

    }

    public void logMessage(String message, byte[] signature){
        MessageLog log = new MessageLog(message,signature);
        this.logger.add(log);
    }

    public void saveState(){
        try {
            String text = serverName.split("\\.")[0];
            text = text.concat(".txt");
            FileOutputStream fos = new FileOutputStream(text);
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