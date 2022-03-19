import java.net.InetAddress;

public class client_alive {
    private InetAddress address;
    private int port ;
    private long time ;
    private int signaled;
    private int current_requests;
    public InetAddress getAddress() {
        return address;
    }
    public void setAddress(InetAddress address) {
        this.address = address;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }
    public int getSignaled() {
        return signaled;
    }
    public void setSignaled(int signaled) {
        this.signaled = signaled;
    }
    public int getCurrent_requests() {
        return current_requests;
    }
    public void setCurrent_requests(int current_requests) {
        this.current_requests = current_requests;
    }
    

}