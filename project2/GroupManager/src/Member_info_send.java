/*
    A class to send the member list to the messengers with the appropriate info
*/
import java.io.Serializable;
import java.net.InetSocketAddress;



public class Member_info_send  implements Serializable{
    private InetSocketAddress address;
    private int port ;//tcp port with the communication with groupManager
    private int udp_port ;//udp port for the messengers communication
    private String member_id ;
    
 
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
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((member_id == null) ? 0 : member_id.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Member_info_send other = (Member_info_send) obj;
        if (member_id == null) {
            if (other.member_id != null)
                return false;
        } else if (!member_id.equals(other.member_id))
            return false;
        return true;
    }

}
