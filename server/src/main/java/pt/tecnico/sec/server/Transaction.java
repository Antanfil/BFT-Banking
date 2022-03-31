package pt.tecnico.sec.server;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;

public class Transaction implements Serializable {

    int id;
    PublicKey sourceAccountPK;
    PublicKey destAccountPK;
    int amount;
    // 0 - completed ; 1 - awaiting confirmation
    int status;

    public Transaction(int tid , PublicKey sourceAccountPK, PublicKey destAccountPK, int amount, int status) {
        this.sourceAccountPK = sourceAccountPK;
        this.destAccountPK = destAccountPK;
        this.amount = amount;
        this.status = status;
        this.id = tid;
    }

    @Override
    public String toString() {
        byte[] byte_pubkey = sourceAccountPK.getEncoded();
        String source =  Base64.getEncoder().encodeToString(byte_pubkey);
        byte[] byte_pubkey1 = destAccountPK.getEncoded();
        String dest =  Base64.getEncoder().encodeToString(byte_pubkey);

        return "Transaction{" +
                "id=" + id +
                ", ammount=" + amount +
                ", status=" + status +
                ",\n sourceAccountPK=" + source +
                ",\n destAccountPK=" + dest +
                '}';
    }

    public PublicKey getSourceAccountPK() {
        return sourceAccountPK;
    }

    public void setSourceAccountPK(PublicKey sourceAccountPK) {
        this.sourceAccountPK = sourceAccountPK;
    }

    public PublicKey getDestAccountPK() {
        return destAccountPK;
    }

    public void setDestAccountPK(PublicKey destAccountPK) {
        this.destAccountPK = destAccountPK;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void conclude(){
        this.status = 0;
    }
}
