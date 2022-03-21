import java.io.Serializable;
public class labels implements Serializable{
    private String name;
    private long to_seek;
    private int line;
    labels(){
        
    }
    labels(String name, long to_seek, int line){
        this.name = name;
        this.to_seek = to_seek;
        this.line = line;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getTo_seek() {
        return to_seek;
    }
    public void setTo_seek(long to_seek) {
        this.to_seek = to_seek;
    }
    public int getLine() {
        return line;
    }
    public void setLine(int line) {
        this.line = line;
    }

    
    
}
