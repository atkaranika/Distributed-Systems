import java.net.DatagramPacket;

public class kill_request {
   private DatagramPacket packet_send;
   private int seqno;
public DatagramPacket getPacket_send() {
    return packet_send;
}
public void setPacket_send(DatagramPacket packet_send) {
    this.packet_send = packet_send;
}
public int getSeqno() {
    return seqno;
}
public void setSeqno(int seqno) {
    this.seqno = seqno;
}
   
}
