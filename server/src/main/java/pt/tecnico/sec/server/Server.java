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
import java.lang.String;

public class Server implements Serializable {

    FrontendBroadcast frontend;
    List<PublicKey> otherServersPks = new ArrayList<>();

    int reads = 0;
    boolean writeIsQueued = false;
    String lastMessage = "";
    String serverName = "";
    PublicKey key = null;
    transient KeyStore keyStore;

    HashMap<String , ClientS> clients = new HashMap<String , ClientS>();
    ArrayList<Integer> sidList = new ArrayList<Integer>();
    HashMap<Integer , String> echoList = new HashMap<>();
    List<MessageLog> logger = new ArrayList<>();


    public List<PublicKey> getOtherServersPks() {
        return otherServersPks;
    }

    public Server(String serverName , int thisPort, String host , int port, int replicas) {
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            this.serverName = serverName;
            keyStore.load(new FileInputStream(this.serverName), "password".toCharArray());
            frontend = new FrontendBroadcast(thisPort , host , port , replicas);

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

    /*
    * MESSAGE HANDLER ============================
    */

    public String handleMessage(String messageReq, byte[] signature) {

        String[] params = messageReq.split(";");
        String msg= "404";
        String truth ="";

        ClientS client = clients.get( params[1] );
        int transactionId = Integer.parseInt(params[2]) * Integer.parseInt(params[3]) ;
        switch(params[0]) {
            case "1":
                System.out.println("Operation is open account. \n -------- \n");
                msg = openAccount( client , params[4] , params[5]);
                this.logMessage(messageReq, signature);
                break;
            case "2":
                System.out.println("Operation is send amount. \n -------- \n");
                writeIsQueued = true;
                while(reads != 0){
                }
                truth = assureTruthfulness( messageReq );
                System.out.println("Truth- "+ truth +" \n -------- \n");
                if (truth.equals("error"))
                    return "-1";
                String[] truthparams = truth.split(";");
                msg = sendAmount( client , truthparams[4] , truthparams[5] , Integer.parseInt(truthparams[6]) , transactionId , Integer.parseInt(truthparams[7]) );
                echoList.clear();
                this.logMessage(messageReq, signature);
                writeIsQueued = false;
                break;
            case "3":
                System.out.println("Operation is check account. \n -------- \n");
                while(writeIsQueued){
                }
                reads++;
                msg = checkAccount( client , params[4] , Integer.parseInt(params[5]) );
                reads--;
                break;
            case "4":
                System.out.println("Operation is receive amount.  \n -------- \n");
                writeIsQueued = true;
                while(reads != 0){
                }
                truth = assureTruthfulness( messageReq);
                if (truth.equals("error"))
                    return "-1";
                String[] truthparam = truth.split(";");
                msg = receiveAmount( client , truthparam[4] , Integer.parseInt( truthparam[5]) );
                echoList.clear();
                this.logMessage(messageReq, signature);
                writeIsQueued = false;
                break;
            case "5":
                System.out.println("Operation is audit account. \n -------- \n");
                while(writeIsQueued){

                }
                reads ++;
                msg = auditAccount( client , params[4], Integer.parseInt(params[5]) );
                reads --;
                break;
        }
        lastMessage = params[2]+";"+params[3]+";"+msg;

        return params[2]+";"+params[3]+";"+msg ;
    }

    /*
    * SERVER OPERATIONS ON ACCOUNTS
    * */

    public String openAccount( ClientS client , String accountPublicKey, String ts){

        PublicKey aPK = stringToKey(accountPublicKey);
        int status = client.createAccount( aPK );
        if(status == -1){
            return "o;0;NOK;" + Integer.toString(frontend.getOwnPort()-8080 ) + ";403";
        }
        else
            return "o;0;ACK;" +Integer.toString(frontend.getOwnPort()-8080   ) + ";200";

    }

    private String sendAmount(ClientS client , String sourceAccount, String destAccount, int amount , int tid , int wts) {

        System.out.println( "-----------\nAccounts current write ts - " + client.accounts.get(stringToKey(sourceAccount) ).getWriteTS() );
        System.out.println( "Received write ts - " + wts + "\n------------\n");

        if( wts <= client.accounts.get(sourceAccount).getWriteTS()) {
            return "w;" + client.accounts.get(sourceAccount).getWriteTS() + ";NOK;" + Integer.toString(frontend.getOwnPort()-8080  ) + ";403";
        }
        client.accounts.get(sourceAccount).setWriteTS(wts);
        if (sourceAccount.equals(destAccount))
            return "w;"+ wts +";NOK;" + Integer.toString(frontend.getOwnPort()-8080 ) + ";401";
        else if(amount <0)
            return "w;" + wts + "NOK;" + Integer.toString(frontend.getOwnPort()-8080  ) + ";402";
        Transaction t = client.sendAmount(stringToKey( sourceAccount ) , stringToKey( destAccount ) , amount , tid , wts );
        if (t == null)
            return "w;" + wts + ";NOK;" + Integer.toString(frontend.getOwnPort()-8080  ) + ";400";


        return "w;" + wts + ";ACK;" + Integer.toString(frontend.getOwnPort()-8080  ) + ";200";
    }

    private String checkAccount(ClientS client, String accountPK , int rts ) {

        System.out.println( "-----------\nAccounts current read ts - " + client.accounts.get(stringToKey(accountPK) ).getReadTS() );
        System.out.println( "Received read ts - " + rts + "\n------------\n");

        if( rts <= client.accounts.get(stringToKey(accountPK) ).getReadTS()) {
            return "r;" + client.accounts.get(stringToKey(accountPK)).getReadTS() + ";NOK;" + Integer.toString(frontend.getOwnPort()-8080  ) + ";400";
        }
        return "r;" + client.accounts.get(stringToKey(accountPK)).getReadTS()+  ";ACK;" + Integer.toString(frontend.getOwnPort()-8080 )+ ";" + client.checkAccount( stringToKey(accountPK) , rts );

    }

    private String receiveAmount( ClientS client, String accountPK , int wts ) {

        System.out.println( "-----------\nAccounts current write ts - " + client.accounts.get(stringToKey(accountPK) ).getWriteTS() );
        System.out.println( "Received write ts - " + wts + "\n------------\n");


        if( wts <= client.accounts.get(accountPK).getWriteTS()) {
            return "w;" + client.accounts.get(accountPK).getWriteTS() + ";NOK;" + Integer.toString(frontend.getOwnPort()-8080  )+ ";400";
        }
        client.receiveAmount( stringToKey(accountPK) , wts );
        return "w;" + wts + ";ACK;"+ Integer.toString(frontend.getOwnPort()-8080  ) + ";200";
    }

    private String auditAccount(ClientS client, String accountPK , int rts) {

        System.out.println( "-----------\nAccounts current read ts - " + client.accounts.get(stringToKey(accountPK) ).getReadTS() );
        System.out.println( "Received read ts - " + rts + "\n------------\n");


        if( rts <= client.accounts.get(stringToKey(accountPK) ).getReadTS()) {
            return "w;" + client.accounts.get(stringToKey(accountPK) ).getReadTS() + ";NOK;" + Integer.toString(frontend.getOwnPort()-8080  ) + ";400";
        }
        return "r;" + client.accounts.get(stringToKey(accountPK)).getReadTS()+";ACK;" + Integer.toString(frontend.getOwnPort()-8080  ) +  ";" + client.getHistory( stringToKey(accountPK) , rts );
    }

    public String closeConnection(String id) {
        ClientS client = clients.get( id );
        String msg = client.getSID()+";"+client.getSeqNo()+";c;0;ACK:"+Integer.toString(frontend.getOwnPort()-8080  )+";200";
        client.setSID(-1);
        client.setSeqNo(0);
        return msg;
    }

    /*
    * INITIALIZATION OF A CLIENTS CONNECTION
    */

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

    public String createConnection( String id , String SID) {
        ClientS client = clients.get( id );
        if(sidList.contains(Integer.parseInt(SID)))
            return "400";
        sidList.add(Integer.parseInt(SID));
        client.setSID(  Integer.parseInt(SID) );
        client.setSeqNo(0);
        String accountsTS = client.getAccountsInfo();
        String msg = "200;0;0;0;ACK;"+Integer.toString(frontend.getOwnPort()-8080  )+ accountsTS;
        return msg;

    }



    /*
    * SECURITY CHECKS (INTEGRITY AND FRESHNESS) RELATED FUNCTIONS
    */
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

    public void logMessage(String message, byte[] signature){
        MessageLog log = new MessageLog(message,signature);
        logger.add(log);
    }



    /*
    * PRESERVE THE STATE OF THE SERVER
    */

    public void shutDown() {
        Set<String> ids = clients.keySet();
        for (String id : ids ) {
            closeConnection( id );
        }
        saveState();

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

    /*
    * BROADCAST RECEIVED RELATED OPERATIONS
    */


    public void receiverBroadcastPK(String serverpk){
        otherServersPks.add(stringToKey(serverpk));
    }

    public void cleanBroadcastList() {
        otherServersPks.clear();
    }

    public void saveEchoMessage(String messageReq, String param , byte[] signature) {

        String message = messageReq.substring( 12 , messageReq.length() );
        echoList.put(Integer.parseInt(param) , message);
        int q = checkForQuorum();
        if( q != -1){
            handleMessage( echoList.get(q) , signature);
        }

    }

    public boolean contains(ArrayList<Integer> usedId , int id){
        for(int i : usedId ){
            if (id == i){
                return true;
            }
        }
        return false;
    }



    /*
    * BROADCAST SEND RELATED OPERATIONS
    */
    public void propagatePK(){
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(serverName), "password".toCharArray());

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

        frontend.broadcastPK(pbKey);
    }

    public int checkForQuorum(){
        System.out.println("QUORUM CHECK\n--------\n");
        String mainMessage = null;
        int quorum = 0;
        int id = -1;
        boolean checkedEverything = false;
        ArrayList<Integer> usedId = new ArrayList<>();

        while( !checkedEverything ){

            for (int a = 0; a < frontend.getReplicasNo(); a++) {
                if (echoList.containsKey(a)) {
                    if (mainMessage == null && !contains(usedId, a)) {
                        mainMessage = echoList.get(a);
                        id = a;
                        usedId.add(id);
                    }

                }
            }
            for (int i = 0; i < frontend.getReplicasNo(); i++) {
                if (echoList.containsKey(i)) {
                    System.out.println("Echo from server "+i+" - "+echoList.get(i) + "\n-----");
                    if (mainMessage.equals(echoList.get(i) ) ) {
                        quorum++;
                    }
                }
            }
            if (quorum >= frontend.getReplicasNo() / 2 + 1) {
                return id;
            }
            if(mainMessage == null){
                checkedEverything=true;
            }
            mainMessage = null;
            quorum = 0;
        }

        return -1;
    }

    private String assureTruthfulness( String messageReq )  {
        int truthAsserted = 0;
        int serverid = frontend.getOwnPort()-8080 ;
        String msg = "BROADCAST;"+ serverid + ";"+ messageReq;

        echoList.put(serverid , messageReq);

        frontend.Broadcast(msg , getServerSignature(msg) );

        while( truthAsserted != frontend.getReplicasNo() ){
            int q = checkForQuorum();
            if( q != -1){
                return echoList.get(q);
            }

            truthAsserted++;
        }
        return "error";
    }

    /*
    AUXILIARY FUNCTIONS
    */


    public String getLastMessage() {
        return lastMessage;
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

}