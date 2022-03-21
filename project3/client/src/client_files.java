import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class client_files {
    private int seqno ;
    private String filename;
    private int fd ;
    private int filecode ;
    private int curr_pos;
    private List <String> flags;
    client_files(){
        flags = new LinkedList<>();

    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public int getFd() {
        return fd;
    }
    public void setFd(int fd) {
        this.fd = fd;
    }
    public int getFilecode() {
        return filecode;
    }
    public void setFilecode(int filecode) {
        this.filecode = filecode;
    }
    public List<String> getFlags() {
        return flags;
    }
    public void setFlags(List<String> flags) {
        this.flags = new LinkedList<>();
        Iterator<String> iter  = flags.iterator();
        String flag ;
        while(iter.hasNext()) {
            flag = iter.next();
            this.flags.add(flag);
        }
       
    }
    public int getCurr_pos() {
        return curr_pos;
    }
    public void setCurr_pos(int curr_pos) {
        this.curr_pos = curr_pos;
    }
    
}