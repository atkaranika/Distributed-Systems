import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import java.util.Set;
import java.io.File;
import java.io.Serializable;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;




public class Main  implements Serializable {
    public static Lock teams_lock =  new Lock();

    public static Lock code_lock =  new Lock();
    public static Lock request_lock = new Lock();
    public static List<teams_data> teams ;
    public static List <copy_file>migrated_info ;
    public static List<request> reqs ; 
    public  static InetAddress main_ip;
    public static int main_port;

    public static int code_init;
    public static  DatagramSocket socket_coordinator;
    public static  DatagramSocket socket_thread;


    public static ObjectOutputStream oo ;
    public static ObjectInputStream iStream ;
    public static DatagramSocket socket_object;

    public thread_data data_for_thread;
    public static int seqno;
    Main(int team_code , int thread_code, String run,int err_line,long position){
        data_for_thread = new thread_data();
        data_for_thread.setTeam_code(team_code);
        data_for_thread.setRun_command(run);
        data_for_thread.setThread_code(thread_code);
        data_for_thread.setErr_line(err_line);
        data_for_thread.setPosition(position);
    }
    Main(){
        
    }



    public void read_for_object(){
        DatagramPacket receivingPacket ;
        byte[] packet_receive = new byte[1024];
        byte[] buf = new byte[1024];
  
       
        InputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        
        System.out.println("before receive something ");
        while (true) {
         
            receivingPacket = new DatagramPacket(packet_receive,1024);
            try {
                socket_object.receive(receivingPacket);
            } catch (IOException e) {
                    e.printStackTrace();
            }
    
            buf = receivingPacket.getData(); 
            teams_data new_team_rec = new teams_data();
            ByteArrayInputStream list_array = new ByteArrayInputStream(buf);
            try {
                ObjectInputStream o = new ObjectInputStream(list_array);
              
                try {
                    new_team_rec = (teams_data)o.readObject();
                  
                   
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            int seqno_loc = 0;
            Iterator<members_data> iter_m = new_team_rec.getMembers().iterator();
            members_data m;
            int migrate_code = 0;
        
            while(iter_m.hasNext()){
                m = iter_m.next();
             //   System.out.println("!!!!!!! "+m.getThread_code()+" "+new_team_rec.getTeam_code()+" "+m.getMigrate()+" "+m.getSeqno());
                if(m.getMigrate() == 1){
                    migrate_code = m.getThread_code();
                    seqno_loc = m.getSeqno();
                    m.setMigrate(0);
                    m.setIp(main_ip);
                    m.setPort(main_port);

                    System.out.println(m.getPort() +"    " + m.getIp());
                }else{
                    m.setMigrate(1);
                }

            }
           
            
  
            System.out.println("After migrate  --> seqno : "+seqno_loc);
 

            
       

            String run = "";
           
            try{
                teams_lock.lock();
            }catch( InterruptedException e){

            }
            Iterator<teams_data> iter_tm = teams.iterator();
            teams_data tm = new teams_data();
            int found_team =0;
            while(iter_tm.hasNext()){
                tm = iter_tm.next();
                if(tm.getTeam_code() == new_team_rec.getTeam_code()){
                    found_team =1;
                    break;
                }
            }
            if(found_team == 1){
                //change the data only for the migrated
                Iterator<members_data> iter_mb = tm.getMembers().iterator();
                members_data mb;
                while(iter_mb.hasNext()){
                    mb = iter_mb.next();
                    if(mb.getThread_code() == migrate_code){
                        mb.setMigrate(0);
                        mb.setIp(main_ip);
                        mb.setPort(main_port);
                    }
                }
            }else{
                teams.add(new_team_rec);
            }
           
            teams_lock.unlock();
            iter_m = new_team_rec.getMembers().iterator();
    
            int thread_code =0;
            String run_ = "";
            int err_line = 0;
            long pos =0;
            
            while(iter_m.hasNext()){
                m = iter_m.next();
                if(m.getMigrate() == 0){
                    thread_code = m.getThread_code();
                   // run_ = m.getProgramm_running();
                    err_line = m.getError_line();
                    pos = m.getPosition();
                }
            }
            int thcm = thread_code;
            int tcm =new_team_rec.getTeam_code();
            String run_final = run_;
            int err_line_final = err_line;
            long pos_final = pos; 
            Thread new_thred = new Thread(new Runnable(){public void run() {new Main(tcm,thcm,run_final,err_line_final,pos_final).compilation_run();}});
            new_thred.start();
            try{
                teams_lock.lock();
            }catch( InterruptedException e){

            }
            Iterator<teams_data> iter_t = teams.iterator();
            teams_data t;
            // teams_data t;
           
            
            while(iter_t.hasNext()){
              t= iter_t.next();
              if(t.getTeam_code() ==tcm){
                    iter_m = t.getMembers().iterator();
                  
                    while(iter_m.hasNext()){
                        m = iter_m.next();
                        if(m.getThread_code() == thcm){
                            m.setThread_id(new_thred.getId());
                        }
                    }
              }
            }
            teams_lock.unlock();
              byte[] send_to_mig = new byte[5];
              //type F
              send_to_mig[0] = 'm';
              ByteBuffer migrate_nums= ByteBuffer.allocate(4);
              migrate_nums.putInt( seqno_loc);
              migrate_nums.rewind();
              byte [] migrate_nums_byte = new byte[migrate_nums.remaining()];
              migrate_nums.position(0);
              migrate_nums.get(migrate_nums_byte);
  
              System.arraycopy(migrate_nums_byte, 0, send_to_mig , 1, migrate_nums_byte.length);
          
              InetSocketAddress address_migrate =  new InetSocketAddress(receivingPacket.getAddress(), receivingPacket.getPort());
              DatagramPacket packet_send = new DatagramPacket(send_to_mig, send_to_mig.length , address_migrate);
      
              try {
                  socket_thread.send(packet_send);
              } catch (IOException e) {
                  e.printStackTrace();
              }
              
           
            
        }

    }
    public void read_from_socket() throws FileNotFoundException{
        DatagramPacket receivingPacket ;
        byte[] packet_receive = new byte[1024];
        byte[] buf = new byte[1024];
  
        char type_message ;
        

        while(true){
            System.out.println("before receive something ");
            packet_receive = new byte[1024];
            receivingPacket = new DatagramPacket(packet_receive,1024);
            
            try {
              //  System.out.println("before receive");

                socket_thread.receive(receivingPacket);
                System.out.println("just receive");
            } catch (IOException e) {
                    e.printStackTrace();
            }
       
            buf = new byte[1024];
            buf = receivingPacket.getData(); 
            String data = new String(buf,StandardCharsets.UTF_8);
            String type = data.substring(0,1);

            if(type.equals("F")){

                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,9)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                int file_length = wrapped.getInt();

                System.out.println("type F seqno is " +seqno_loc);
                String filename  = new String(Arrays.copyOfRange(buf, 9, 9+file_length), StandardCharsets.UTF_8);
                //check if file exist
                
                File f = new File(filename);
                String final_filename ="";
                if(f.exists()) {
                    try {
                        String suffixes = filename.substring(filename.lastIndexOf("."), filename.length());
                        String address = receivingPacket.getAddress().toString();
                        String new_filename = String.valueOf(seqno_loc)+address.substring(1, address.length())+suffixes;
                        System.out.println("--->create file from migration " + new_filename);
                        File new_file = new File(new_filename);
                        new_file.createNewFile();
                       
                         final_filename = new String(new_filename);
                    } catch (Exception e) {
                         e.printStackTrace();
                    }        
                }else{
                     final_filename = new String(filename);
                }
                
                DatagramPacket packet_send;
                byte[] send_to_mig = new byte[9+ final_filename.length()];
                //type F
                send_to_mig[0] = 'f';
                ByteBuffer migrate_nums = ByteBuffer.allocate(8);
                migrate_nums.putInt(seqno_loc);
                migrate_nums.putInt(final_filename.length());
                migrate_nums.rewind();
                byte [] migrate_nums_byte = new byte[migrate_nums.remaining()];
                migrate_nums.position(0);
                migrate_nums.get(migrate_nums_byte);


                byte[] filename_array = final_filename.getBytes();
                System.arraycopy(migrate_nums_byte, 0, send_to_mig , 1, migrate_nums_byte.length);
                System.arraycopy(filename_array, 0, send_to_mig , migrate_nums_byte.length+1, filename_array.length);
              
                InetSocketAddress address_migrate =  new InetSocketAddress(receivingPacket.getAddress(), receivingPacket.getPort());
                packet_send = new DatagramPacket(send_to_mig, send_to_mig.length , address_migrate);
        
                try {
                    socket_thread.send(packet_send);
                } catch (IOException e) {
                    e.printStackTrace();
                }



            }else if (type.equals("f")){
               
                //ACK FOR F request
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,9)); // big-endian by default
    
                wrapped.rewind();
                int seqno_loc= wrapped.getInt();
                int file_length = wrapped.getInt();
                System.out.println("Receive ack for seqno request --->" + seqno_loc);
                String filename  = new String(Arrays.copyOfRange(buf, 9, 9+file_length), StandardCharsets.UTF_8);
                
                try {
                    request_lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Iterator<request> iter_r = reqs.iterator();
                request curr_req;
                while(iter_r.hasNext()){
                    curr_req = iter_r.next();
                    if(curr_req.getReqid() == seqno_loc){
                        curr_req.setNewFileName(filename);
                        System.out.println("taken fname : "+ filename);
                        curr_req.setAcked(1);
                        
                    }

                }
                request_lock.unlock();
           
           
           
            }else if(type.equals("C")){
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,14)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                int file_length = wrapped.getInt();
                int bytes = wrapped.getInt();
                byte flag = wrapped.get();

                System.out.println("type C seqno is " +seqno_loc);
                System.out.println( "size to read is " + bytes) ;
                String filename  = new String(Arrays.copyOfRange(buf, 14, 14+file_length), StandardCharsets.UTF_8);
                RandomAccessFile random_stream;

                Iterator <copy_file> iter = migrated_info.iterator();
                copy_file info ;
                int found = 0 ;

                int counter = 0 ;
                while(iter.hasNext()){
                    info = iter.next();
                    counter++;
                    if(info.filename.equals(filename)){
                        
                        found = 1;
                    }

                }
                if (found == 0){
                    random_stream = new RandomAccessFile(filename, "rw");
                    copy_file new_copy_file = new copy_file();
                    new_copy_file.fd = random_stream ;
                    new_copy_file.filename = filename ;
                    new_copy_file.seqno = seqno_loc ;
                   
                    byte [] bytes_to_write = Arrays.copyOfRange(buf, 14+file_length,14+file_length+bytes);
                    
                    System.out.println("receive part of file " + filename + " info  " + new String(bytes_to_write)) ;
                    try {
                        new_copy_file.fd.write(bytes_to_write);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    migrated_info.add(new_copy_file);
                }
                else {
                    if(migrated_info.get(counter).seqno < seqno_loc){
                        migrated_info.get(counter).seqno  = seqno_loc ;
                        byte [] bytes_to_write = Arrays.copyOfRange(buf, 14+file_length+bytes,14);
                        try {
                            migrated_info.get(counter).fd.write(bytes_to_write);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }

                DatagramPacket packet_send;
                byte[] send_to_mig = new byte[9];
                //type F
                send_to_mig[0] = 'c';
                ByteBuffer migrate_nums = ByteBuffer.allocate(8);
                migrate_nums.putInt(seqno_loc);
                migrate_nums.putInt(socket_object.getLocalPort());
             
                migrate_nums.rewind();
                byte [] migrate_nums_byte = new byte[migrate_nums.remaining()];
                migrate_nums.position(0);
                migrate_nums.get(migrate_nums_byte);


                System.arraycopy(migrate_nums_byte, 0, send_to_mig , 1, migrate_nums_byte.length);

              
                InetSocketAddress address_migrate =  new InetSocketAddress(receivingPacket.getAddress(), receivingPacket.getPort());
                packet_send = new DatagramPacket(send_to_mig, send_to_mig.length , address_migrate);
        
                try {
                    socket_thread.send(packet_send);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                
            }else if(type.equals("c")){ 
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,9)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                int port = wrapped.getInt();
                Iterator <request>iter = reqs.iterator() ; 
                System.out.println("Receive ack for seqno request --->" + seqno_loc);
                request req ;
                while(iter.hasNext()){
                    req = iter.next();
                    if(req.getReqid() == seqno_loc){
                        req.setAcked(1);
                        req.setPort(port);
                    }
                }

            
            
            }else if (type.equals("m")){
                
                //ACK FOR F request
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,5)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                
                System.out.println("Receive ack for seqno request --->" + seqno_loc);
                try {
                    request_lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Iterator<request> iter_r = reqs.iterator();
                request curr_req;
                while(iter_r.hasNext()){
                    curr_req = iter_r.next();
                    if(curr_req.getReqid() == seqno_loc){
                        curr_req.setAcked(1);
                    }

                }
                request_lock.unlock();
           
           
           
            }else if(type.equals("P")){

                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,9)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                int print_length = wrapped.getInt();

                System.out.println("type P seqno is " +seqno_loc);
                String prn_message  = new String(Arrays.copyOfRange(buf, 9, 9+print_length), StandardCharsets.UTF_8);
                //check if file exist
                

                System.out.println(prn_message);



            }else if(type.equals("S")){
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,21)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                int sending_team = wrapped.getInt();
                int sending_thread = wrapped.getInt();
                int to_th = wrapped.getInt();
                int msg_length = wrapped.getInt();
                String msg  = new String(Arrays.copyOfRange(buf, 21, 21+msg_length), StandardCharsets.UTF_8);

                //search the thread and send
                try {
                    teams_lock.lock();
                } catch (InterruptedException e3) {
                    // TODO Auto-generated catch block
                    e3.printStackTrace();
                }
               Iterator<teams_data> iter_team = teams.iterator();
               teams_data curr_team;
                while(iter_team.hasNext()){
                    curr_team = iter_team.next();
                    if(curr_team.getTeam_code() == sending_team){
                        Iterator<members_data> iter_member = curr_team.getMembers().iterator();
                        members_data curr_member ;
                        while(iter_member.hasNext()){
                            curr_member = iter_member.next();
                            if(curr_member.getThread_code() == to_th){
                             //   System.out.println("main ip "  +main_ip + "main port "  + main_port + " to ip " + curr_member.getIp() + " to port "+ curr_member.getPort());
                                
                                messages new_mess = new messages();
                                System.out.println("MESSAGE IS : "+ msg);
                                new_mess.setMessage(msg);
                                new_mess.setThread_from(sending_thread);
                                new_mess.setIp_from(receivingPacket.getAddress());
                                new_mess.setPort_from(receivingPacket.getPort());
                                new_mess.setSeqno(seqno_loc);
                                curr_member.getMessages().add(new_mess);

                                break;
                

                            }
                       
                        }
                        break;
                    }
                }
                teams_lock.unlock();

             
            }else if(type.equals("R")){
                 
            
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,5)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                
                System.out.println("Receive ack for seqno request --->" + seqno_loc);
                try {
                    request_lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Iterator<request> iter_r = reqs.iterator();
                request curr_req;
                while(iter_r.hasNext()){
                    curr_req = iter_r.next();
                    if(curr_req.getReqid() == seqno_loc){
                        curr_req.setAcked(1);
                    }

                }
                request_lock.unlock();
           
           
            }else if(type.equals("N")){
System.out.println("NNNN message ");
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,21)); // big-endian by default
            
                wrapped.rewind();
                int seqno_loc = wrapped.getInt();
                int team_n = wrapped.getInt();
                int thread_n = wrapped.getInt();
                int port_n = wrapped.getInt();
                int ip_size = wrapped.getInt();
                String ip_n_str  = new String(Arrays.copyOfRange(buf, 21, 21+ip_size), StandardCharsets.UTF_8);
                
                //search the thread and send
                try {
                    teams_lock.lock();
                } catch (InterruptedException e3) {
                    // TODO Auto-generated catch block
                    e3.printStackTrace();
                }
               Iterator<teams_data> iter_team = teams.iterator();
               teams_data curr_team;
                while(iter_team.hasNext()){
                    curr_team = iter_team.next();
                    if(curr_team.getTeam_code() == team_n){
                        if(curr_team.getMembers().get(0).getStart_ip() == main_ip && curr_team.getMembers().get(0).getStart_port()== main_port){
//if this is the starting programm
                            byte[] send_to_prn = new byte[21+ ip_size];
                            send_to_prn[0] = 'N';
                            ByteBuffer prn_nums = ByteBuffer.allocate(20);
                            prn_nums.putInt(seqno_loc);
                            prn_nums.putInt(team_n);
                            prn_nums.putInt(thread_n);

                            prn_nums.putInt(port_n);
                            prn_nums.putInt(ip_size);

                            prn_nums.rewind();
                            byte [] prn_nums_byte = new byte[prn_nums.remaining()];
                            prn_nums.position(0);
                            prn_nums.get(prn_nums_byte);
        

                            byte[] ip_array =ip_n_str.getBytes();
                            System.arraycopy(prn_nums_byte, 0, send_to_prn , 1, prn_nums_byte.length);
                            System.arraycopy(ip_array, 0, send_to_prn , prn_nums_byte.length+1, ip_array.length);
                          
                            Iterator<members_data> iter_mbr= curr_team.getMembers().iterator();
                            members_data mbr;
                            while(iter_mbr.hasNext()){
                                mbr = iter_mbr.next();
                                if(mbr.getMigrate() == 1){
                                    if(mbr.getThread_code() == thread_n){
                                        try {
                                            mbr.setIp(InetAddress.getByName(ip_n_str));
                                        } catch (UnknownHostException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                        mbr.setPort(port_n);
                                        
                                    }else{
                                        System.out.println("RETRANSMIT THE N MESSAGE TO THE OTHERS");
                                        InetSocketAddress address_migrate_N =  new InetSocketAddress(mbr.getIp(), mbr.getPort());
                                        DatagramPacket packet_send_N= new DatagramPacket(send_to_prn, send_to_prn.length , mbr.getIp(), mbr.getPort());
                                        try {

                                            socket_thread.send(packet_send_N);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                 

                        }else{
                            System.out.println("TAke the N message");
                            Iterator<members_data> iter_member = curr_team.getMembers().iterator();
                            members_data curr_member ;
                            while(iter_member.hasNext()){
                                curr_member = iter_member.next();
                                if(curr_member.getThread_code() == thread_n){
                                 //   System.out.println("main ip "  +main_ip + "main port "  + main_port + " to ip " + curr_member.getIp() + " to port "+ curr_member.getPort());
                                 try {
                                    curr_member.setIp(InetAddress.getByName(ip_n_str));
                                } catch (UnknownHostException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                 curr_member.setPort(port_n);
                                 
                                 curr_member.setMigrate(1);
                                 System.out.println("SET NEW DATDA FOR "+curr_member.getThread_code()+" "+curr_member.getIp()+" "+curr_member.getPort()+" "+curr_member.getMigrate());
                                    break;
                    
    
                                }
                           
                            }
                        }
                        //if we are the main send the message to all the others that are migrated except from the same thread
                        //else just change the data fro this thread
        
                        break;
                    }
                }
                teams_lock.unlock();
            }

        }
        
       
    }



    public void read_from_coor_socket() throws FileNotFoundException{
        DatagramPacket receivingPacket ;
        byte[] packet_receive = new byte[1024];
        int team_code;
        byte[] team_code_byte= new byte[4];
        byte[] buf = new byte[1024];
        char type_message ;
        

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
            for(int i=0; i<4; i++){
                team_code_byte[i] = buf[i+1];
        

            }

            
            team_code = ByteBuffer.wrap(team_code_byte).getInt();
            System.out.println("thread just receive something code "+ team_code+ type_message) ;
            if(type_message == 'T'){
               /// System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                try {
                    code_lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                code_init = team_code;
                code_lock.unlock();
            }
        }
        
       
    }
    public void compilation_run(){

        List <String> args = new LinkedList<String>();
        List<String> wordslist = new LinkedList<String>();
        List<labels> labels_list = new LinkedList<labels>();
        int header = 0;
        List<variable> var_list = new LinkedList<variable>();
        String Name_of_programm ="";
        if(data_for_thread.getRun_command().equals("")){
            wordslist.add("run") ;
        }else{
            String[] words=data_for_thread.getRun_command().split(" ");

            for(int j=0; j<words.length; j++){
                if(!words[j].equals("")){
                    wordslist.add(words[j]);
                }

            }
            for (int i = 1 ; i < wordslist.size() ; i++){
                args.add(wordslist.get(i));
            }
        }
        if(wordslist.get(0).equals("run")){
            File tempFile = null;
            if(data_for_thread.getRun_command().equals("")){
                
                try {
                    teams_lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                  Iterator<teams_data> iter_t = teams.iterator();
                  teams_data t;
                 
                  
                  while(iter_t.hasNext()){
                    t= iter_t.next();
                    if(t.getTeam_code() == data_for_thread.getTeam_code() ){
                        Iterator<members_data> iter_m = t.getMembers().iterator();
                        members_data m ;
                        while(iter_m.hasNext()){
                            m = iter_m.next();
                            if(m.getThread_code() == data_for_thread.getThread_code()){
                                header = m.getHeader();
                                Name_of_programm =m.getProgramm_running();
                                tempFile = new File(m.getProgramm_running());
                                Iterator<variable> iter_v = m.getVars().iterator();
                                variable v;
                                while(iter_v.hasNext()){
                                    v = iter_v.next();
                                    variable new_var  = new variable(v.getValue_string(),v.getName());
                                    var_list.add(new_var);
                                    System.out.println("vaaar " +new_var.getName()+" "+new_var.getValue_string());
                                    //m.removeVars();
                                }
                                Iterator<labels> iter_l = m.getLabels().iterator();
                                labels l;
                                while(iter_l.hasNext()){
                                    System.out.println("adding labels");
                                    l = iter_l.next();
                                    labels new_var  = new labels(l.getName(),l.getTo_seek(),l.getLine());
                                    labels_list.add(new_var);
                                    //m.removeVars();
                                }
                            }
                        }
                    }
                  }
                  teams_lock.unlock();

            }else{
                Name_of_programm = wordslist.get(1);
                tempFile = new File(wordslist.get(1));
                System.out.println(wordslist.get(1));
                System.out.println(wordslist.size());
            }
       
            boolean exists = tempFile.exists();
            if(exists){
System.out.println("OMG "+data_for_thread.getRun_command());
                if(!data_for_thread.getRun_command().equals("")){
System.out.println("SETTING ");
                    try {
                        teams_lock.lock();
                    } catch (InterruptedException e3) {
                        // TODO Auto-generated catch block
                        e3.printStackTrace();
                    }
                    Iterator<teams_data> iter_team = teams.iterator();
                    teams_data curr_team;
                    while(iter_team.hasNext()){
                        curr_team = iter_team.next();
                        if(curr_team.getTeam_code() == data_for_thread.getTeam_code()){
                            System.out.println("MPHKA "+ curr_team.getTeam_code() );

                            Iterator<members_data> iter_member = curr_team.getMembers().iterator();
                            members_data curr_member ;
                            while(iter_member.hasNext()){
                                curr_member = iter_member.next();

                                if(curr_member.getThread_code() == data_for_thread.getThread_code()){
                                    System.out.println("MPHKA THREAD" );

                                    curr_member.setProgramm_running(Name_of_programm);
                                    System.out.println("SET PROGRAMM NAME : "+Name_of_programm+"   ---  "+curr_member.getProgramm_running());
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    teams_lock.unlock();
                }
                String line = "";
                String[] rules;

                RandomAccessFile randomac = null;
                try {
                    randomac = new RandomAccessFile(Name_of_programm, "rw");
                } catch (FileNotFoundException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
                if(!data_for_thread.getRun_command().equals("")){
                    //list for variables
                    Iterator <String>iter = args.iterator();
                    String arg ; 
                    int arg_num = 0;
                    while(iter.hasNext()){
                        arg = iter.next();
                        variable new_arg = new variable();
                        new_arg.setName("arg"+ String.valueOf(arg_num));
                        new_arg.setValue_string(arg);
                        var_list.add(new_arg);
                        arg_num++;
                    }
                    variable new_arg = new variable();
                    new_arg.setName("argc");
                    new_arg.setValue_string(String.valueOf(args.size()));
                    var_list.add(new_arg);
                }
                    //list for labels
                    long position=0;
                     long position_label = 0;
                    int error_line= 0;
                    if(data_for_thread.getRun_command().equals("")){
                        position = data_for_thread.getPosition();
                  
                        error_line= data_for_thread.getErr_line();
                        try {
                            randomac.seek(position);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    while(true){
                        //check if the migration is one to send the data to another 
                        try {
                            teams_lock.lock();
                        } catch (InterruptedException e2) {
                            // TODO Auto-generated catch block
                            e2.printStackTrace();
                        }
                        Iterator<teams_data> team_iter = teams.iterator();
                        teams_data mi_team ;
                        int index_of_team = 0;
                        while(team_iter.hasNext()){
                            mi_team = team_iter.next();
                            if(mi_team.getTeam_code() == data_for_thread.getTeam_code()){
                                Iterator<members_data>   member_iter =  mi_team.getMembers().iterator();
                                members_data curr_member;
                                while(member_iter.hasNext()){
    
                                    curr_member = member_iter.next();
                                    if(curr_member.getThread_code() == data_for_thread.getThread_code()){
                                        try {
                                             Thread.sleep(5000);
                                         } catch (InterruptedException e3) {
                                             break;
                                             // TODO Auto-generated catch block
                                             //e3.printStackTrace();
                                         }
                                     
                                        if(curr_member.getMigrate() == 1){

                                            System.out.println("Time for migration -- "+data_for_thread.getThread_code()+" "+curr_member.getProgramm_running()+" "+curr_member.getIp()+" "+curr_member.getPort());
                                            request newReq = new request();
                                            newReq.setNewFileName(curr_member.getProgramm_running());
                                            newReq.setAcked(0);
                                            newReq.setReqid(seqno);
                                           
                                            byte[] send_to_mig = new byte[9+ curr_member.getProgramm_running().length()];
                                            //type F
                                            System.out.println("Send F message : ");
                                            send_to_mig[0] = 'F';
                                            ByteBuffer migrate_nums = ByteBuffer.allocate(8);
                                            migrate_nums.putInt(seqno);
                                            migrate_nums.putInt(curr_member.getProgramm_running().length());
                                            migrate_nums.rewind();
                                            byte [] migrate_nums_byte = new byte[migrate_nums.remaining()];
                                            migrate_nums.position(0);
                                            migrate_nums.get(migrate_nums_byte);
                        

                                            byte[] filename_array = curr_member.getProgramm_running().getBytes();
                                            System.arraycopy(migrate_nums_byte, 0, send_to_mig , 1, migrate_nums_byte.length);
                                            System.arraycopy(filename_array, 0, send_to_mig , migrate_nums_byte.length+1, filename_array.length);
                                            reqs.add(newReq);
                                          //  InetAddress inetAddress = InetAddress.getByName(address_migrate.getAddress());

                                            InetSocketAddress address_migrate =  new InetSocketAddress(curr_member.getIp(), curr_member.getPort());
                                            
                                            DatagramPacket packet_send = new DatagramPacket(send_to_mig, send_to_mig.length , curr_member.getIp(), curr_member.getPort());
                                            System.out.println("address migrate " + address_migrate.getAddress() + "  port  " + address_migrate.getPort());
                                            try {
                                                socket_thread.send(packet_send);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            seqno++;
                                            int found = 0 ;
                                            String filename = "";
                                            while(true){
                                                Iterator <request>iter_req = reqs.iterator();
                                               request req ;
                                              
                                                while(iter_req.hasNext()){
                                                    req = iter_req.next();
                                                   
                                                    if(req.getReqid() == newReq.getReqid()){
                                                        if(req.getAcked() == 1){
                                                            found = 1;
                                                          //  System.out.println("-->Acked seqno " + req.getReqid());
                                                            filename = req.getNewFileName();
                                                            System.out.println(filename+"      newwwwwwwwwww");
                                                            break ; 
                                                        }
                                                    }
                                                }
                                                if(found == 1){
                                                    break ;
                                                }
                                            }
                                            InetSocketAddress address_object  = null;
                                            String fileOpen = curr_member.getProgramm_running();
                                            byte[] fileByteArray = readFileToByteArray(new File(fileOpen)); // Array of bytes the file is made of
                                            
                                            for (int i = 0; i < fileByteArray.length; i = i + (1024 -14 - filename.length())) {
                                                boolean flag = false ;
                                                ByteBuffer send_migrate_buffer = ByteBuffer.allocate(13);
                                                send_migrate_buffer.putInt(seqno).putInt(filename.length());
                                            
                                               filename_array = filename.getBytes();
                                                
                                                if(i + (1024 -14 - filename.length()) > fileByteArray.length){
                                                    send_migrate_buffer.putInt(fileByteArray.length-i);
                                                    send_migrate_buffer.put((byte) 1);
                                                    flag = true ;
                                                }
                                                else{
                                                    send_migrate_buffer.putInt(1024 -13- filename_array.length-1-filename_array.length);
                                                    send_migrate_buffer.put((byte) 0);
                                                }
                                                

                                                send_migrate_buffer.rewind();
                                                byte [] send_migrate_byte = new byte[send_migrate_buffer.remaining()];

                                                send_migrate_buffer.position(0);
                                                send_migrate_buffer.get(send_migrate_byte);
                                                System.out.println("part of file to send " + new String(Arrays.copyOfRange(fileByteArray, i, fileByteArray.length)));


                                                // Create message
                                                System.out.println("C message:");
                                                byte[] message = new byte[1024]; // First two bytes of the data are for control (datagram integrity and order)
                                                message[0] = 'C';
                                             
                                    
                                                if (!flag) {
                                                    System.out.println("not the last packet");
                                                    System.arraycopy(send_migrate_byte, 0, message, 1, send_migrate_byte.length);
                                                    System.arraycopy(filename_array , 0, message, 1+ send_migrate_byte.length,filename_array.length);
                                                    System.arraycopy(fileByteArray, i, message, send_migrate_byte.length+filename_array.length+1 , 1024 -send_migrate_byte.length- filename_array.length-1);
                                                } else { // If it is the last datagram
                                                    System.arraycopy(send_migrate_byte, 0, message, 1, send_migrate_byte.length);
                                                    System.arraycopy(filename_array , 0,message ,send_migrate_byte.length+1,filename_array.length);
                                                    System.arraycopy(fileByteArray, i, message, send_migrate_byte.length+filename_array.length +1, fileByteArray.length-i);
                                                    System.out.println("the last packet size  "+ (fileByteArray.length - i));
                                          
                                                }
                                                newReq = new request();
                                                newReq.setAcked(0);
                                                newReq.setReqid(seqno);
                                                
                                                reqs.add(newReq);
                                                System.out.println("address migrate " + address_migrate.getAddress() + "  port  " + address_migrate.getPort());
                                                packet_send = new DatagramPacket(message, message.length , address_migrate);
                                                seqno++;
                                               
                                                try {
                                                    socket_thread.send(packet_send);
                                                    
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                found = 0 ;
                                                while(true){
                                                    Iterator <request>iter_req = reqs.iterator();
                                                   request req ;
                                                  
                                                    while(iter_req.hasNext()){
                                                        req = iter_req.next();
                                                       
                                                        if(req.getReqid() == newReq.getReqid()){
                                                            if(req.getAcked() == 1){
                                                                found = 1;
                                                                address_object =  new InetSocketAddress(curr_member.getIp(), req.getPort());
                                                                break ; 
                                                            }
                                                        }
                                                    }
                                                    if(found == 1){
                                                        break ;
                                                    }
                                                }
                                                if(flag){
                                                    break ; 
                                                }

                                            }
                                        
                                           // System.out.println("-->Acked seqno for c " + newReq.getReqid());
                                            /////code for m 
                                          //  System.out.println("Seqno before m " + seqno);
                                           // ByteBuffer send_migrate_buffer = ByteBuffer.allocate(24);
                                            //System.out.println(send_migrate_buffer.remaining());
                                            //send_migrate_buffer.putInt(seqno).putInt(filename.length()).putInt(error_line).putLong(position);


                                            //members_data send_object =  new members_data();
                                            curr_member.setSeqno(seqno);
                                            curr_member.setError_line(error_line);
                                            curr_member.setPosition(position);
                                            curr_member.setAddress("10.0.0.2");
                                            curr_member.setStart_port(socket_thread.getLocalPort());
                                            curr_member.setProgramm_running(filename);
                                            curr_member.setVars(var_list);
                                            curr_member.setLabels(labels_list);
                                            curr_member.setHeader(header);
                                            System.out.println("Before serialize :");
                                            teams_data send_obdj = new teams_data();
                                            send_obdj.setMembers(mi_team.getMembers());
                                            Iterator<members_data> iter_mb = send_obdj.getMembers().iterator();
                                            members_data mb = new members_data();
                                            while(iter_mb.hasNext()){
                                                mb = iter_mb.next();
                                                if(mb.getMigrate() == 1){
                                                    if(mb.getThread_code() != curr_member.getThread_code()){
                                                        mb.setMigrate(0);
                                                    }
                                                }
                                            }
                                            send_obdj.setTeam_code(mi_team.getTeam_code());
                                          
                                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                            ObjectOutputStream oos;
                                            try {
                                                oos = new ObjectOutputStream(bos);
                                                oos.writeObject(send_obdj);
                                            } catch (IOException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                          
                                            //send my vids with reliable send to the team
                                            byte[] message = bos.toByteArray();

                                            
                                            newReq = new request();
                                            newReq.setNewFileName(curr_member.getProgramm_running());
                                            newReq.setAcked(0);
                                            newReq.setReqid(seqno);
                                            seqno++;
                                            reqs.add(newReq);
                                            packet_send = new DatagramPacket(message, message.length ,  address_object);
                                    
                                            try {
                                                System.out.println("send M packet to other programm");
                                                socket_thread.send(packet_send);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            found = 0 ;
                                            while(true){
                                                Iterator <request>iter_req = reqs.iterator();
                                               request req ;
                                              
                                                while(iter_req.hasNext()){
                                                    req = iter_req.next();
                                                   
                                                    if(req.getReqid() == newReq.getReqid()){
                                                        if(req.getAcked() == 1){
                                                            found = 1;
                                                           // System.out.println("-->Acked seqno for m " + req.getReqid());
                                                            
                                                            break ; 
                                                        }
                                                    }
                                                }
                                                if(found == 1){
                                                    ////////////////////////////////////////////////////////
                                                     //send to another
                                                     byte[] send_to_prn = new byte[21+ curr_member.getIp().toString().length()-1];
                                                    send_to_prn[0] = 'N';
                                                    ByteBuffer prn_nums = ByteBuffer.allocate(20);
                                                    prn_nums.putInt(seqno);
                                                    prn_nums.putInt(mi_team.getTeam_code());
                                                    prn_nums.putInt(curr_member.getThread_code());

                                                    prn_nums.putInt(curr_member.getPort());
                                                    prn_nums.putInt(curr_member.getIp().toString().length()-1);

                                                    prn_nums.rewind();
                                                    byte [] prn_nums_byte = new byte[prn_nums.remaining()];
                                                    prn_nums.position(0);
                                                    prn_nums.get(prn_nums_byte);
                                
        
                                                    byte[] ip_array = curr_member.getIp().toString().replace("/","").getBytes();
                                                    System.arraycopy(prn_nums_byte, 0, send_to_prn , 1, prn_nums_byte.length);

                                                    System.arraycopy(ip_array, 0, send_to_prn , prn_nums_byte.length+1, ip_array.length);
                                                    
                                                    if(curr_member.getStart_ip() == main_ip && curr_member.getStart_port() == main_port){
                                                        Iterator<members_data> iter_mbr = mi_team.getMembers().iterator();
                                                        members_data mbr ;
                                                        while(iter_mbr.hasNext()){
                                                            mbr = iter_mbr.next();
                                                            if(mbr.getIp() != main_ip && mbr.getIp() != curr_member.getIp()){
                                                                //InetSocketAddress address_migrate_N =  new InetSocketAddress(mbr.getIp(), mbr.getPort());
                                                               // System.out.println("AAAAAAAAA "+mbr.getIp()+mbr.getPort());
                                                                DatagramPacket packet_send_N= new DatagramPacket(send_to_prn, send_to_prn.length ,mbr.getIp(),mbr.getPort());
                                                                try {
                                                                    System.out.println("Send N message to : " +   mbr.getIp()+" "+mbr.getPort());
            
                                                                    socket_thread.send(packet_send_N);
                                                                } catch (IOException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }
                                                        }

                                         
                                             
                                                    }else{

                                                        InetSocketAddress address_migrate_N =  new InetSocketAddress(curr_member.getStart_ip(), curr_member.getStart_port());
                                                        DatagramPacket packet_send_N= new DatagramPacket(send_to_prn, send_to_prn.length ,address_migrate_N);
                                                        try {
                                                            System.out.println("Send N message to : " +   curr_member.getStart_ip()+" "+ curr_member.getStart_port());
    
                                                            socket_thread.send(packet_send_N);
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                           
        
                                                    seqno++;
                                                    int check = isthelastmigrate((LinkedList<members_data>)mi_team.getMembers());
                                                    if(check == 0){
                                                        team_iter.remove();
                                                        System.out.println("REMOVE THE TEAM");
                                                    }
                                                    teams_lock.unlock();

                                                    return ;
                                                }
                                            }
                                            ////////////

                                        }
                                    }                                    
                                }
                                break;
                            }
                            index_of_team++;
                        }
                        teams_lock.unlock();

                        int counter = 0; 

                        try {
                            position_label = randomac.getFilePointer();

                            line = randomac.readLine();
                            position = randomac.getFilePointer();

                            error_line++;

                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        if(line== null){
                            System.out.println("No return statement at line "+error_line);    
                            break;            
                        }
                        if(line.equals("")){
                            System.err.println("blank line");
                            continue;
                        }
                        rules=line.split(" ");
                        List<String> ruleslist = new LinkedList<String>();
                        for(int j=0; j<rules.length; j++){
                            if(!rules[j].equals("")){
                                ruleslist.add(rules[j]);
                            }
            
                        }
                        if(header == 0){
                            System.out.println("SIMPLESCRIPT");
                            if( !ruleslist.get(counter).equals("#SIMPLESCRIPT")){
                                System.out.println("error in code at line      "+error_line);

                                break ;
                                
                            }else{
                                header =1;
                                continue;
                            }
                        }
                        if( ruleslist.get(counter).startsWith("#")){
                            System.out.println("LABEL");

                            labels new_label = new labels();
                            String new_label_str = rules[0].substring(1, ruleslist.get(counter).length());
                            if(contains_label(labels_list, new_label_str) == -1){
                                new_label.setName(new_label_str);
                                new_label.setTo_seek(position_label);
                                new_label.setLine(error_line-1);
                                labels_list.add(new_label);
                            }
                            ruleslist.remove(0);
                        }
                        
                        variable var;
                        ///////////////////////////////ADD////////////////////////////////
                      System.out.println("ENTOLHHH   "+ruleslist.get(counter));
                        if( ruleslist.get(counter).equals("ADD")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                if(ruleslist.get(counter).startsWith("$")){
                                    String curr_name =  ruleslist.get(counter).substring(1, ruleslist.get(counter).length());
                                    int current_pos = contains_variable(var_list,curr_name) ;
                           
                                    counter++;
                                    int result=0;
                                    int first_int = 0;
                                    int second_int =0;
                                    try{
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(first_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                            }
                                        }else{
                                                first_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(second_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                            }
                                        }else{
                                            second_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        result = first_int+second_int;
                                    }catch(NumberFormatException e){
                                        System.out.println("error in code at line      "+error_line);

                                        break ;
                                    }
                             
                                    if(current_pos !=-1){
                                        var_list.get(current_pos).setValue_string(String.valueOf(result));
                                        System.out.println(curr_name+"   "+Integer.parseInt(var_list.get(current_pos).getValue_string()));

                                    }else{
                                        var = new variable();
                                        var.setName(curr_name);
                                        var.setValue_string(String.valueOf(result));
                                        var_list.add(var);
                                        System.out.println(curr_name+"   "+result);

                                    }
                                }else{
                                    System.out.println("error in code at line      "+error_line);
                                    break;

                                }

                            }else{
                                System.out.println("Error in line "+error_line);
                                break ;
                            }
                            
                       
            //////////////////////////////////////////////////////SET////////////////////////////////////////////////////////////////////
                        }else if(ruleslist.get(counter).equals("SET")){
                            counter++;
                            if( ruleslist.size() == 3){
                                //check for $ var
                                if(ruleslist.get(counter).startsWith("$")){
                                    String curr_name =  ruleslist.get(counter).substring(1, ruleslist.get(counter).length());
                                    int current_pos = contains_variable(var_list,curr_name) ;
                           
                                    counter++;
                                    int result=0;
                                    int first_int = 0;
                                    int second_int =0;
                                    try{
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(first_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                            }
                                        }else{
                                                first_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                       
                                    }catch(NumberFormatException e){
                                        System.out.println("error in code at line      "+error_line);

                                        break ;
                                    }
                             
                                    if(current_pos !=-1){
                                        var_list.get(current_pos).setValue_string(String.valueOf(first_int));

                                    }else{
                                        var = new variable();
                                        var.setName(curr_name);
                                        var.setValue_string(String.valueOf(first_int));
                                        var_list.add(var);
                                        System.out.println(curr_name+"   "+first_int);

                                    }
                                }else{
                                    System.out.println("error in code at line      "+error_line);
                                    break;

                                }

                            }else{
                                System.out.println("Error in line "+error_line);
                                break ;
                            }
                            
                            

            ////////////////////////////////////////////SUB/////////////////////////////////////////
                        }else if(  ruleslist.get(counter).equals("SUB")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                if(ruleslist.get(counter).startsWith("$")){
                                    String curr_name =  ruleslist.get(counter).substring(1, ruleslist.get(counter).length());
                                    int current_pos = contains_variable(var_list,curr_name) ;
                           
                                    counter++;
                                    int result=0;
                                    int first_int = 0;
                                    int second_int =0;
                                    try{
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(first_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                            }
                                        }else{
                                                first_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(second_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                            }
                                        }else{
                                            second_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        result = first_int - second_int;
                                    }catch(NumberFormatException e){
                                        System.out.println("error in code at line      "+error_line);

                                        break ;
                                    }
                             
                                    if(current_pos !=-1){
                                        var_list.get(current_pos).setValue_string(String.valueOf(result));
                                        System.out.println(curr_name+"   "+Integer.parseInt(var_list.get(current_pos).getValue_string()));

                                    }else{
                                        var = new variable();
                                        var.setName(curr_name);
                                        var.setValue_string(String.valueOf(result));
                                        var_list.add(var);
                                        System.out.println(curr_name+"   "+result);

                                    }
                                }else{
                                    System.out.println("error in code at line      "+error_line);
                                    break;

                                }

                            }else{
                                System.out.println("Error in line "+error_line);
                                break ;
                            }
            ///////////////////////////////////////////mul//////////////////////////////////////////////////
                        }else if(  ruleslist.get(counter).equals("MUL")){

                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                if(ruleslist.get(counter).startsWith("$")){
                                    String curr_name =  ruleslist.get(counter).substring(1, ruleslist.get(counter).length());
                                    int current_pos = contains_variable(var_list,curr_name) ;
                           
                                    counter++;
                                    int result=0;
                                    int first_int = 0;
                                    int second_int =0;
                                    try{
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(first_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                            }
                                        }else{
                                                first_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(second_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                            }
                                        }else{
                                            second_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        result = first_int * second_int;
                                    }catch(NumberFormatException e){
                                        System.out.println("error in code at line      "+error_line);

                                        break ;
                                    }
                             
                                    if(current_pos !=-1){
                                        var_list.get(current_pos).setValue_string(String.valueOf(result));
                                        System.out.println(curr_name+"   "+Integer.parseInt(var_list.get(current_pos).getValue_string()));

                                    }else{
                                        var = new variable();
                                        var.setName(curr_name);
                                        var.setValue_string(String.valueOf(result));
                                        var_list.add(var);
                                        System.out.println(curr_name+"   "+result);

                                    }
                                }else{
                                    System.out.println("error in code at line      "+error_line);
                                    break;

                                }

                            }else{
                                System.out.println("Error in line "+error_line);
                                break ;
                            }
            ////////////////////////////////div/////////////////////////////////////////////////////////
                        }else if(  ruleslist.get(counter).equals("DIV")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                if(ruleslist.get(counter).startsWith("$")){
                                    String curr_name =  ruleslist.get(counter).substring(1, ruleslist.get(counter).length());
                                    int current_pos = contains_variable(var_list,curr_name) ;
                           
                                    counter++;
                                    int result=0;
                                    int first_int = 0;
                                    int second_int =0;
                                    try{
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(first_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                            }
                                        }else{
                                                first_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(second_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                            }
                                        }else{
                                            second_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        result = first_int/second_int;
                                    }catch(NumberFormatException e){
                                        System.out.println("error in code at line      "+error_line);

                                        break ;
                                    }
                             
                                    if(current_pos !=-1){
                                        var_list.get(current_pos).setValue_string(String.valueOf(result));
                                        System.out.println(curr_name+"   "+Integer.parseInt(var_list.get(current_pos).getValue_string()));

                                    }else{
                                        var = new variable();
                                        var.setName(curr_name);
                                        var.setValue_string(String.valueOf(result));
                                        var_list.add(var);
                                        System.out.println(curr_name+"   "+result);

                                    }
                                }else{
                                    System.out.println("error in code at line      "+error_line);
                                    break;

                                }

                            }else{
                                System.out.println("Error in line "+error_line);
                                break ;
                            }
                            
            //////////////////////////////////mod///////////////////////////////////////////////////////////////////
                        }else if(  ruleslist.get(counter).equals("MOD")){

                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                if(ruleslist.get(counter).startsWith("$")){
                                    String curr_name =  ruleslist.get(counter).substring(1, ruleslist.get(counter).length());
                                    int current_pos = contains_variable(var_list,curr_name) ;
                           
                                    counter++;
                                    int result=0;
                                    int first_int = 0;
                                    int second_int =0;
                                    try{
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(first_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                            }
                                        }else{
                                                first_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        if(ruleslist.get(counter).startsWith("$")){
                                            int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                            if(second_pos==-1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{
                                                second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                            }
                                        }else{
                                            second_int = Integer.parseInt( ruleslist.get(counter));
                                        }
                                        counter++;
                                        result = first_int%second_int;
                                    }catch(NumberFormatException e){
                                        System.out.println("error in code at line      "+error_line);

                                        break ;
                                    }
                             
                                    if(current_pos !=-1){
                                        var_list.get(current_pos).setValue_string(String.valueOf(result));
                                        System.out.println(curr_name+"   "+Integer.parseInt(var_list.get(current_pos).getValue_string()));

                                    }else{
                                        var = new variable();
                                        var.setName(curr_name);
                                        var.setValue_string(String.valueOf(result));
                                        var_list.add(var);
                                        System.out.println(curr_name+"   "+result);

                                    }
                                }else{
                                    System.out.println("error in code at line      "+error_line);
                                    break;

                                }

                            }else{
                                System.out.println("Error in line "+error_line);
                                break ;
                            }

                        }else if(ruleslist.get(counter).equals("BGT")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                int first_int = 0;
                                int second_int =0;
                                try{
                                    if(ruleslist.get(counter).startsWith("$")){
                                        int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(first_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                        }
                                    }else{
                                            first_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    
                                    counter++;


                                    if(ruleslist.get(counter).startsWith("$")){
                                        int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(second_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                        }
                                    }else{
                                        second_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    counter++;
                                    String label = ruleslist.get(counter);
                                    int pos = contains_label(labels_list, label);
                                    if(pos == -1){
                                        System.out.println("error in code at line "+error_line);
                                        break ;
                                    }
                                    else {
                                        long pos_seek = labels_list.get(pos).getTo_seek();
                                        if(first_int > second_int){
                                            error_line = labels_list.get(pos).getLine();
                                            randomac.seek(pos_seek);
                                            continue ;
                                        }
                                    }
                                    

                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }
                       
                            
                       
                        }else if(  ruleslist.get(counter).equals("BGE")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                int first_int = 0;
                                int second_int =0;
                                try{
                                    if(ruleslist.get(counter).startsWith("$")){
                                        int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(first_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                        }
                                    }else{
                                            first_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    
                                    counter++;


                                    if(ruleslist.get(counter).startsWith("$")){
                                        int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(second_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                        }
                                    }else{
                                        second_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    counter++;
                                    String label = ruleslist.get(counter);
                                    int pos = contains_label(labels_list, label);
                                    if(pos == -1){
                                        System.out.println("error in code at line "+error_line);
                                        break ;
                                    }
                                    else {
                                        long pos_seek = labels_list.get(pos).getTo_seek();
                                        if(first_int >= second_int){
                                            error_line = labels_list.get(pos).getLine();
                                            randomac.seek(pos_seek);
                                            continue ;
                                        }
                                    }
                                    

                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }

                        }else if(  ruleslist.get(counter).equals("BLT")){
                             
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                int first_int = 0;
                                int second_int =0;
                                try{
                                    if(ruleslist.get(counter).startsWith("$")){
                                        int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(first_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                        }
                                    }else{
                                            first_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    
                                    counter++;


                                    if(ruleslist.get(counter).startsWith("$")){
                                        int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(second_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                        }
                                    }else{
                                        second_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    counter++;
                                    String label = ruleslist.get(counter);
                                    int pos = contains_label(labels_list, label);
                                    if(pos == -1){
                                        System.out.println("error in code at line "+error_line);
                                        break ;
                                    }
                                    else {
                                        long pos_seek = labels_list.get(pos).getTo_seek();
                                        if(first_int < second_int){
                                            error_line = labels_list.get(pos).getLine();
                                            randomac.seek(pos_seek);
                                            continue ;
                                        }
                                    }
                                    

                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }
                        }else if(  ruleslist.get(counter).equals("BLE")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                int first_int = 0;
                                int second_int =0;
                                try{
                                    if(ruleslist.get(counter).startsWith("$")){
                                        int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(first_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                        }
                                    }else{
                                            first_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    
                                    counter++;


                                    if(ruleslist.get(counter).startsWith("$")){
                                        int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(second_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                        }
                                    }else{
                                        second_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    counter++;
                                    String label = ruleslist.get(counter);
                                    int pos = contains_label(labels_list, label);
                                    if(pos == -1){
                                        System.out.println("error in code at line "+error_line);
                                        break ;
                                    }
                                    else {
                                        long pos_seek = labels_list.get(pos).getTo_seek();
                                        if(first_int <= second_int){
                                            error_line = labels_list.get(pos).getLine();
                                            randomac.seek(pos_seek);
                                            continue ;
                                        }
                                    }
                                    

                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }

                        }else if(  ruleslist.get(counter).equals("BEQ")){
                            counter++;
                            if( ruleslist.size() == 4){
                                //check for $ var
                                int first_int = 0;
                                int second_int =0;
                                try{
                                    if(ruleslist.get(counter).startsWith("$")){
                                        int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(first_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            first_int= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                        }
                                    }else{
                                            first_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    
                                    counter++;


                                    if(ruleslist.get(counter).startsWith("$")){
                                        int second_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(second_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            second_int= Integer.parseInt(var_list.get(second_pos).getValue_string());
                                        }
                                    }else{
                                        second_int = Integer.parseInt( ruleslist.get(counter));
                                    }
                                    counter++;
                                    String label = ruleslist.get(counter);
                                    int pos = contains_label(labels_list, label);
                                    if(pos == -1){
                                        System.out.println("error in code at line "+error_line);
                                        break ;
                                    }
                                    else {
                                        long pos_seek = labels_list.get(pos).getTo_seek();
                                        if(first_int == second_int){
                                            error_line = labels_list.get(pos).getLine();
                                            randomac.seek(pos_seek);
                                            continue ;
                                        }
                                    }
                                    

                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }

                        }else if(  ruleslist.get(counter).equals("BRA")){
                            if( ruleslist.size() == 2){
                                counter++;
                                String label = ruleslist.get(counter);
                                int pos = contains_label(labels_list, label);
                                if(pos == -1){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                }
                                else {
                                    
                                    long pos_seek = labels_list.get(pos).getTo_seek();
                                   
                                    try {
                                        error_line = labels_list.get(pos).getLine();
                                        randomac.seek(pos_seek);
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    continue ;
                                
                                }
                        
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }
                   
                          
                        }else if( ruleslist.get(counter).equals("SND")){
                            //every time that we send to an ip same as ours we add it on list of thr specific threa
                            /////////////
                            String message_to_send = "";
                            int to_thread =0;
                            counter++;
                            if( ruleslist.size() >= 3){
                                //check for $ var
                                to_thread = 0;
                                try{
                                    if(ruleslist.get(counter).startsWith("$")){
                                        int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                        if(first_pos==-1){
                                            System.out.println("error in code at line    "+error_line);
                                            break;
                                        }else{
                                            to_thread= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                        }
                                    }else{
                                            to_thread = Integer.parseInt(ruleslist.get(counter));
                                            //exception
                                    }
                                    
                                    counter++;
                                    

                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;
                                }
                              
                                int temp = 0 ;
                                for(int m=counter; m<ruleslist.size(); m++){
                                    if(temp == 0){
                                        if(ruleslist.get(counter).startsWith("\"")){
                                            temp = 1 ;
                                        }
                                        else {
                                            //check for $var
                                            if(ruleslist.get(counter).startsWith("$")){
                                                int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                                if(first_pos==-1){
                                                   
                                                    System.out.println(new Throwable().getStackTrace()[0].getLineNumber() + " Thread " + data_for_thread.getTeam_code() + "." + data_for_thread.getThread_code()+"error in code at line   "+error_line);
                                                    break;
                                                }else{
                                                    message_to_send = message_to_send +" "+var_list.get(first_pos).getValue_string()+ ",";
                                                   
                                        
                                                }
                                            }else{
                                                message_to_send = message_to_send +" "+ruleslist.get(counter) + ",";
                                                 
                                            }
                                            
                                        } 
                                    }
                                    if(temp == 1){
                                        if(ruleslist.get(counter).endsWith("\"")){
                                            message_to_send = message_to_send + ruleslist.get(counter)+",";
                                            temp = 0 ;
                                        }
                                        else {
                                            message_to_send = message_to_send + ruleslist.get(counter);
                                        }
                                    }
                                   
                                    counter++;
                                }

                               
                            
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }



                            //search the thread and send
                            try {
                                teams_lock.lock();
                            } catch (InterruptedException e3) {
                                // TODO Auto-generated catch block
                                e3.printStackTrace();
                            }
                           Iterator<teams_data> iter_team = teams.iterator();
                           teams_data curr_team;
                           int seqno_send = 0;
                            while(iter_team.hasNext()){
                                curr_team = iter_team.next();
                                if(curr_team.getTeam_code() == data_for_thread.getTeam_code()){
                                    Iterator<members_data> iter_member = curr_team.getMembers().iterator();
                                    members_data curr_member ;
                                    while(iter_member.hasNext()){
                                        curr_member = iter_member.next();
                                        if(curr_member.getThread_code() == to_thread){
                                         //   System.out.println("main ip "  +main_ip + "main port "  + main_port + " to ip " + curr_member.getIp() + " to port "+ curr_member.getPort());
                                            if(curr_member.getIp() == main_ip && curr_member.getPort() == main_port){
                                                messages new_mess = new messages();
                                                System.out.println("MESSAGE IS : "+ message_to_send);
                                                new_mess.setMessage(message_to_send);
                                                new_mess.setThread_from(data_for_thread.getThread_code());
                                                new_mess.setIp_from(main_ip);
                                                new_mess.setPort_from(main_port);
                                               
                                                new_mess.setSeqno(seqno);
                                                seqno_send = seqno;
                                                curr_member.getMessages().add(new_mess);
    
                                                break;
                                            }else{
                                                byte []send_to_mig = new byte[1024];
                                                send_to_mig[0] = 'S';
                                                ByteBuffer migrate_nums = ByteBuffer.allocate(20);
                                           
                                                
                                                migrate_nums.putInt(seqno);
                                                migrate_nums.putInt(curr_team.getTeam_code());
                                                migrate_nums.putInt(data_for_thread.getThread_code());
                                                migrate_nums.putInt(to_thread);
                                                migrate_nums.putInt(message_to_send.length());

                                                migrate_nums.rewind();
                                                byte [] migrate_nums_byte = new byte[migrate_nums.remaining()];
                                                migrate_nums.position(0);
                                                migrate_nums.get(migrate_nums_byte);
                                                byte []message_array = message_to_send.getBytes();

                                                System.arraycopy(migrate_nums_byte, 0, send_to_mig , 1, migrate_nums_byte.length);
                                                System.arraycopy(message_array , 0, send_to_mig , migrate_nums_byte.length+1, message_array.length);


                                                System.out.println("SND address " + curr_member.getIp() + " port " + curr_member.getPort()); 
                                                InetSocketAddress address_migrate =  new InetSocketAddress(curr_member.getIp() ,curr_member.getPort());
                                                DatagramPacket packet_send = new DatagramPacket(send_to_mig, send_to_mig.length , address_migrate);
                                        
                                                try {
                                                    socket_thread.send(packet_send);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                //send to another with socket
                                            }
                                            seqno ++ ;
                                        }
                                   
                                    }
                                    break;
                                }
                            }
                            teams_lock.unlock();
                            try {
                                request_lock.lock();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        
                            request send_request = new request();
                            send_request.setAcked(0);
                            send_request.setReqid(seqno);
                            reqs.add(send_request);
                            request_lock.unlock();
                            while(true){
                                try {
                                    request_lock.lock();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            
                                Iterator<request> iter_r = reqs.iterator();
                                request curr_req;
                                int found =0;
                                while(iter_r.hasNext()){
                                    curr_req = iter_r.next();
                                    if(curr_req.getReqid() == seqno_send){
                                        if(curr_req.getAcked() == 1){
                                            found = 1;
                                            break;
                                        }
                                        
                                    }
                
                                }
                                if(found == 1){
                                    request_lock.unlock();
                                    break;
                                }
                                request_lock.unlock();
                            }

                        }else if(ruleslist.get(counter).equals("RCV")){
                                                   //later receive
                             
                             /////////////
                             String new_message ="";
                             int from_thread =0;
                             counter++;
                             if( ruleslist.size() >= 3){
                                 //check for $ var
                                 from_thread = 0;
                                 try{
                                     if(ruleslist.get(counter).startsWith("$")){
                                         int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                         if(first_pos==-1){
                                            
                                             System.out.println(new Throwable().getStackTrace()[0].getLineNumber() + " Thread " + data_for_thread.getTeam_code() + "." + data_for_thread.getThread_code()+"error in code at line   "+error_line);
                                             break;
                                         }else{
                                             
                                             from_thread= Integer.parseInt(var_list.get(first_pos).getValue_string());
                                 
                                         }
                                     }else{
                                             from_thread = Integer.parseInt( ruleslist.get(counter));
                                          
                                     }
                                     
                                     counter++;
                                     
 
                                 }catch(NumberFormatException e){
                                     System.out.println(new Throwable().getStackTrace()[0].getLineNumber()  +" thread : "+data_for_thread.getTeam_code()+" "+data_for_thread.getThread_code() +"error in code at line "+error_line);
                                     break ;
                                 }
                           
                             
                             }else{
                                 System.out.println(new Throwable().getStackTrace()[0].getLineNumber() + " Thread " + data_for_thread.getTeam_code() + "." + data_for_thread.getThread_code()+"error in code at line   "+error_line);
                                 break ;
                             }
 
                             //block until receive a message
                             String receiving_message = "";
                             int found_message = 0 ;
                             while(true){
                                 try {
                                 //search the thread and send
                                     try {
                                         teams_lock.lock();
                                     } catch (InterruptedException e3) {
                                         // TODO Auto-generated catch block
                                         e3.printStackTrace();
                                     }
                                     Iterator<teams_data> iter_team = teams.iterator();
                                     teams_data curr_team;
                                     while(iter_team.hasNext()){
                                         curr_team = iter_team.next();
                                         if(curr_team.getTeam_code() == data_for_thread.getTeam_code()){
                                             Iterator<members_data> iter_member = curr_team.getMembers().iterator();
                                             members_data curr_member ;
                                             while(iter_member.hasNext()){
                                                 curr_member = iter_member.next();
                                                 if(curr_member.getThread_code() == data_for_thread.getThread_code()){
                                             
                                                     //just ckeck the list
                                                     if(curr_member.getMessages().size()  == 0){
             
                                                         break;
                                                     }
                                                     Iterator<messages> mess_iter = curr_member.getMessages().iterator();
                                                     messages nm ;
                                                     while(mess_iter.hasNext()){
                                                         nm= mess_iter.next();
                                                         if(nm.getThread_from() == from_thread){
                                                             //new message from thread that we are waiting 
                                                             
                                                             receiving_message = nm.getMessage();
                                                             System.out.println("------> "+ receiving_message);
                                                          
                                                             if(main_ip == nm.getIp_from() && main_port == nm.getPort_from()){
                                                                //just change it into the list 
                                                            
                                                                try {
                                                                    request_lock.lock();
                                                                } catch (InterruptedException e) {
                                                                    // TODO Auto-generated catch block
                                                                    e.printStackTrace();
                                                                }
                                                            
                                                                Iterator<request> iter_r = reqs.iterator();
                                                                request curr_req;
                                                                int found =0;
                                                                while(iter_r.hasNext()){
                                                                    curr_req = iter_r.next();
                                                                    if(curr_req.getReqid() == nm.getSeqno()){

                                                                        curr_req.setAcked(1);
                                                                        
                                                                    }
                                                
                                                                }
                                                      
                                                                request_lock.unlock();
                                                             }else{
                                                                 //send R message
                                                                 byte[] send_to_mig = new byte[5];
                                                                 //type F
                                                                 send_to_mig[0] = 'R';
                                                                 System.out.println("R message RRR" +nm.getIp_from());
                                                                 ByteBuffer migrate_nums= ByteBuffer.allocate(4);
                                                                 migrate_nums.putInt( nm.getSeqno());
                                                                 migrate_nums.rewind();
                                                                 byte [] migrate_nums_byte = new byte[migrate_nums.remaining()];
                                                                 migrate_nums.position(0);
                                                                 migrate_nums.get(migrate_nums_byte);
                                                     
                                                                 System.arraycopy(migrate_nums_byte, 0, send_to_mig , 1, migrate_nums_byte.length);
                                                             
                                                                 InetSocketAddress address_migrate =  new InetSocketAddress(nm.getIp_from(), nm.getPort_from());
                                                                 DatagramPacket packet_send = new DatagramPacket(send_to_mig, send_to_mig.length , address_migrate);
                                                         
                                                                 try {
                                                                     socket_thread.send(packet_send);
                                                                 } catch (IOException e) {
                                                                     e.printStackTrace();
                                                                 }
                                                             }
                                                             mess_iter.remove();
                                                             found_message =1;
                                                             break;
                                                         }
                                                     }
                                                    //  messages new_mess = new messages();
                                                    //  new_mess.setMessage(new_message);
                                                    //  new_mess.setThread_from(data_for_thread.getThread_code());
                                                    //  curr_member.getMessages().add(new_mess);
 
                                                 }
                                         
                                             }
                                             
                                         }
                                     }
                                     if(found_message == 1){
                                         break;
                                     
                                     }
                                 }finally {
                                     teams_lock.unlock();
                                 }
                                
 
                             }
                             
 
                             //ckeck if the message has the appropriate values from the receiving variables
                             
                             String [] vars_rcv= receiving_message.split(",");
                             if(ruleslist.size()-2 != vars_rcv.length){
                                 System.out.println(new Throwable().getStackTrace()[0].getLineNumber() + " Thread " + data_for_thread.getTeam_code() + "." + data_for_thread.getThread_code()+"error in code at line   "+error_line);
                             }
                             else for(int i = 0 ; i < vars_rcv.length; i++){
                               //  System.out.println("->>>>>       "+vars_rcv[i] + " "+ ruleslist.get(counter));
                             
                                 if(ruleslist.get(counter).startsWith("$")){
                                     int first_pos = contains_variable(var_list,ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                     if(first_pos==-1){
                                         variable new_var = new variable();
                                         new_var.setValue_string(vars_rcv[i]);
                                         new_var.setName(ruleslist.get(counter).substring(1, ruleslist.get(counter).length()));
                                         System.out.println(new Throwable().getStackTrace()[0].getLineNumber() +"RCV: Thread " + data_for_thread.getTeam_code() + "." + data_for_thread.getThread_code()+" adds   "+new_var.getName()+ " value "+ new_var.getValue_string());
                                         var_list.add(new_var);
                                         
                                     }else{
                                         var_list.get(first_pos).setValue_string(vars_rcv[i]);
                                     }
                                 }else{
                                     System.out.println(new Throwable().getStackTrace()[0].getLineNumber() +" Thread " + data_for_thread.getTeam_code() + "." + data_for_thread.getThread_code()+"error in code at line   "+error_line);
                                     break ;
                                     
                                 }
                                 counter ++;
                             }
  
                            
                            

                            //ckeck if the message has the appropriate values from the receiving variables



                        }else if(ruleslist.get(counter).equals("SLP")){

                            if( ruleslist.size() == 2){
                                counter++;
                                int sleep_num=0;
                                try{
                                    sleep_num = Integer.parseInt( ruleslist.get(counter));
                                }catch(NumberFormatException e){
                                    System.out.println("error in code at line "+error_line);
                                    break ;

                                }
                                
                                try {
                                    //wait(sleep_num*1000);
                           
                                    Thread.sleep(sleep_num);
                                  
                
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                   break;
                                }

                         
                            }else{
                                System.out.println("Error on line "+error_line);
                                break ;
                            }

                        }else if(ruleslist.get(counter).equals("PRN")){
                                //PRINT FUNCTION
                                //System.out.println("ruleslistsize : "+ruleslist.size() );
                                if( ruleslist.size() >= 2){

                                    counter++;
                                    String total_print = "";

                                    //we can print or number or string or variable with a specific value 
                                    //strings have " " so we should remove them
                                    //numbers are printed directly and if this is a var we search for this value
                                    String print_value="";
                                    for (int p = counter; p<ruleslist.size(); p++ ){
                                        if(ruleslist.get(p).startsWith("\"")){
                                            print_value = ruleslist.get(p).substring(1,ruleslist.get(p).length());
                                            if(print_value.endsWith("\"")){
                                                print_value = print_value.substring(0,print_value.length()-1);
                                            }
                                            
                                        }else if (ruleslist.get(p).startsWith("$")){
                                            
                                            int pos = contains_variable(var_list,ruleslist.get(p).substring(1, ruleslist.get(p).length()));
                                            if(pos == -1){
                                                System.out.println("error in code at line    "+error_line);
                                                break;
                                            }else{

                                                print_value= var_list.get(pos).getValue_string().replaceAll("\"", "");
                                            }
                                  
                                        }else{
                                            if(print_value.endsWith("\"")){
                                                print_value = ruleslist.get(p).substring(0,ruleslist.get(p).length()-1);
                                            }else{
                                                print_value =ruleslist.get(p);
                                            }
                                        }
                                        total_print = total_print+print_value+" ";
                                        counter++;

                                    }
                                    

                                    //search the thread and send
                                    try {
                                        teams_lock.lock();
                                    } catch (InterruptedException e3) {
                                        // TODO Auto-generated catch block
                                        e3.printStackTrace();
                                    }
                                    Iterator<teams_data> iter_team = teams.iterator();
                                    teams_data curr_team;
                                        while(iter_team.hasNext()){
                                            curr_team = iter_team.next();
                                            if(curr_team.getTeam_code() == data_for_thread.getTeam_code()){
                                                
                                                Iterator<members_data> iter_member = curr_team.getMembers().iterator();
                                                members_data curr_member ;
                                                
                                                while(iter_member.hasNext()){
                                                    curr_member = iter_member.next();
                                                        if (curr_member.getThread_code()== data_for_thread.getThread_code()){
                                                        if(curr_member.getStart_ip().equals(main_ip) && curr_member.getStart_port() == main_port){
                                                          
                                                            System.out.println(total_print+" --- team : "+data_for_thread.getTeam_code() + " thread : "+data_for_thread.getThread_code());

                                                            break;
                                                        }else{
                                                            //send to another
                                                            total_print = total_print+" --- team : "+data_for_thread.getTeam_code() + " thread : "+data_for_thread.getThread_code();
                                                            byte[] send_to_prn = new byte[9+ total_print.length()];
                                                            //type F
                                                            System.out.println("Send P message : ");
                                                            send_to_prn[0] = 'P';
                                                            ByteBuffer prn_nums = ByteBuffer.allocate(8);
                                                            prn_nums.putInt(seqno);
                                                            prn_nums.putInt(total_print.length());
                                                            prn_nums.rewind();
                                                            byte [] prn_nums_byte = new byte[prn_nums.remaining()];
                                                            prn_nums.position(0);
                                                            prn_nums.get(prn_nums_byte);
                                        
                
                                                            byte[] filename_array = total_print.getBytes();
                                                            System.arraycopy(prn_nums_byte, 0, send_to_prn , 1, prn_nums_byte.length);
                                                            System.arraycopy(filename_array, 0, send_to_prn , prn_nums_byte.length+1, filename_array.length);
                                                          
                                                          //  InetAddress inetAddress = InetAddress.getByName(address_migrate.getAddress());
                
                                                            InetSocketAddress address_migrate =  new InetSocketAddress(curr_member.getStart_ip(), curr_member.getStart_port());
                                                            DatagramPacket packet_send = new DatagramPacket(send_to_prn, send_to_prn.length , curr_member.getStart_ip(), curr_member.getStart_port());
                                                            System.out.println("address migrate " + address_migrate.getAddress() + "  port  " + address_migrate.getPort());
                                                            try {
                                                                socket_thread.send(packet_send);
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                            seqno++;
                                                        }
                                                    }
                                                    
                                            
                                                }
                                                break;
                                            }
                                        }
                                        teams_lock.unlock();


    
                             
                                }else{
                                    System.out.println("Error in line "+error_line);
                                    break ;
                                }
    
                                

                        }else if(ruleslist.get(counter).equals("RET")){
                             
                            System.out.println("end");
                            break;
                        }else{
                            System.out.println("error in code at line    "+error_line);
                        }
                    }
         
                
            }else{
                System.out.println("Wrong filename...try again");
            }

        }else{
            System.out.println("Error!!!");
        }
        //remove from list
        return;

}


    private static int isthelastmigrate(LinkedList<members_data> members_checking){
        Iterator<members_data> iter_mb = members_checking.iterator();
        members_data mb;
        int zero =0;
        while(iter_mb.hasNext()){
            mb = iter_mb.next();
            if(mb.getMigrate() == 0){
                zero = 1;
            }
        }
        if(zero ==1){
            return 1;
        }else{
            return 0;
        }

    }
    private static byte[] readFileToByteArray(File file) {
        FileInputStream fis = null;
        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }
    
public int contains_variable(List<variable> vars, String name){

    Iterator<variable> iter = vars.iterator();
    variable z = new variable();
    int position=0;
    while(iter.hasNext()){
        z =iter.next();
        if(z.getName().equals(name)){
            return position;
        }
        position++;
    }
    return -1;
    
}

public int contains_label(List<labels> labels, String name){
    Iterator<labels> iter = labels.iterator();
    labels z = new labels();
    int position=0;
    while(iter.hasNext()){
        z =iter.next();
        if(z.getName().equals(name)){
            return position;
        }
        position++;
    }
    return -1;

}
public void kill_all(int kill_id){
    //Give you set of Threads
    Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
    //////////////////////////////
    //if migration =1 send message to another ip to kill the thread;

    //iterator to find the team
    try {
        teams_lock.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    Iterator<teams_data> team_iter = teams.iterator();
    teams_data curr_team ;
    while(team_iter.hasNext()){
        curr_team = team_iter.next();
        if(curr_team.getTeam_code() == kill_id){
            //kill the threads of all team.
            Iterator<members_data>   member_iter =  curr_team.getMembers().iterator();
            members_data curr_member;
            while(member_iter.hasNext()){

                curr_member = member_iter.next();
                if(curr_member.getMigrate() == 0){
                    //Iterate over set to find yours
                    for(Thread thread : setOfThread){
                        if(thread.getId()== curr_member.getThread_id()){
                            System.out.println("Just kill "+ curr_member.getThread_id());
                            thread.interrupt();
                        }
                    }
    
                }else{
                    //send message 

                }
       

            }
            break;
        }
    }
    teams_lock.unlock();

}


    public static void main(String[] args) throws Exception {
        //init
         oo = null;
          iStream = null;
        migrated_info = new LinkedList<copy_file>();
        teams = new LinkedList<teams_data>(); 
        reqs = new LinkedList<request>(); 
        seqno = 0;
        socket_coordinator = new DatagramSocket(null);
        Thread thread_coor = new Thread(new Runnable(){public void run() {try {
        new Main().read_from_coor_socket();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }}});
        thread_coor.start();
        socket_thread = new DatagramSocket(0,InetAddress.getByName("10.0.0.2"));
        main_ip = InetAddress.getByName("10.0.0.2");
        System.out.println("My ip : "+ main_ip);
        main_port = socket_thread.getLocalPort();
        System.out.println("My Port -->" + main_port);
        Thread thread_th = new Thread(new Runnable(){public void run() {try {
        new Main().read_from_socket();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }}});
        thread_th.start();
       // InetSocketAddress socketAddress = new InetSocketAddress("10.0.0.2", 0);

            //socket_object.connect(socketAddress);

        socket_object = new DatagramSocket(0,InetAddress.getByName("10.0.0.2"));

   
        Thread thread_object = new Thread(new Runnable() {public void run() {new Main().read_for_object();}});

        thread_object.start();


        //read from terminal
        Scanner scaner = new Scanner(System.in);
        


        int end = 0 ;
    
        while(true){
            String input = scaner.nextLine();
            if(input.startsWith("run")){
                String[] threads_run = input.split("\\|\\|");
                //send to coordinator to receive team code
            

                byte[] send_to_corr = new byte[1];
                try {
                    code_lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                code_init = -1;
                code_lock.unlock();
                send_to_corr[0] = 'T';
                
                DatagramPacket packet_send;
                System.out.println("client asks for team code ");
                       InetSocketAddress address_coordinator =  new InetSocketAddress("10.0.0.1", 2020);
                packet_send = new DatagramPacket(send_to_corr, send_to_corr.length , address_coordinator);
        
                try {
                    socket_coordinator.send(packet_send);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(true){
                    try {
                        code_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    
                
                    if(code_init!=-1){
                        code_lock.unlock();

                        break;
                    }
                    code_lock.unlock();
                }
                
                System.out.println("---->client gets team code " + code_init);

                //get team code and create a new node to th elist members
                teams_data new_team  = new teams_data();
                new_team.setTeam_code(code_init);
                int thread_code = 0;

                for(int i=0; i< threads_run.length; i++ ){
                    int curr_code = thread_code;
                    int curr_i = i;
                    int team_code =  code_init;
                    //System.out.println("before RUN "+threads_run[curr_i]);
                    Thread new_thred = new Thread(new Runnable(){public void run() {new Main(team_code,curr_code,threads_run[curr_i],0,0).compilation_run();}});
                    new_thred.start();
                    members_data new_member = new members_data();
                    new_member.setIp(main_ip);
                    new_member.setStart_ip(main_ip);
                    new_member.setStart_port(main_port);
                    new_member.setPort(main_port);
                    new_member.setThread_code(thread_code);
                    thread_code++;
                    new_member.setThread_id(new_thred.getId());
                    new_member.setMigrate(0);
                    new_member.setTeam_code(code_init);

                    new_team.getMembers().add(new_member);

                }
                try{
                    teams_lock.lock();
                }catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                teams.add(new_team);
                teams_lock.unlock();

                
                }else if (input.startsWith("migrate")){
                    System.out.println("-------------Migration------------");   
                    String[] migration_array = input.split(" ");
                    int migration_team=0;
                    int migration_thread=0;
                    InetAddress migrate_ip ;
                    int migrate_port = 0;

                    try{
                    if(migration_array.length == 5){
                        migration_team = Integer.parseInt( migration_array[1]);
                        migration_thread = Integer.parseInt( migration_array[2]);
                        migrate_ip = InetAddress.getByName(migration_array[3]);
                        migrate_port =  Integer.parseInt( migration_array[4]);
                        //iterator to find the team
                        System.out.println("---->migrated "+ migration_team+ migration_thread + " " );
                        teams_lock.lock();
                        Iterator<teams_data> team_iter = teams.iterator();
                        teams_data curr_team ;
                        while(team_iter.hasNext()){
                            curr_team = team_iter.next();
                            if(curr_team.getTeam_code() == migration_team){
                                Iterator<members_data>   member_iter =  curr_team.getMembers().iterator();
                                members_data curr_member;
                                while(member_iter.hasNext()){
                                    curr_member = member_iter.next();
                                    if(curr_member.getThread_code() == migration_thread){
                                        curr_member.setMigrate(1);
                                        curr_member.setIp(migrate_ip);
                                        curr_member.setPort(migrate_port);
                                        System.out.println("MIGRATION "+migrate_ip+" "+migrate_port);

                                    }

                                }
                                break;
                            }
                        }
                
                    }else{
                        System.out.println("Wrong instruction");
                    }
                    }catch(NumberFormatException e){
                        System.out.println("You should the number of team that you want to kill");
                        continue;
                    }finally{
                        teams_lock.unlock();

                    }
                

                }else if(input.startsWith("kill")){
                    System.out.println("-------------Kill------------"); 
                    String[] kill_array = input.split(" ");
                    List<String> kill_list = new  LinkedList<String>();
                    int kill_id=0;
                    try{
                    if(kill_array.length>2){
                        for(int k =0 ; k<kill_array.length; k++){
                            if(!kill_array[k].equals("")){
                                kill_list.add(kill_array[k]);
                            }
                        }

                        kill_id = Integer.parseInt( kill_list.get(1));

                    }else{
                        kill_id = Integer.parseInt( kill_array[1]);
                    }
                    }catch(NumberFormatException e){
                        System.out.println("You should the number of team that you want to kill");
                        teams_lock.lock();
                        continue;
                    }
                    //Give you set of Threads
                    Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                    //////////////////////////////
                    //if migration =1 send message to another ip to kill the thread;
               
                    //iterator to find the team
                    teams_lock.lock();
                    Iterator<teams_data> team_iter = teams.iterator();
                    teams_data curr_team ;
                    while(team_iter.hasNext()){
                        curr_team = team_iter.next();
                        if(curr_team.getTeam_code() == kill_id){
                            //kill the threads of all team.
                            Iterator<members_data>   member_iter =  curr_team.getMembers().iterator();
                            members_data curr_member;
                            while(member_iter.hasNext()){

                                curr_member = member_iter.next();
                                //Iterate over set to find yours
                                for(Thread thread : setOfThread){
                                    if(thread.getId()== curr_member.getThread_id()){
                                        System.out.println("Just kill "+ curr_member.getThread_id());
                                        thread.interrupt();
                                    }
                                }
                            

                            }
                            break;
                        }
                    }
                    teams_lock.unlock();


                }else if (input.startsWith("list")){
                    System.out.println("-------------List------------"); 
                    teams_lock.lock();
                    Iterator<teams_data> team_iter = teams.iterator();
                    teams_data curr_team ;
                    while(team_iter.hasNext()){
                        curr_team = team_iter.next();
                            //kill the threads of all team.
                            
                            Iterator<members_data>   member_iter =  curr_team.getMembers().iterator();
                            members_data curr_member;
                            while(member_iter.hasNext()){
                                
                                curr_member = member_iter.next();
                                if(curr_member.getMigrate() == 0){
                                    System.out.println("Thread : "+curr_member.getThread_code()+" from team : "+curr_team.getTeam_code()+"  running programm: "+curr_member.getProgramm_running());
                                }
                                //kill curr_member.getthread_id()

                            }
                        
                    }
                    teams_lock.unlock();

                }else{
                    System.out.println("Wrong instruction"); 
                    continue;  
                }
        }
    }
}