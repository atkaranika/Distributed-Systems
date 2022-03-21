import java.nio.ByteBuffer;

public class read_data {
    private int seqno;
   
    private ByteBuffer data;
    private int size;
    public int messages ;
    read_data(){
        data  = null;
        size = -10;
        messages = 0 ;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public ByteBuffer getData() {
        return data;
    }
    public void setData(ByteBuffer data) {
        this.data = data;
    }
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    
}