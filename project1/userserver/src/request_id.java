import java.net.InetAddress;
public class request_id {
    private int request_id_server;
    private int request_id_api;
    private int taken_over ;                                                //ama analifthike      
    private byte[] data_for_server;    
    private int svcid ; 
    private InetAddress address;
    private int port ;
    private long time ;
    private int signaled;
    private char OK;        
    public int  getRequest_id_server() {
        
        return request_id_server;
    }
    public void setRequest_id_server(int request_id_server) {
        this.request_id_server = request_id_server;
    }
   
    public int getRequest_id_api() {
        return request_id_api;
    }
    public void setRequest_id_api(int request_id_api) {
        this.request_id_api = request_id_api;
    }
    public int getTaken_over() {
        return taken_over;
    }
    public void setTaken_over(int taken_over) {
        this.taken_over = taken_over;
    }
    public byte[] getData_for_server() {
        return data_for_server;
    }
    public void setData_for_server(byte[] data_for_server) {
        this.data_for_server = data_for_server;
    }
    public int getSvcid() {
        return svcid;
    }
    public void setSvcid(int svcid) {
        this.svcid = svcid;
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
    public int getOK() {
        return OK;
    }
    public void setOK(char oK) {
        OK = oK;
    }

    

}