package pt.tecnico.sec.server;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Account implements Serializable {

    PublicKey accountPK;
    int balance;
    ArrayList<Transaction> transactionHistory = new ArrayList<Transaction>() ;
    ArrayList<Transaction> incomingTransactions = new ArrayList<Transaction>() ;

    public PublicKey getAccountPK() {
        return accountPK;
    }

    public int getBalance() {
        return balance;
    }

    public ArrayList<Transaction> getIncomingTransactions() {
        return incomingTransactions;
    }

    public Account(PublicKey accountPK) {
        this.accountPK = accountPK;
        this.balance = 50 ;
    }

    public Transaction createOutgoingTransaction(PublicKey dest, int amount, int tid) {
        if ( amount > this.balance ){
            return null;
        }

        Transaction transaction = new Transaction(tid , accountPK , dest , amount , 1 );
        transactionHistory.add(transaction);
        balance = balance - amount;
        return transaction;
    }

    public void createIncomingTransaction(Transaction transaction) {
        incomingTransactions.add(transaction);
    }

    public void acceptIncomingTransfers() {
        System.out.println( incomingTransactions.size() ) ;

        for(Transaction incomingTransaction : incomingTransactions){
            incomingTransaction.conclude();
            balance = balance + incomingTransaction.getAmount() ;
            transactionHistory.add(incomingTransaction);
        }
        incomingTransactions.clear();
    }

    public List<Transaction> getTransactionHistory(){
        return transactionHistory;
    }
}
