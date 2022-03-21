import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;

public class Server_files {
    private String filename;


    private int code_filename;
    public int fd;
    public RandomAccessFile random_stream;
    private List<Integer> open_fd; //for fall detection --> to do list of classes with addresses to chack if client is alive and then if all client are dead close the file
    private int tMod ;
    
    Server_files(){
        fd = 0;
        open_fd = new LinkedList<Integer>();
        tMod = 0 ;

    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getCode_filename() {
        return code_filename;
    }

    public void setCode_filename(int code_filename) {
        this.code_filename = code_filename;
    }

    public int getFd() {
        return fd;
    }

    public void setFd(int fd) {
        this.fd = fd;
    }

    public List<Integer> getOpen_fd() {
        return open_fd;
    }

    public void setOpen_fd(List<Integer> open_fd) {
        this.open_fd = open_fd;
    }

    public RandomAccessFile getRandom_stream() {
        return random_stream;
    }

    public void setRandom_stream(RandomAccessFile random_stream) {
        this.random_stream = random_stream;
    }

    public int gettMod() {
        return tMod;
    }

    public void settMod(int tMod) {
        this.tMod = tMod;
    }

}