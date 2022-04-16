package pt.tecnico.sec.server;

public class MessageLog {
    private String message;
    private byte[] signature;
    public MessageLog(String message, byte[] signature){
        this.message = message;
        this.signature = signature;
    }
    public String getMessage(){ return message; }
    public byte[] getSignature(){ return signature; }
    public void setMessage(String message){ this.message = message; }
    public void setSignature(byte[] signature){ this.signature = signature; }
}