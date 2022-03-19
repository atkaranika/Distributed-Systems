
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
//import System.current.Milliseconds ;
import java.util.concurrent.TimeUnit;


public class Serverapi{

    protected static Lock lock = new Lock();
    protected static Lock lock_2 = new Lock();
   // private static DatagramPacket packet ;
    public static  MulticastSocket socket;
   // private static byte[] buf = new byte[256];
    protected static  List<request_id>  request_couples = new LinkedList<>();
    protected static  List<Integer>  services = new LinkedList<>();
    protected static  List<server_ack>  server_ack_list = new LinkedList<>();

   // public static int  reqid_api;
    public static int  reqid_server;
    public static File file;
    public static String file_path;
    private static int packet_throw;
    private static InetAddress address_coordinator;
    private static int port_coordinator ;
    public static  DatagramSocket socket_coordinator;
    public   String mac ;
    ///@Override

    public  Serverapi(String mac){
        this.mac = mac ;
    }

    public  Serverapi(){
        
    }
    public void thread_4_run(){
        
        DatagramPacket receivingPacket ;
        byte[] packet_receive = new byte[1024];
        int  reqid_api;
        byte[] req_id_api = new byte[4];
        char type_message ;
        List<String> list;
        request_id z ;
        byte[] buf = new byte[1024];
        while(true){
  
            packet_receive = new byte[1024];
            receivingPacket = new DatagramPacket(packet_receive,1024);
            
            try {
                
                socket_coordinator.receive(receivingPacket);
            } catch (IOException e) {
                    e.printStackTrace();
            }
        
            buf = new byte[1024];
            buf = receivingPacket.getData(); 
            type_message = (char)buf[0];
            req_id_api[0] = buf[1];  
            req_id_api[1] = buf[2];  
            req_id_api[2] = buf[3];  
            req_id_api[3] = buf[4];  
            reqid_api = ByteBuffer.wrap(req_id_api).getInt();
      
       
                try {
                    this.lock.lock();
                if(type_message == 'N'){
                    Iterator <request_id> iter = request_couples.iterator() ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getRequest_id_api()== reqid_api) {
                            try {
                                Files.write(Paths.get(file_path), (String.valueOf(z.getRequest_id_api())+"\n").getBytes(), StandardOpenOption.APPEND);
                            }catch (IOException e) {
                                //exception handling left as an exercise for the reader
                            }
                            iter.remove();
                        }
                    }
                }else if(type_message == 'O'){
                    System.out.println("server takes ok from cordinator for reqid api :"+ reqid_api );
                    Iterator <request_id> iter = request_couples.iterator() ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getRequest_id_api()== reqid_api) {
                            z.setOK('O');
                            break;
                        }
                    }

                }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }finally{
                
                    this.lock.unlock();
                }
            
        }

    }



    public  void thread_3_run() throws InterruptedException{
     
        DatagramPacket packet ;
       
        while(true){
        
            
            this.lock.lock();
            Iterator <request_id> iter = request_couples.iterator() ;
            request_id check_if_alive ;
            byte[] confirm = new byte[5];

            while(iter.hasNext()){
                check_if_alive = iter.next(); 
                if((System.currentTimeMillis() - check_if_alive.getTime()) > 5000 && check_if_alive.getOK() == 'O'){
                    
                    System.out.println("server check if "+ " "+ check_if_alive.getRequest_id_api()+" is alive") ;
                    byte []confirm_reqid = ByteBuffer.allocate(4).putInt(check_if_alive.getRequest_id_api()).array();
                    confirm[0] = 'C';
                    confirm[1] = confirm_reqid[0];
                    confirm[2] = confirm_reqid[1];
                    confirm[3] = confirm_reqid[2];
                    confirm[4] = confirm_reqid[3];
                    packet = new DatagramPacket(confirm, confirm.length ,  check_if_alive.getAddress(), check_if_alive.getPort());
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    check_if_alive.setSignaled(1);
                }

                    
            }
     
            this.lock.unlock();
         
            TimeUnit.SECONDS.sleep(2);
       
            this.lock.lock();

            iter = request_couples.iterator() ;
       
            while(iter.hasNext()){
                check_if_alive = iter.next();
               
                if((System.currentTimeMillis() - check_if_alive.getTime()) > 2000 && check_if_alive.getSignaled() == 1){
                    int req_id_api_remove = check_if_alive.getRequest_id_api();
                    iter.remove();
                    System.out.println("server remove from list reqid "+ check_if_alive.getRequest_id_api() + "because is not alive ");
                    this.lock_2.lock();
                    Iterator <server_ack> iter_ack = server_ack_list.iterator() ;
                    server_ack dead_ack  ;
                    while(iter_ack.hasNext()){
                        dead_ack = iter_ack.next();
                        if(dead_ack.getRequest_id_api() == req_id_api_remove ){
                            iter_ack.remove();
                          
                            break;
                        }
                    }
                    this.lock_2.unlock();
                }    
               
            }
            this.lock.unlock();

    }
}


    public  void thread_2_run() throws InterruptedException{
     
        DatagramPacket packet ;
     
        while(true){
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.lock_2.lock();
            Iterator <server_ack> iter = server_ack_list.iterator() ;
            server_ack z  ;

           
                byte[] confirm = new byte[5];
                

            while(iter.hasNext()){
                z = iter.next();
              // System.out.println("TIMES :" +System.currentTimeMillis()  + " "+ z.getTime());
                if((System.currentTimeMillis() - z.getTime()) > 2000){
                    z.setTime(System.currentTimeMillis() );
                    System.out.println("server observes that ");
                    byte []confirm_reqid = ByteBuffer.allocate(4).putInt(z.getRequest_id_api()).array();
                    confirm[0] = 'A';
                    confirm[1] = confirm_reqid[0];
                    confirm[2] = confirm_reqid[1];
                    confirm[3] = confirm_reqid[2];
                    confirm[4] = confirm_reqid[3];
                    packet = new DatagramPacket(confirm, confirm.length ,  z.getAddress(), z.getPort());
                    try {
                        socket.send(packet);
                      

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    iter.remove();
                    
                }
                    
            }
            this.lock_2.unlock();
    
    }
}
    public  void thread_1_run(){
        int  reqid_api;

        DatagramPacket receivingPacket ;
        byte[] packet_receive = new byte[1024];
        byte[] service_receive = new byte[4];
        byte[] req_id_api_confirm = new byte[4];
   
        byte[] size_of_packet = new byte[4];
        char type_message ;
        List<String> list;
        request_id z ;
        byte[] buf = new byte[256];
 
        while(true){
            //receive a packet 9 bytes 
  
            packet_receive = new byte[1024];
            receivingPacket = new DatagramPacket(packet_receive,1024);
            
            try {
               
                socket.receive(receivingPacket);
             
                
            } catch (IOException e) {
                    e.printStackTrace();
            }
        
            buf = new byte[1024];
            buf = receivingPacket.getData(); 
            type_message = (char)buf[0];
           

            try {
                    this.lock.lock();
                       

                if(type_message == 'C'){
                    req_id_api_confirm[0] = buf[1];  
                    req_id_api_confirm[1] = buf[2];  
                    req_id_api_confirm[2] = buf[3];  
                    req_id_api_confirm[3] = buf[4];  
                    reqid_api = ByteBuffer.wrap(req_id_api_confirm).getInt();
                    Iterator <request_id> iter = request_couples.iterator() ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getRequest_id_api()== reqid_api) {
                            z.setSignaled(0);
                            z.setTime(System.currentTimeMillis());
                        }
                    }
                    System.out.println("server get that " + reqid_api + " is alive ");
                    continue ;
                }else if(type_message=='A'){
                    
                    size_of_packet[0] = buf[1];
                    size_of_packet[1] = buf[2];
                    size_of_packet[2] = buf[3];
                    size_of_packet[3] = buf[4];
                    int int_size_of_packet = ByteBuffer.wrap(size_of_packet).getInt();
                    System.out.println("STO A "+int_size_of_packet);

                    for(int i = 0 ;i <int_size_of_packet; i+=4){
                        
                        req_id_api_confirm[0] = buf[i+5];  
                        req_id_api_confirm[1] = buf[i+6];  
                        req_id_api_confirm[2] = buf[i+7];  
                        req_id_api_confirm[3] = buf[i+8];  
                        reqid_api = ByteBuffer.wrap(req_id_api_confirm).getInt();
                        System.out.println("server get acknowledge for reqid " + reqid_api);
                        Iterator <request_id> iter = request_couples.iterator() ;
            
                        while(iter.hasNext()){
                            z = iter.next();
                            if(z.getRequest_id_api()== reqid_api) {
                                iter.remove();
                                break ;
                            }
                        }
            
    
                    }
              
                    continue ;
                }
                else if(type_message == 'R'){
                    //read size of data
                    size_of_packet[0] = buf[1];
                    size_of_packet[1] = buf[2];
                    size_of_packet[2] = buf[3];
                    size_of_packet[3] = buf[4];
                    int int_size_of_packet = ByteBuffer.wrap(size_of_packet).getInt();
                   
                    req_id_api_confirm[0] = buf[5];
                    req_id_api_confirm[1] = buf[6];
                    req_id_api_confirm[2] = buf[7];
                    req_id_api_confirm[3] = buf[8];
                    reqid_api = ByteBuffer.wrap(req_id_api_confirm).getInt();
                   
                    Iterator <request_id> iter = request_couples.iterator() ;
               
            
                     while(iter.hasNext()){
                        z = iter.next();
                        
                        if(z.getRequest_id_api()== reqid_api) {
                            System.out.println("server get the num of " +reqid_api);
                          
                            try {
                                list = Files.readAllLines(Paths.get(file_path));
                                
                                if (list.contains((String.valueOf(reqid_api)))){
                                 
                                    z.setTaken_over(1) ;
                                    z.setSignaled(0);
                                    z.setTime(System.currentTimeMillis());
                                    break ;
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                           
                            packet_throw++;
                            byte[] data_for_server = new byte[int_size_of_packet];
                            if(packet_throw != 8){
                                z.setSignaled(0);
                                z.setTime(System.currentTimeMillis());
                                if(z.getTaken_over() == 0 ){
                                     for(int i = 9; i < int_size_of_packet+9 ; i++){
                                         data_for_server[i-9] = buf[i];    
                                     }
                                    System.out.println(data_for_server +"     "+ int_size_of_packet);
                                    z.setData_for_server( data_for_server);
                                    int  num = ByteBuffer.wrap(data_for_server).getInt(); 
                                   
                                    break;
                                }
                            }else{
                               
                                System.out.println("server throws packet (check for missed in network)" + z.getRequest_id_api());
                                packet_throw = 0;
                            }
                        }

                    }

                }else if (type_message == 'D'){


                    int flag = 0;
                    for(int i = 0 ; i < 4; i++){
                        req_id_api_confirm[i] = buf[i+1];    
                    }
                    reqid_api = ByteBuffer.wrap(req_id_api_confirm).getInt();
                  
                    Iterator <request_id> iter = request_couples.iterator() ;
                     while(iter.hasNext()){
                        z = iter.next();
                        if(z.getRequest_id_api()== reqid_api) {
                            //redescover
                            z.setSignaled(0);
                            z.setTime(System.currentTimeMillis());
                            System.out.println("server gets rediscover for reqid   "+reqid_api );
                            flag = 1;
                            break;
                        }
                    }
                    if(flag  == 0){
                        iter = request_couples.iterator() ;
                        z = new request_id();
                        while(iter.hasNext()){
                            z = iter.next();
                            if(z.getRequest_id_api()== reqid_api) {
                                break ;
                            }
                        }
                        try {
                            list= Files.readAllLines(Paths.get(file_path));
                            if (list.contains((String.valueOf(reqid_api)))){
                            
                                z.setTaken_over(1) ;
                                continue ;
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                       
                        for(int i = 5 ; i < 9 ; i++){
                            service_receive[i-5] = buf[i];    
                        }
                        int num_of_ok =0;
                        Iterator<request_id> num_of_ok_iter=request_couples.iterator();
                        request_id curr_req ;
                        while(num_of_ok_iter.hasNext()){
                            curr_req = num_of_ok_iter.next();
                            if(curr_req.getOK() == 'O'){
                                num_of_ok++;
                            }
                        }
                        byte[] send_to_corr = new byte[9];
                        send_to_corr[0] = 'O';
                        byte[] req_api_corr = ByteBuffer.allocate(4).putInt(reqid_api).array();
                        send_to_corr[1] = req_api_corr[0];
                        send_to_corr[2] = req_api_corr[1];
                        send_to_corr[3] = req_api_corr[2];
                        send_to_corr[4] = req_api_corr[3];
                        byte[] num_of_ok_corr = ByteBuffer.allocate(4).putInt(num_of_ok).array();
                        send_to_corr[5] = num_of_ok_corr[0];
                        send_to_corr[6] = num_of_ok_corr[1];
                        send_to_corr[7] = num_of_ok_corr[2];
                        send_to_corr[8] = num_of_ok_corr[3];
                        DatagramPacket packet_send;
                        System.out.println("server asks coordinator to serve request  " + "  "+  reqid_api + "to address" + address_coordinator);
                        packet_send = new DatagramPacket(send_to_corr, send_to_corr.length , address_coordinator, port_coordinator);
                        try {
                            socket_coordinator.send(packet_send);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        int k ;
                        Iterator <Integer> iter_serv = services.iterator() ;
                        while(iter_serv.hasNext()){
                            
                            k = iter_serv.next();
                            
                            if(k == ByteBuffer.wrap(service_receive).getInt()) {
                   
                                reqid_server++;
                                request_id new_node = new request_id();
                                new_node.setRequest_id_server(reqid_server);
                                new_node.setRequest_id_api(reqid_api);
                                new_node.setSvcid(k);
                                new_node.setTaken_over(0);
                                new_node.setAddress(receivingPacket.getAddress());
                                new_node.setPort(receivingPacket.getPort());
                                new_node.setSignaled(0);
                                new_node.setTime(System.currentTimeMillis());
                                new_node.setOK('N');
                                request_couples.add(new_node);
                       
                                break ;
                            }
                        }
                    }
                }


            } catch (InterruptedException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            } 
            finally{
                this.lock.unlock();
            }
        }

    }

    public void init() throws SocketException{
        //init
       // file = new File("serverdata.txt");
        packet_throw = 0;
        String currentDir = System.getProperty("user.dir");
        file_path = currentDir+"/"+ this.mac+ ".txt";
        reqid_server = 0;
        InetSocketAddress address =  new InetSocketAddress("230.0.0.5", 2020);
        try {
            this.socket_coordinator = new DatagramSocket(null);
            address = new InetSocketAddress("230.0.0.5", 2020);
            //socket.bind(address);
    
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] hello_message = new byte[1];
        hello_message[0]= 'H';
        DatagramPacket dgram;
        
        try {
            dgram = new DatagramPacket(hello_message, hello_message.length, address);
            System.out.println("send hello to coor");
            socket_coordinator.send(dgram);
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        DatagramPacket receivingPacket ;
        byte[] hello_receive = new byte[1024];
        receivingPacket = new DatagramPacket(hello_receive,1024);
        
        try {
            System.out.println("wait to receive");
            socket_coordinator.receive(receivingPacket); 
            port_coordinator = receivingPacket.getPort();
            address_coordinator = receivingPacket.getAddress();
            System.out.println(address_coordinator + " " + port_coordinator);
        } catch (IOException e) {
                e.printStackTrace();
        }
        try {
            //multicast address
            NetworkInterface networkInterface = NetworkInterface.getByName(mac);
            
            socket = new MulticastSocket(8080);
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("224.0.0.1"), 8080);
    
          
            socket.joinGroup(addr, networkInterface );



            System.out.println("prin"+ socket);
           // (new Serverapi()).start();
    
            Thread thread_1 = new Thread(new Runnable(){public void run() {new Serverapi().thread_1_run();}});
            thread_1.start();

            Thread thread_2 = new Thread(new Runnable(){public void run() {try {
                new Serverapi().thread_2_run();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }}});
        
            thread_2.start();
    
            Thread thread_3 = new Thread(new Runnable(){public void run() {try {
                    new Serverapi().thread_3_run();
            } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                e.printStackTrace();
            }}});
        
            //thread_3.start();
           
            
               
            Thread thread_4 = new Thread(new Runnable(){public void run() {new Serverapi().thread_4_run();}});
            thread_4.start();
            file = new File(file_path);

            if (file.createNewFile()) {
                
                System.out.println("File has been created.");
            } else {
            
                System.out.println("File already exists." );
            }
 
    
        } catch (Exception e) {
            e.printStackTrace();
        }
       
    
    }
    
    public  int register (int svcid){
        
        int new_service = svcid;
        services.add(new_service);
        return 0;
    }

    public int getRequest (int svcid, serverdata buffer ) throws InterruptedException, IOException{
        int  reqid_api;
     //   Thread.currentThread().setPriority(5);
        DatagramPacket packet_send ;
        List<String> list= Files.readAllLines(Paths.get(file_path));
        request_id z  = new request_id();
        request_id dead = new request_id();
        request_id ckeck_id_alive = new request_id();
        Iterator <request_id> iter_ckeck ;
        int found_req =0;
        Iterator <request_id> iter ;
        int einai_null = 0;
        while(true){
            found_req =0;
            this.lock.lock();
            try {
                iter = request_couples.iterator() ;
                while(iter.hasNext()){ 
                    z = iter.next();
                    if((z.getSvcid()== svcid) && (z.getTaken_over() == 0) && (z.getOK() == 'O')) {
                        if (list.contains(String.valueOf(z.getRequest_id_api()))){
                            System.out.println("iparxi sto arxio to "+ z.getRequest_id_api());
                            continue ;
                        }
                        ckeck_id_alive = z;
                        found_req =1;
                        break;
                    }
                }
            }
            finally{
                    this.lock.unlock();
            }
            if(found_req == 1){
               // System.out.println("get req send confirmation "+z.getRequest_id_api()); 
                byte []confirm_reqid = ByteBuffer.allocate(4).putInt(z.getRequest_id_api()).array();
                byte[] confirm = new byte[5];
                confirm[0] = 'D';
                confirm[1] = confirm_reqid[0];
                confirm[2] = confirm_reqid[1];
                confirm[3] = confirm_reqid[2];
                confirm[4] = confirm_reqid[3];
                packet_send = new DatagramPacket(confirm, confirm.length ,  z.getAddress(), z.getPort());
                try {
                    System.out.println("D");
                    socket.send(packet_send);
                } catch (IOException e) {
                    e.printStackTrace();
                }
             
               // System.out.println("!@@@!@!@ prin kollhsv  +  "+z.getRequest_id_api());
                while(true){
                    
                 //wait(1);
                    this.lock.lock();
                    try{ 
                        iter_ckeck  = request_couples.iterator() ;
                        while(iter_ckeck.hasNext()){
                            dead = iter_ckeck.next();
                            if(ckeck_id_alive.getRequest_id_api() == dead.getRequest_id_api()){
                                if(z.getData_for_server() != null){
                           
                                    if(z.getTaken_over() ==0){
                                        System.out.println("GET REQUEST: SERVER RECEIVES "+ z.getRequest_id_api() + "   "+ ByteBuffer.wrap(z.getData_for_server()).getInt());
                                        z.setTaken_over(1); 
                                        try {
                                            Files.write(Paths.get(file_path), (String.valueOf(z.getRequest_id_api())+"\n").getBytes(), StandardOpenOption.APPEND);
                                        }catch (IOException e) {
                                            //exception handling left as an exercise for the reader
                                        }
                                                                        ////////ACKKKKKKKKK
        
                                        buffer.setData(z.getData_for_server());
                                        server_ack ack = new server_ack();
                                        ack.setRequest_id_api(z.getRequest_id_api());
                                        ack.setTime(System.currentTimeMillis());
                                        ack.setAddress(z.getAddress());
                                        ack.setPort(z.getPort());
        
                                        this.lock_2.lock();
                                        server_ack_list.add(ack);
                                        this.lock_2.unlock();
                                    
        
                                        return (z.getRequest_id_server());
                                    }
                                    //break ;
                                }else{
                                    einai_null = 1;
                                }
                               
                            }
                            
                        }
                      
                        if(einai_null ==1){
                            einai_null = 1;
                            continue;
                        }else{
                            System.out.println("den einai sth lista ");
                            found_req =0;
                            break; 
                        }  
            
                    }finally{

                        this.lock.unlock();
                    

                    }
                }   
            }
        
         }
    
       }
    
    public  void sendReply (int reqid, byte[] buffer, int len) {
         DatagramPacket packet ;
       // System.out.println("SEND REPLY PRIORITY: "+Thread.currentThread().getPriority());
        int ret_reqid = 0;
        byte []reply = new byte[len+ 9];
        byte [] reqid_buffer ;
        byte[] size_of_data;
        InetAddress address ;
        int port ;
        

        
        request_id z ;
       // System.out.println("request couples from send reply1");
        try {
             try {
                this.lock.lock();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

          // System.out.println("MAIN SENDREPLY LOCK");
          // System.out.println("request couples from send reply");
         
            Iterator <request_id> iter = request_couples.iterator() ;
            while(iter.hasNext()){
                z = iter.next();
                if(z.getRequest_id_server()== reqid) {
                System.out.println("found "+ reqid +" "+ ByteBuffer.wrap(z.getData_for_server()).getInt());
                    
                    ret_reqid = z.getRequest_id_api();
                    address = z.getAddress() ;
                    port = z.getPort();
                   // System.out.println("RETURN REQID "+" "+ret_reqid);
                    reply[0] = 'R';
                    
                    size_of_data = ByteBuffer.allocate(4).putInt(len).array();
                    for(int i = 1 ; i < 5 ; i++){
                        reply[i] = size_of_data[i-1];
                    }
                    reqid_buffer = ByteBuffer.allocate(4).putInt(ret_reqid).array();
                    for(int i = 5 ; i < 9 ; i++){
                        reply[i] = reqid_buffer[i-5];
                    }
                   
                    for(int i = 9 ; i < len-1+9 ; i++){
                        reply[i] = buffer[i-9];
                    }

                    packet = new DatagramPacket(reply, reply.length, address, port);
                    try {
                        System.out.println("SEND REPLY: send "+ " "+ ByteBuffer.wrap(z.getData_for_server()).getInt());
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        this.lock_2.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <server_ack> iter_ack = server_ack_list.iterator() ;
                    server_ack z_ack  ;
        
                   
                    byte[] confirm = new byte[5];
                        
        
                    while(iter_ack.hasNext()){
                        z_ack = iter_ack.next();
                    
                        if(z_ack.getRequest_id_api() == z.getRequest_id_api()){
                            
                            server_ack_list.remove(z_ack);
                            break;
                            
                        }
                            
                    }
                    this.lock_2.unlock();
               
                    break ;

                }

            }
          
        }finally{
            this.lock.unlock();

        }
    }
    public  int unregister (int svcid){
        services.remove(svcid);
        return 0;
    }
   
}