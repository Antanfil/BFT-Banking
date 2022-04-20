package pt.tecnico.sec.server;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AccountRegister implements Serializable {

    PublicKey accountPK;
    int balance;
    ArrayList<Transaction> transactionHistory = new ArrayList<Transaction>() ;
    ArrayList<Transaction> incomingTransactions = new ArrayList<Transaction>() ;
    int writeTS = 0;
    int readTS = 0;
    boolean beingAccessed = false;

    public int getReadTS() {
        return readTS;
    }


    public PublicKey getAccountPK() {
        return accountPK;
    }

    public int getBalance() {
        return balance;
    }

    public ArrayList<Transaction> getIncomingTransactions() {
        return incomingTransactions;
    }

    public AccountRegister(PublicKey accountPK) {
        this.accountPK = accountPK;
        this.balance = 50 ;
        this.writeTS = 0;
        this.readTS = 0;
    }

    public Transaction createOutgoingTransaction(PublicKey dest, int amount, int tid) {
        beingAccessed = true;
        if ( amount > this.balance ){
            return null;
        }

        Transaction transaction = new Transaction(tid , accountPK , dest , amount , 1 );
        transactionHistory.add(transaction);
        balance = balance - amount;
        beingAccessed = false;
        return transaction;
    }

    public void createIncomingTransaction(Transaction transaction) {
        incomingTransactions.add(transaction);
    }

    public void acceptIncomingTransfers() {
        beingAccessed = true;
        for(Transaction incomingTransaction : incomingTransactions){
            incomingTransaction.conclude();
            balance = balance + incomingTransaction.getAmount() ;
            transactionHistory.add(incomingTransaction);
        }
        incomingTransactions.clear();
        beingAccessed = false;
    }

    public List<Transaction> getTransactionHistory(){
        return transactionHistory;
    }


    public String getAccountsInfo() { //wts;rts;accoPk;
        beingAccessed = true;
        byte[] byte_pubkey = accountPK.getEncoded();
        String pbKey =  Base64.getEncoder().encodeToString(byte_pubkey);
        beingAccessed = false;
        return pbKey+";"+Integer.toString(writeTS)+";"+Integer.toString(readTS);
    }

    public int getWriteTS() {
        return this.writeTS;
    }

    public void setWriteTS(int writeTimestamp) {
        this.writeTS = writeTimestamp;
    }

}
