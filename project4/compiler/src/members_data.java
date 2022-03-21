import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;
public class members_data implements Serializable  {
    private long thread_id;
    private int thread_code;
    private  InetAddress ip;
    private InetAddress start_ip;
    private int port; 
    private int start_port;
    private int migrate;
    private String programm_running;
    private List<variable> vars;
    private List<messages> messages;
    private List<labels> labels_list;
    private int seqno ;
    private int error_line ;
    private long position ;
    private String address ; 
    private int header;
    private int team_code ;

    
    public int getTeam_code() {
        return team_code;
    }
    public void setTeam_code(int team_code) {
        this.team_code = team_code;
    }
 

    
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public int getError_line() {
        return error_line;
    }
    public void setError_line(int error_line) {
        this.error_line = error_line;
    }
    public long getPosition() {
        return position;
    }
    public void setPosition(long position) {
        this.position = position;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }


    members_data(){
        this.messages= new LinkedList<>();
        this.ip = null;
        this.port  = -1;
        this.vars = new LinkedList<>();
        this.labels_list = new LinkedList<>();
    }
    public long getThread_id() {
        return thread_id;
    }
    public void setThread_id(long thread_id) {
        this.thread_id = thread_id;
    }
    public int getThread_code() {
        return thread_code;
    }
    public void setThread_code(int thread_code) {
        this.thread_code = thread_code;
    }
    public InetAddress getIp() {
        return ip;
    }
    public void setIp(InetAddress ip) {
        this.ip = ip;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public int getMigrate() {
        return migrate;
    }
    public void setMigrate(int migrate) {
        this.migrate = migrate;
    }
    public String getProgramm_running() {
        return programm_running;
    }
    public void setProgramm_running(String programm_running) {
        this.programm_running = programm_running;
    }
    public List<messages> getMessages() {
        return messages;
    }
    public void setMessages(List<messages> messages) {
        this.messages = messages;
    }
    public InetAddress getStart_ip() {
        return start_ip;
    }
    public void setStart_ip(InetAddress start_ip) {
        this.start_ip = start_ip;
    }
    public List<variable> getVars() {
        return vars;
    }
    public void setVars(List<variable> vars) {
        Iterator<variable> iter= vars.iterator();
        variable curr_var;
        while(iter.hasNext()){
            curr_var = iter.next();
            System.out.println("vaaar " +curr_var.getName()+" "+curr_var.getValue_string());

        
            variable new_var = new variable(curr_var.getValue_string(), curr_var.getName());
            this.vars.add(new_var);
        }
    
    }
    public void removeVars(){
        Iterator<variable> iter=  this.vars.iterator();
        while(iter.hasNext()){
            iter.remove();
        }
    }
    public int getStart_port() {
        return start_port;
    }
    public void setStart_port(int start_port) {
        this.start_port = start_port;
    }
    public List<labels> getLabels() {
        return labels_list;
    }
    public void setLabels(List<labels> labels_list) {
        Iterator<labels> iter= labels_list.iterator();
        labels curr_var;
        while(iter.hasNext()){
            curr_var = iter.next();

        
            labels new_var = new labels(curr_var.getName(), curr_var.getTo_seek(), curr_var.getLine());
            this.labels_list.add(new_var);
        }
    }
    public List<labels> getLabels_list() {
        return labels_list;
    }
    public void setLabels_list(List<labels> labels_list) {
        this.labels_list = labels_list;
    }
    public int getHeader() {
        return header;
    }
    public void setHeader(int header) {
        this.header = header;
    }
    
    
}