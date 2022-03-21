
import java.nio.ByteBuffer;

public class hidden_memory {
    private ByteBuffer block_data;
    private long  freshness;
    private int tmod;
    private int codename;
    private int num_of_block;
    private int seqno;

    public ByteBuffer getBlock_data() {
        return block_data;
    }
    public void setBlock_data(ByteBuffer block_data) {
        this.block_data = block_data;
    }

    public long getFreshness() {
        return freshness;
    }
    public int getTmod() {
        return tmod;
    }
    public void setTmod(int tmod) {
        this.tmod = tmod;
    }
    public int getNum_of_block() {
        return num_of_block;
    }
    public void setNum_of_block(int num_of_block) {
        this.num_of_block = num_of_block;
    }
    public int getCodename() {
        return codename;
    }
    public void setCodename(int codename) {
        this.codename = codename;
    }
    public void setFreshness(long freshness) {
        this.freshness = freshness;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    
}
