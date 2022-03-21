import java.net.InetAddress;
import java.io.Serializable;
public class messages implements Serializable {
    private String message;
    private int thread_from;
    private  InetAddress ip_from;
    private int port_from; 
    private int seqno;
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public int getThread_from() {
        return thread_from;
    }
    public void setThread_from(int thread_from) {
        this.thread_from = thread_from;
    }
    public InetAddress getIp_from() {
        return ip_from;
    }
    public void setIp_from(InetAddress ip_from) {
        this.ip_from = ip_from;
    }
    public int getPort_from() {
        return port_from;
    }
    public void setPort_from(int port_from) {
        this.port_from = port_from;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }    
    
    
}
