
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//import System.current.Milliseconds ;


public class coordinator {

    public static Lock lock = new Lock();
    public static Lock lock_2 = new Lock();
    private static List<coordinator_data> list = new LinkedList<>();
    public static File file;
    public static String file_path;
    private static  List<coor_servers_data> availableServers = new LinkedList<>(); 

    public static  MulticastSocket socket;
    public static int serv_counter = 0 ;
    //socket = new DatagramSocket(2020,InetAddress.getLocalHost());

    public  void receive_from_servers(){
        byte []buf;
        byte[] size_of_packet = new byte[4];
        char type_message ;

        while(true){
            
            byte[] packet_receive = new byte[1024];
            byte[] reqid_api = new byte[4];
            int int_reqid_api;
            byte[] cost = new byte[4];
            int int_cost ;
            DatagramPacket receivingPacket = new DatagramPacket(packet_receive,1024);

            try {
                socket.receive(receivingPacket);
                
            } catch (IOException e) {
                    e.printStackTrace();
            }

            buf = new byte[1024];
            buf = receivingPacket.getData(); 

            type_message = (char)buf[0];
            coor_servers_data newServer = new coor_servers_data();
            newServer.setAddress(receivingPacket.getAddress());
            newServer.setPort(receivingPacket.getPort());
            if(type_message == 'H'){
                    System.out.println("Hello");
                try {
                    this.lock_2.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Iterator <coor_servers_data> iterServers= availableServers.iterator() ;
                coor_servers_data zs ; 
                int foundServer = 0 ;

                while(iterServers.hasNext()){
                    zs = iterServers.next();
                    if(zs.getAddress().equals(newServer.getAddress()) && zs.getPort() == newServer.getPort()){

                        foundServer = 1 ;
                        break ;
                    }

                }
                if (foundServer == 0){
                    newServer.setCounter(0);
                    availableServers.add(newServer);
                    
                }

                byte[] hello_message = new byte[1];
                hello_message[0]= 'H';
                DatagramPacket dgram;
                
                try {
                    dgram = new DatagramPacket(hello_message, hello_message.length, newServer.getAddress(), newServer.getPort());
                    System.out.println("send hello to server");
                    socket.send(dgram);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                this.lock_2.unlock();
                continue;
            }
            reqid_api[0] = buf[1];  
            reqid_api[1] = buf[2];  
            reqid_api[2] = buf[3];  
            reqid_api[3] = buf[4];  

            cost[0] = buf[5];  
            cost[1] = buf[6];  
            cost[2] = buf[7];  
            cost[3] = buf[8];  
            int_reqid_api = ByteBuffer.wrap(reqid_api).getInt();
            int_cost = ByteBuffer.wrap(cost).getInt();

            coordinator_data z ;
            
            if(type_message == 'O'){
                
                try {
                    this.lock.lock();

             
                int found = 0 ;
                Iterator <coordinator_data> iter = list.iterator() ;
                //= new List<String>;
               
                newServer.setCost(int_cost);

                while(iter.hasNext()){
                    z = iter.next();
                    if(z.getReqid()== int_reqid_api) {
                        z.add_servers_list(newServer);
                        found = 1 ;
                        break;
                    }
                
                }
                if(found == 0 ){
                    try {
                        List<String> list_file = Files.readAllLines(Paths.get(file_path));
                        if (list_file.contains(String.valueOf(int_reqid_api))){
                            byte [] sendOk = new byte[5];
                            sendOk[0] = 'N';
                            byte [] sendReqid = ByteBuffer.allocate(4).putInt(int_reqid_api).array();
                            sendOk[1] = sendReqid[0];
                            sendOk[2] = sendReqid[1];
                            sendOk[3] = sendReqid[2];
                            sendOk[4] = sendReqid[3];
                            System.out.println("SEND NOT OK THREAD REQID API "+" "+int_reqid_api+ "SERVER ADDRESS:" + " "+ newServer.getAddress() + "COST :"+ " "+newServer.getCost()) ;
            
                            DatagramPacket packet_send = new DatagramPacket(sendOk, sendOk.length ,  newServer.getAddress(), newServer.getPort());
                            try {
                                socket.send(packet_send);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        
                            continue;
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
    
                    
                    coordinator_data newRequest = new coordinator_data(); 
                    newRequest.setReqid(int_reqid_api);

                    newRequest.add_servers_list(newServer);
                    list.add(newRequest);
                }

            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            finally{
                this.lock.unlock();
            }
                
            }
        }
    }


    public void sendOk(){

        
        coordinator_data z;
        coor_servers_data server ;
        DatagramPacket packet_send ;
        int counter = 0 ;
        int serversAvail = 0;

        while(true){
            try {
                this.lock.lock();
    
                if(list.size() >0){
             
                    counter++;
                                // Iterator <coordinator_data> iter = list.iterator() ;
                     Iterator <coordinator_data> iter_list = list.iterator() ;
              

                    if (iter_list.hasNext()){
                        z = iter_list.next();
                        
                        server = z.ReturnMin();
                      
                        serversAvail = z.getLserversList().size();
                        if(serversAvail  < (2/3) * availableServers.size() && counter < (2/3) * availableServers.size() ){
                            continue;
                        }
                        counter = 0 ;
                        byte []sendReqid = ByteBuffer.allocate(4).putInt(z.getReqid()).array();
                        byte[] sendOk= new byte[5];
                        sendOk[0] = 'O';
        
                        sendOk[1] = sendReqid[0];
                        sendOk[2] = sendReqid[1];
                        sendOk[3] = sendReqid[2];
                        sendOk[4] = sendReqid[3];
        
                        packet_send = new DatagramPacket(sendOk, sendOk.length ,  server.getAddress(), server.getPort());
                        
                        System.out.println("SEND OK THREAD REQID API "+" "+z.getReqid()+ "SERVER ADDRESS:" + " "+ server.getAddress() + "COST :"+ " "+ server.getCost()) ;
                        
                       

                        
                        try {
                            socket.send(packet_send);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                       
                        Iterator <coor_servers_data> iterForServers = z.getLserversList().iterator() ;
                        coor_servers_data zs;
                        while(iterForServers.hasNext()){
                            zs = iterForServers.next();
                            if(zs.getAddress().equals(server.getAddress()) && zs.getPort() == server.getPort()){
                                
                                Iterator <coor_servers_data> inservers = availableServers.iterator() ;
                                coor_servers_data inz;
                                this.lock_2.lock();
                                while(inservers.hasNext()){
                                    inz = inservers.next();
                                    if(inz.getAddress().equals(server.getAddress()) && inz.getPort() == server.getPort()){
                                        serv_counter++;
                                        inz.setCounter(inz.getCounter()+1);
                                        System.out.println("server " + " " + server.getAddress() +" has  "+ inz.getCounter() +" requests all: " + serv_counter  );
                                        break ;
                                    }
                                }
                                this.lock_2.unlock();
                                iterForServers.remove();
                                continue ;
                                
                            }
                            if(!zs.getAddress().equals(server.getAddress()) || zs.getPort() != server.getPort()){
                                
                                sendOk[0] = 'N';
                               
                                System.out.println("SEND NOT OK THREAD REQID API "+" "+z.getReqid()+ "SERVER ADDRESS:" + " "+ zs.getAddress() + "COST :"+ " "+ zs.getCost()) ;
                                
                                packet_send = new DatagramPacket(sendOk, sendOk.length ,  zs.getAddress(), zs.getPort());
                                try {
                                    socket.send(packet_send);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(zs.getAddress()+"  "+z.getReqid());
                                iterForServers.remove();
                            }
        
                        }
                        try {
                            Files.write(Paths.get(file_path), (String.valueOf(z.getReqid())+"\n").getBytes(), StandardOpenOption.APPEND);
                        }catch (IOException e) {
                            System.out.println("ERRRRRRRRROOOOOOOOOOOORRRRR");
                        }
                        System.out.println("-------------------------------");
                        iter_list.remove();

                    }
                //z = list.get(0);
               
                

            }
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }finally{
    
            this.lock.unlock();
        }
        }
    }
    
public static void init(){
    String currentDir = System.getProperty("user.dir");
    file_path = currentDir+"/coor_data.txt";
    file = new File(file_path);

    try {
        if (file.createNewFile()) {
                
            System.out.println("File has been created.");
            } 
        else {
            
            System.out.println("File already exists." );
        }
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    try {
        //multicast address
       
        NetworkInterface networkInterface = NetworkInterface.getByName("00:00:00:00:00:01");
         socket = new MulticastSocket(2020);
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("230.0.0.5"), 2020);

        InetAddress group = InetAddress.getByName("230.0.0.5");
        socket.joinGroup(addr,  networkInterface);

         System.out.println("prin"+ socket);
      


     }catch (Exception e) {
         e.printStackTrace();
     }
}
    public static void main(String[] args) {
       System.out.println("hello");

         init();

        Thread thread_1 = new Thread(new Runnable(){public void run() {new coordinator().receive_from_servers();}});
        thread_1.start();
        Thread thread_2 = new Thread(new Runnable(){public void run() {new coordinator().sendOk();}});
        thread_2.start();
        

        
    }
    
}