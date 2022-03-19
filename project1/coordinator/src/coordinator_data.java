import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.Iterator;

public class coordinator_data {

    private  List<coor_servers_data > servers_list = new LinkedList<>();    
    private int reqid ;

    public  List<coor_servers_data> getLserversList() {
        return servers_list;
    }

    public int getReqid() {
        return reqid;
    }

    public void setReqid(int reqid) {
        this.reqid = reqid;
    }
   
    public void add_servers_list(coor_servers_data newServer){
        this.servers_list.add(newServer);
      

    }

    public coor_servers_data ReturnMin(){
        Iterator <coor_servers_data> iter = servers_list.iterator() ;
        coor_servers_data z ;
        int min = 50;
        coor_servers_data ret_server = new coor_servers_data();

        while(iter.hasNext()){
            z = iter.next();
            if(z.getCost() < min){
                min = z.getCost();
                ret_server = z ;

            }
        }
      
        return ret_server ;
    }    
}
