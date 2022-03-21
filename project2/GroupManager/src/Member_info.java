
/*
    A class for the appropriate info for each messenger that the GroupManager is responsible for
*/
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;


public class Member_info implements Serializable{
    private InetSocketAddress address;
    private int port ;
    private String member_id ;
    private int udp_port ;
    private Socket tcp_socket; //tcp socket open for communication with each messenger

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
    public Socket getTcp_socket() {
        return tcp_socket;
    }
    public void setTcp_socket(Socket tcp_socket) {
        this.tcp_socket = tcp_socket;
    }
    
}