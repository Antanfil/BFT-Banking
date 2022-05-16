package pt.tecnico.sec.server;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class ClientS implements Serializable {

    HashMap<PublicKey , AccountRegister> accounts = new HashMap<PublicKey , AccountRegister>();     //accounts in the registers
    PublicKey clientPK;
    int id;
    int SID = -1;
    int seqNo = 0;
    Puzzle puzzle = null;

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
            AccountRegister account = new AccountRegister( aPK );
            accounts.put( aPK , account);
            return 0;
        }
        else
            return -1;
    }

    public Transaction sendAmount(PublicKey source, PublicKey dest, int amount, int tid , int ts) {
        AccountRegister account = accounts.get(source);
        while(account.beingAccessed){
            continue;
        }
        Transaction tra = account.createOutgoingTransaction( dest , amount , tid );
        if(tra == null)
                return null;
        AccountRegister account1 = accounts.get(dest);
        account1.createIncomingTransaction( tra );
        account.setWriteTS(ts);
        return tra;

    }

    public String checkAccount(PublicKey accountPK , int ts) {
        AccountRegister account = accounts.get( accountPK );
        String msg = Integer.toString( account.getBalance() );

        ArrayList iTL = account.getIncomingTransactions();
        account.setReadTS( ts );
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

    public void receiveAmount(PublicKey accountPK , int ts) {
        AccountRegister account = accounts.get( accountPK );
        while(account.beingAccessed){
            continue;
        }
        account.acceptIncomingTransfers();
        account.setWriteTS(ts) ;
    }

    public String getHistory(PublicKey accountPK , int ts) {
        AccountRegister account = accounts.get( accountPK );
        String msg = "";
        if(account.getTransactionHistory().isEmpty()){
            account.readTS++;
            return "200";
        }
        for (Transaction transaction : account.getTransactionHistory()) {
            msg = transaction.toString()+";";
        }
        account.setReadTS(ts);
        return "201;"+msg;
    }

    public String getAccountsInfo() {
        String accountsInfo = "";

        for (Map.Entry<PublicKey, AccountRegister> set :
                accounts.entrySet()) {

            accountsInfo = accountsInfo +  ";" + set.getValue().getAccountsInfo()  ;

        }
        return accountsInfo;
    }

    public void setPuzzle(int answser) {
        Date timestamp = new Date();
        timestamp.setTime(timestamp.getTime() + 60000);
        Puzzle puz = new Puzzle(answser, timestamp);
        this.puzzle = puz;
    }

    public String verifyPuzzle(int answer) {
        Date currentTime = new Date();

        if (puzzle.getPuzzleSolution() == answer && puzzle.getTimestamp().after(currentTime)) {
            System.out.println("Puzzle solved");
            
            return "1";
        }
        else {
            return "-1";
        }
    }

}
