public class write_data {
    public int seqno;
    public int size_written;
    public int codename;
    public int messages ;
  
    public  write_data(int blocks){
        messages = 0 ;
   
    }
    
    public write_data() {
        this.size_written = -10;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public int getSize_written() {
        return size_written;
    }
    public void setSize_written(int size_written) {
        this.size_written = size_written;
    }
    public int getCodename() {
        return codename;
    }
    public void setCodename(int codename) {
        this.codename = codename;
    }

    
}