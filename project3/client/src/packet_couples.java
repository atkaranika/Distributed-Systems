
import java.net.DatagramPacket;

public class packet_couples {
    public DatagramPacket packet;
    public int seqno;
    public DatagramPacket getPacket() {
        return packet;
    }
    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    
}
