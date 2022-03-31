package pt.tecnico.sec.server;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientS implements Serializable {

    HashMap<PublicKey , Account> accounts = new HashMap<PublicKey , Account>();
    PublicKey clientPK;
    int id;
    int SID = -1;
    int seqNo = 0;

    public PublicKey getClientPK() {
        return clientPK;
    }

    public int getId() {
        return id;
    }

    public ClientS(String clientId, PublicKey clientKey) {
        id = Integer.parseInt(clientId);
        clientPK = clientKey;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

    public void setSID(int SID) {
        this.SID = SID;
    }

    public int getSID() {
        return SID;
    }

    public int createAccount(PublicKey aPK) {
        if( !accounts.containsKey(aPK) ) {
            Account account = new Account( aPK );
            accounts.put( aPK , account);
            return 0;
        }
        else
            return -1;
    }

    public Transaction sendAmount(PublicKey source, PublicKey dest, int amount, int tid) {
        Account account = accounts.get(source);
        Transaction tra = account.createOutgoingTransaction( dest , amount , tid );

        Account account1 = accounts.get(dest);
        account1.createIncomingTransaction( tra );
        return tra;

    }

    public String checkAccount(PublicKey accountPK) {
        Account account = accounts.get( accountPK );
        String msg = Integer.toString( account.getBalance() );

        ArrayList iTL = account.getIncomingTransactions();
        if( iTL.isEmpty() ){
            return "200;"+msg;
        }
        else {
            for (int i = 0; i < iTL.size() ; i++) {
                msg = msg +";"+iTL.get(i).toString();
            }
            return "201;"+msg;
        }

    }

    public void receiveAmount(PublicKey accountPK) {
        Account account = accounts.get( accountPK );
        account.acceptIncomingTransfers();
    }

    public String getHistory(PublicKey accountPK) {
        Account account = accounts.get( accountPK );
        String msg = "";
        if(account.getTransactionHistory().isEmpty()){
            return "200";
        }
        for (Transaction transaction : account.getTransactionHistory()) {
            msg = transaction.toString()+";";
        }
        return "201;"+msg;
    }
}
