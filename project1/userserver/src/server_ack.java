import java.net.InetAddress;

public class server_ack {
    private int request_id_api;
    private long time ;
    private InetAddress address;
    private int port ;
    
    public int getRequest_id_api() {
        return request_id_api;
    }
    public void setRequest_id_api(int request_id_api) {
        this.request_id_api = request_id_api;
    }
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }
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
    
}