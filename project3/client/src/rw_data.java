import java.nio.ByteBuffer;

public class rw_data {
    private int size;
    private ByteBuffer data;
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public ByteBuffer getData() {
        return data;
    }
    public void setData(ByteBuffer data) {
        this.data = data;
    }
    

}