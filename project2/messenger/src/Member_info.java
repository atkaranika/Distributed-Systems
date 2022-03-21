import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;


public class Member_info implements Serializable{
    private InetSocketAddress address;
    private int port ;
    private String member_id ;
    private int udp_port ;
    public InetSocketAddress getAddress() {
        return address;
    }
    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getMember_id() {
        return member_id;
    }
    public void setMember_id(String member_id) {
        this.member_id = member_id;
    }
    public int getUdp_port() {
        return udp_port;
    }
    public void setUdp_port(int udp_port) {
        this.udp_port = udp_port;
    }
    
}