package pt.tecnico.sec.server;

import java.security.PublicKey;
import java.util.ArrayList;

public class ClientS {

    ArrayList<Account> accounts = new ArrayList<Account>();
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
}
