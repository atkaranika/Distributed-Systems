import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Messenger_api  implements Serializable {
    public static  DatagramSocket socket_manager;
    private static InetAddress address_manager;
    private static int port_manager ;
    private static Socket socket_manager_tcp;
    private static DatagramSocket socket_manager_udp;
    private static PrintWriter socket_write ;
    private static BufferedReader socket_read ;
    private int udp_port ;
    public static String mypid;
    //public  int new_team;
    public static LinkedList <messenger_teams> mess_teams = new LinkedList<>();
    public static LinkedList <team_change> team_changes = new LinkedList<>();
    //var for count the times a messenger is joined somewhere
    public static int join_times;
    //threads
    public Thread thread_1 ; 
    public Thread thread_2 ; 
    //locks
    public static Lock lock = new Lock();
    public static Lock lock_change = new Lock();
    public static Lock lock_send = new Lock();
    
    Messenger_api(){
        
    }
    //function to set the udp port
    public void set_udp_port(int udp){
        udp_port = udp;
    }
    //function for read from tcp
    public void read_from_tcp(){
        InputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        System.out.println();
        while (true) {
            try {
                inputStream = socket_manager_tcp.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
            if(objectInputStream!= null){
                //read the list of new team from the socket
                try {
                    LinkedList<Member_info_send> taken_team = new LinkedList<Member_info_send>();
                    taken_team = (LinkedList<Member_info_send>) objectInputStream.readObject();
                    try {
                        this.lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(taken_team == null){
                        mess_teams.getLast().setTeam_code(-1);
                        continue;
                    }

                    //look the team that is changing view and reaad the new view number
                    int vno =taken_team.getLast().getPort();
                    int code_team = taken_team.getLast().getUdp_port();
                    taken_team.removeLast();
                    if(indexof(code_team, mess_teams) == -1){ 
                        this.lock.unlock();
                        while(true){
                            try {
                                this.lock.lock();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            if(mess_teams.getLast().getTeam_code() == -2){
                               
                                break ;
                            }
                            this.lock.unlock();
                        }
                        //is the first time for this team --> join
                        mess_teams.getLast().setTeam_code(code_team);
                        mess_teams.getLast().setCurrent_view(vno);
                        mess_teams.getLast().setCurrent_team(taken_team);

                        //add node on team_change
                        team_change new_team_ch  = new team_change();
                        new_team_ch.setTeam_code(code_team);
                        new_team_ch.setChange(0);
                        try {
                            this.lock_change.lock();
                         
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        team_changes.add(new_team_ch);
                        this.lock_change.unlock();
                        this.lock.unlock();
                    }else{
                        //already have a view in this team so add the new team and view on next
                        mess_teams.get(indexof(code_team, mess_teams)).setTeam_code(code_team);
                        mess_teams.get(indexof(code_team, mess_teams)).setNext_view(vno);
                        mess_teams.get(indexof(code_team, mess_teams)).setNext_team(taken_team);
                        this.lock.unlock();
                         //call the view change
                        change_view(code_team);
                    }
           
                    Iterator<Member_info_send> read_iter = taken_team.iterator();
                    Member_info_send read_member = new Member_info_send();
                    while(read_iter.hasNext()){
                        read_member = read_iter.next();
                       // new_team_ch.setChange(1);
                        System.out.println("-------------port: "+read_member.getPort()+"  addr: "+read_member.getAddress());
                    } 

                   // new_team = 1;
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else{
                return;
            }
        }  
    }
    //function to read from udp
    public void read_from_udp(){
        while(true){
            byte[] packet_receive = new byte[1024];
            DatagramPacket receivingPacket = new DatagramPacket(packet_receive,1024);
            try {
                socket_manager_udp.receive(receivingPacket);
            } catch (IOException e) {
                    e.printStackTrace();
            }
            byte[] buf = new byte[1024];
            buf = receivingPacket.getData(); 
            this.deliver(buf);
        }

    }
    
    public void change_view(int g){
        try {
            this.lock_send.lock();
          
        } catch (InterruptedException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        try {
            this.lock.lock();   
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        List <mids> list_send_messages = mess_teams.get(indexof(g,mess_teams)).getVids();
        //take the list of messages that the app have already read
        List <mids> list_mids = mess_teams.get(indexof(g,mess_teams)).getTaken_messages();

        Iterator <mids>iter = list_send_messages.iterator();
        mids z ;
        //convert my vids to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(list_send_messages);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.lock.unlock();
        //send my vids with reliable send to the team
        byte[] list_bytes = bos.toByteArray();
        reliable_send(g, list_bytes, list_bytes.length, 0, 1);

       
        Iterator<Member_info_send> it =  mess_teams.get(indexof(g,mess_teams)).getCurrent_team().iterator();
        Member_info_send mb;
        while(it.hasNext()){
            mb = it.next();
            System.out.println("!!!!!!!!!!!!!list  "+mb.getMember_id());
        }
   
        while(true){
            //wait until get all lists
            try {
                try {
                    this.lock.lock();
                    
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }     
                if(mess_teams.get(indexof(g,mess_teams)).getCounter()+1 >= mess_teams.get(indexof(g,mess_teams)).getCurrent_team().size()){
                    
                    break ;
                }
            }finally{
                this.lock.unlock();

            }

        }
       int found_all = 1;
        while(true){
            //wait untill get all messages
            found_all = 1 ;
            try{
                try {
                    this.lock.lock();
              
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                list_send_messages = mess_teams.get(indexof(g,mess_teams)).getVids();
                //take the list of messages that the app have already read
                list_mids = mess_teams.get(indexof(g,mess_teams)).getTaken_messages();
                Iterator <mids> iter_mids = list_send_messages.iterator();
                mids z_mids ;
                while(iter_mids.hasNext()){
                    z_mids = iter_mids.next();
                    System.out.println("VIDS----> "+z_mids.getSeqno()+ "   "+z_mids.getId());

                }
                iter_mids = list_mids.iterator();
                while(iter_mids.hasNext()){
                    z_mids = iter_mids.next();
                    System.out.println("MIDS----> "+z_mids.getSeqno()+ "   "+z_mids.getId());

                }

                iter_mids = list_send_messages.iterator();
                while(iter_mids.hasNext()){
                    z_mids = iter_mids.next();
                    if(!list_mids.contains(z_mids)){
                        found_all = 0 ;
                        break ;
                    }

                }
                if(found_all == 1){
                    break ;
                }
            
            }
            finally {
                this.lock.unlock();
            }
        }
        System.out.println("-->mids isa me vids");
        try {
            this.lock.lock();
            mess_teams.get(indexof(g,mess_teams)).getVids().removeAll(mess_teams.get(indexof(g,mess_teams)).getVids());
            mess_teams.get(indexof(g,mess_teams)).getTaken_messages().removeAll(mess_teams.get(indexof(g,mess_teams)).getTaken_messages());
            mess_teams.get(indexof(g,mess_teams)).getMids_list().removeAll(mess_teams.get(indexof(g,mess_teams)).getMids_list());
            mess_teams.get(indexof(g,mess_teams)).setCurrent_team(mess_teams.get(indexof(g,mess_teams)).getNext_team()) ;
            mess_teams.get(indexof(g,mess_teams)).setCurrent_view(mess_teams.get(indexof(g,mess_teams)).getNext_view()) ;
            mess_teams.get(indexof(g,mess_teams)).setCounter(0);
            mess_teams.get(indexof(g,mess_teams)).setSeqno(0);
            mess_teams.get(indexof(g,mess_teams)).getDelivered().addAll( mess_teams.get(indexof(g,mess_teams)).getMbuf());
            mess_teams.get(indexof(g,mess_teams)).getMbuf().removeAll( mess_teams.get(indexof(g,mess_teams)).getMbuf());
            //mess_teams.get(indexof(g,mess_teams)).getDpid().removeAll( mess_teams.get(indexof(g,mess_teams)).getDpid());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            this.lock.unlock();
        }
        try {
            this.lock_change.lock();
          

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(indexof_change(g, team_changes) != -1){
            team_changes.get(indexof_change(g, team_changes)).setChange(1);

        }
        this.lock_change.unlock();
     
        this.lock_send.unlock();
     

    }

    public void reliable_send(int g ,  byte[] msg , int len, int catoc, int flag_vc){
        //if catoc = 0 fifo  if catoc  =1 catoc

        try {
            this.lock.lock();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        messenger_teams team_to_send= new messenger_teams();   
        team_to_send = mess_teams.get(indexof(g,mess_teams));

        //iterator to members of the team
        Iterator<Member_info_send> iter =team_to_send.getCurrent_team().iterator();
        Member_info_send to_member;
        while (iter.hasNext()){
            String rm_header;
            to_member = iter.next();
            if(flag_vc == 1){
                rm_header = "V-SYNC";
            }
            else {  
                rm_header = "RM-MSG";
            }
            /////find size of items 
            int mypid_length = mypid.length() ;
            int seqno_length = String.valueOf(mess_teams.get(indexof(g,mess_teams)).getSeqno()).length();
            int msg_length = new String(msg, StandardCharsets.UTF_8).length();
            
            ByteBuffer lengths = ByteBuffer.allocate(20);
            lengths.putInt(g).putInt(team_to_send.getCurrent_view()).putInt(mypid_length).putInt(seqno_length).putInt(msg_length).rewind();
           
            //here is where I need to convert the int length to a byte array
            byte[] lengths_arr = new byte[lengths.remaining()];
            lengths.position(0);
            lengths.get(lengths_arr);

            byte[] header_byte = rm_header.getBytes(); 

            String mypid_seqno = mypid +String.valueOf(mess_teams.get(indexof(g,mess_teams)).getSeqno());

            ////////////////////////////////////////////////////items for redeliver
            byte pid_byte[] = mypid.getBytes();
            ByteBuffer pid_length = ByteBuffer.allocate(4);
            pid_length.putInt(mypid_length).rewind();
            byte[] pid_length_arr = new byte[pid_length.remaining()];
            pid_length.position(0);
            pid_length.get(pid_length_arr);
            ////////////////////////////////////////////////////
            
        
            byte[] strmsg = mypid_seqno.getBytes();

            byte[] total_msg_array = new byte[header_byte.length + lengths_arr.length + strmsg.length+ msg.length + pid_byte.length + pid_length_arr.length];
           
            System.arraycopy(header_byte, 0, total_msg_array, 0, header_byte.length );
            System.arraycopy(lengths_arr, 0, total_msg_array, header_byte.length , lengths_arr.length  );
            System.arraycopy(strmsg , 0, total_msg_array, lengths_arr.length + header_byte.length , strmsg.length);
            System.arraycopy(msg , 0, total_msg_array,strmsg.length + lengths_arr.length + header_byte.length, msg.length);
            System.arraycopy(pid_length_arr, 0, total_msg_array,strmsg.length + lengths_arr.length + header_byte.length + msg.length, pid_length_arr.length);
            System.arraycopy(pid_byte, 0, total_msg_array,strmsg.length + lengths_arr.length + header_byte.length + msg.length + pid_length_arr.length, pid_byte.length);

            DatagramPacket packet = new DatagramPacket(total_msg_array , total_msg_array .length,to_member.getAddress().getAddress() , to_member.getUdp_port());
            try {
                System.out.println("send grp : Sent to "+to_member.getMember_id()+" address: "+to_member.getAddress().getAddress()+" port : "+to_member.getUdp_port());
                socket_manager_udp.send(packet);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        this.lock.unlock();
        

    }

    public void grp_send(int g ,  byte[] msg , int len, int catoc){

        try {
            this.lock_send.lock();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        reliable_send(g, msg, len, catoc,0);

        messenger_teams team_to_send= new messenger_teams();
        
        try {
            this.lock.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        team_to_send.setTeam_code(g);
      
        team_to_send = mess_teams.get(indexof(g,mess_teams));

        mids addtovids = new mids();
        addtovids.setId(mypid);
        addtovids.setSeqno(mess_teams.get(indexof(g,mess_teams)).getSeqno());
        team_to_send.getVids().add(addtovids);
        this.lock.unlock();
        System.out.println("send grp: add to vids"+ mypid + "seqno  " + (mess_teams.get(indexof(g,mess_teams)).getSeqno()));
        mess_teams.get(indexof(g,mess_teams)).setSeqno(mess_teams.get(indexof(g,mess_teams)).getSeqno()+1);
        this.lock_send.unlock();
        
    }

    public void  deliver(byte []msg){
        String data = new String(msg,StandardCharsets.UTF_8);
      
        String header = data.substring(0,6);
     
        System.out.println("---->deliver gets  "+ header);
        ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(msg, 6, 26)); // big-endian by default
        int g =wrapped.getInt();
        messenger_teams old_team = new messenger_teams();
        old_team.setTeam_code(g);
        if(mess_teams.contains(old_team)){
            int vno = wrapped.getInt();
            int pid_length = wrapped.getInt();
            int seqno_length = wrapped.getInt();
            int msg_length =  wrapped.getInt();
       
            String pid = new String(Arrays.copyOfRange(msg, 26, 26+pid_length), StandardCharsets.UTF_8);
            String seqno = new String(Arrays.copyOfRange(msg, 26+pid_length, 26+pid_length + seqno_length), StandardCharsets.UTF_8);
            String strmsg = new String(Arrays.copyOfRange(msg, 26+pid_length + seqno_length, 26+pid_length + seqno_length+ msg_length), StandardCharsets.UTF_8);

            ////code for redeliver 
            ByteBuffer wrapped_redeliver_size  = ByteBuffer.wrap(Arrays.copyOfRange(msg, 26+pid_length + seqno_length+ msg_length, 26+pid_length + seqno_length+ msg_length+4));                   //to get sizeof pid who redelivers
            int red_size = wrapped_redeliver_size.getInt();

            String red_pid = new String(Arrays.copyOfRange(msg,  26+pid_length + seqno_length+ msg_length+4,  26+pid_length + seqno_length+ msg_length+4+red_size), StandardCharsets.UTF_8);
            ////////////
            messenger_teams team = new messenger_teams();
            try {
                this.lock.lock();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if(indexof(g, mess_teams) != -1 ){
                if(vno >= mess_teams.get(indexof(g, mess_teams)).getCurrent_view()){
                    mids new_message = new mids();
                    new_message.setId(pid);
                    new_message.setSeqno(Integer.parseInt(seqno));

                    team = mess_teams.get(indexof(g, mess_teams));

                    
                    ByteArrayInputStream list_array = new ByteArrayInputStream(Arrays.copyOfRange(msg, 26+pid_length + seqno_length, 26+pid_length + seqno_length+ msg_length));
                    if (team.getMids_list() == null){
                        team.getMids_list().add(new_message);
                        if(!team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().contains(red_pid)){
                            team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().add(red_pid) ;
                        }
                        ///////////////////////////////////////////////FOR VIEW SYNC
                        if(header.equals("V-SYNC")){
                           
                            try {
                                ObjectInputStream o = new ObjectInputStream(list_array);
                                List <mids> member_vids = (List<mids>) o.readObject();
                                Iterator <mids>iter_member_vids = member_vids.iterator();
                                mids z_member_vids  ;
                                
                                while(iter_member_vids.hasNext()){
                                    z_member_vids = iter_member_vids.next();
                                    if(!team.getVids().contains(z_member_vids)){
                                        team.getVids().add(z_member_vids);
                                    }
                                }
                            } catch (IOException | ClassNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            
                            team.setCounter(team.getCounter()+1);
                        }
                        
                        ////////////////////////////////////////////////

                    }
                    else if(!team.getMids_list().contains(new_message)){
                        System.out.println("pid " + new_message.getId());
                        System.out.println("seqno "  + new_message.getSeqno());
                        //add message
                        team.getMids_list().add(new_message);

                        //add pid that send this message
                    if(!team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().contains(red_pid)){
                        System.out.println("add to red list "+ red_pid + "message" + new_message.getId());
                        team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().add(red_pid) ;
                    }
                    if (!header.equals("V-SYNC")){
                                                     
                        deliver del = new deliver();
                        del.setId(pid);
                        del.setSeqno(Integer.parseInt(seqno));
                        del.setMsg(Arrays.copyOfRange(msg, 26+pid_length + seqno_length, 26+pid_length + seqno_length+msg_length));                       
                            if(vno == team.getCurrent_view()){
                            if(del.getSeqno() == 0){
                                team.getApp_delivered().add(del);
                            }else{
                                //if is not 0
                                Iterator <mids> iter= team.getTaken_messages().iterator();
                                mids z ;
                                
                                int max_seqno = 0 ;
                                while(iter.hasNext()){
                                    z = iter.next();
                        
                                    if(z.getId().equals(del.getId()) && z.getSeqno() > max_seqno){
                                        max_seqno = z.getSeqno();
                                    }
                                }
                                if(max_seqno+1 == Integer.parseInt(seqno)){
                                    team.getApp_delivered().add(del);
                                }else{
                                    team.getDelivered().add(del);
                                    //check if there is a message appropriate for app 
                                                
                                    Iterator<deliver> iter_dl = team.getDelivered().iterator();
                                    deliver dl;
                                    while(iter_dl.hasNext()){
                                        dl = iter_dl.next();
                                        if(dl.getSeqno() == max_seqno+1 && dl.getId().equals(del.getId())){
                                            team.getApp_delivered().add(dl);
                                            iter_dl.remove();
                                        }
                                    }
                                }
                            }
                        }else{
                            team.getMbuf().add(del);
                        }       
                    }
                        printlist(mess_teams.get(indexof(g,mess_teams)).getTaken_messages());

                        
                        ///////////////////////////////////////////////FOR VIEW SYNC

                    if(header.equals("V-SYNC")){
                        int retrans =0;
                        try {
                            ObjectInputStream o = new ObjectInputStream(list_array);
                            List<mids> member_vids;
                            try {
                                member_vids = (List<mids>) o.readObject();
                                Iterator <mids>iter_member_vids = member_vids.iterator();
                                mids z_member_vids  ;
                                while(iter_member_vids.hasNext()){
                                    z_member_vids = iter_member_vids.next();
                                    if(!team.getVids().contains(z_member_vids)){
                                        team.getVids().add(z_member_vids);
                                        retrans =1;
                                    }
                                }
                            } catch (ClassNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        //if(retrans == 1){
                            team.setCounter(team.getCounter()+1);
                        //}
                        System.out.println("deliver: get V-SYNC team counter " + team.getCounter());
                    }
                        ////////////////////////////////////////////////



            }
            else {//if contains

                if(!team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().contains(red_pid)){
                    team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().add(red_pid) ;
                }
            }

                  
                    
                    if(!pid.equals(mypid)){
                    //////diaperasi olis tis omadas
                    Iterator<Member_info_send> iter = team.getCurrent_team().iterator();
                    Member_info_send z ;
                    while(iter.hasNext()){
                        
                        z =  iter.next();
                    
                        if(!team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().contains(mypid)){
                            System.out.println("mypid : "+ mypid);
                            team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().add(mypid) ;
                        }
                    


                        if(!(z.getMember_id().equals(mypid)) && !(team.getMids_list().get(team.getMids_list().indexOf(new_message)).getMembers_send().contains(z.getMember_id()))){
                            
                        
                            
                            /////REDELIVER 
                            ByteBuffer wrapped_new_msg = ByteBuffer.wrap(Arrays.copyOfRange(msg, 0,  26+pid_length + seqno_length+ msg_length )); 
                            byte[] new_msg = new byte[wrapped_new_msg.remaining()];
                            wrapped_new_msg.position(0);
                            wrapped_new_msg.get(new_msg);
                            
                            byte pid_byte[] = mypid.getBytes();
                            ByteBuffer pid_length_redel = ByteBuffer.allocate(4);
                            pid_length_redel.putInt(mypid.length()).rewind();

                            byte[] pid_length_arr = new byte[pid_length_redel.remaining()];
                            pid_length_redel.position(0);
                            pid_length_redel.get(pid_length_arr);

                            byte [] myid = mypid.getBytes();

                            byte[] total_msg_array = new byte[new_msg.length + pid_length_arr.length + myid.length];
                
                            System.arraycopy(new_msg, 0, total_msg_array, 0, new_msg.length );
                
                            System.arraycopy(pid_length_arr, 0, total_msg_array, new_msg.length  , pid_length_arr.length);

                            System.arraycopy(myid, 0, total_msg_array, new_msg.length +pid_length_arr.length , myid.length);

                            ///////////////////////////////////////


                            DatagramPacket packet = new DatagramPacket(total_msg_array,total_msg_array.length,z.getAddress().getAddress() , z.getUdp_port());
                            try {
                                socket_manager_udp.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            }
                this.lock.unlock();
        }
        //}
    
    }
    public void init_messenger(){
      
       // seqno = 0;
        join_times= 0;
        byte []buf;
        char type_message;
        byte [] port_receive = new byte[4];
        InetSocketAddress address =  new InetSocketAddress("230.0.0.5", 1000);
        try {
            this.socket_manager = new DatagramSocket(null);
            address = new InetSocketAddress("230.0.0.5", 1000);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        byte[] discover_message = new byte[1];
        discover_message[0]= 'D';
        DatagramPacket dgram;
        try {
            dgram = new DatagramPacket(discover_message, discover_message.length, address);
         
            socket_manager.send(dgram);
        } catch (UnknownHostException e1) {
            
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DatagramPacket receivingPacket ;
        byte[] discover_message_receive = new byte[1024];
        receivingPacket = new DatagramPacket(discover_message_receive,1024);
        
        try {
            socket_manager.receive(receivingPacket); 
            // port_manager = receivingPacket.getPort();
            address_manager = receivingPacket.getAddress();
            buf = new byte[1024];
            buf = receivingPacket.getData(); 
            type_message = (char)buf[0];
            if(type_message == 'D'){
                port_receive[0] = buf[1];  
                port_receive[1] = buf[2];  
                port_receive[2] = buf[3];  
                port_receive[3] = buf[4];  
                port_manager =  ByteBuffer.wrap(port_receive).getInt();
            }

        } catch (IOException e) {
                e.printStackTrace();
        }

        InetSocketAddress socketAddress = new InetSocketAddress(address_manager, port_manager);
  
        socket_manager_tcp = new Socket();      
        try {
            socket_manager_tcp.connect(socketAddress);
          System.out.println("Connected to "+socket_manager_tcp.getInetAddress().toString()+":"+socket_manager_tcp.getPort());
        
            socket_write= new PrintWriter(socket_manager_tcp.getOutputStream(), true);
            socket_read = new BufferedReader( new InputStreamReader(socket_manager_tcp.getInputStream() ));

        } catch (UnknownHostException e) {
          System.err.println("Don't know about host " + address_manager);
          System.exit(1);
        } catch (IOException e) {
          System.err.println("Couldn't get I/O for the connection to " +address_manager);
          System.exit(1);
        }

    }

    public  int grp_join(String grpname,String myid){
            this.mypid = myid;
            join_times++;
            System.out.println("grp_join begins");
    
            String delimiter = "--";
            String send_join = grpname + delimiter+myid+ delimiter + udp_port;
            try {
                this.lock.lock();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            socket_write.println(send_join);
            
            

            String line;
            if(join_times==1){
            try {
                socket_manager_udp = new DatagramSocket(udp_port);
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
             }
            messenger_teams new_team = new messenger_teams();
            new_team.setTeam_code(-2);
            new_team.setCurrent_view(-2);
            new_team.setGroup_name(grpname);
            
            mess_teams.add(new_team);
            


            if(join_times==1){
               
                //if this is our first join we open the connection
                Thread thread_1 = new Thread(new Runnable(){public void run() {new Messenger_api().read_from_tcp();}});
                thread_1.start();
                thread_2 = new Thread(new Runnable(){public void run() {new Messenger_api().read_from_udp();}});
                thread_2.start();
            }
            this.lock.unlock();
            while(true){
                try {
                    this.lock.lock();
                 
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if(mess_teams.getLast().getTeam_code() != -2){
                    if(mess_teams.getLast().getTeam_code() == -1){
                        this.lock.unlock();
                        return (-1);
                    }else{
                        this.lock.unlock();
                        return mess_teams.getLast().getTeam_code();
                    }
                }
                this.lock.unlock();
            }
    }
   
    public int indexof(int g, List<messenger_teams> list){

        Iterator <messenger_teams> iter = list.iterator();
        messenger_teams z ;
        int counter = 0 ;
        while(iter.hasNext()){
            z = iter.next();
            
            if(z.getTeam_code() == g){
                return counter;
            }
            counter++;

        }
        return -1 ; 
    }
    public static int indexof_change(int g, List<team_change> list){

        Iterator <team_change> iter = list.iterator();
        team_change z ;
        int counter = 0 ;
        while(iter.hasNext()){
            z = iter.next();
            if(z.getTeam_code() == g){
                
                return counter ;
            }
            counter++;

        }
        return -1 ; 
    }
    public static int indexof_mids(String pid, List<mids> list){

        Iterator <mids> iter = list.iterator();
        mids z ;
        int counter = 0 ;
        while(iter.hasNext()){
            z = iter.next();
            if(z.getId().equals(pid)){
                
                return counter ;
            }
            counter++;

        }
        return -1 ; 
    }


    public void grp_leave(int g){
        String grpname ; 
        try {
            this.lock.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Iterator<messenger_teams> iter =  mess_teams.iterator();
        messenger_teams team ; 
        while(iter.hasNext()){
            team = iter.next();
            if(team.getTeam_code() == g){
                grpname = team.getGroup_name();
                System.out.println("send to manager to leave from group "+ grpname);
                String delimiter = "--";
                String send_leave = grpname + delimiter+mypid+ delimiter + udp_port;
                socket_write.println(send_leave);
                break ; 
            }
        }
        this.lock.unlock();

        while(true){
            try {
                this.lock.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Member_info_send check = new Member_info_send();
            check.setMember_id(mypid);
           

            if(!mess_teams.get(indexof(g,mess_teams)).getCurrent_team().contains(check)){
                iter = this. mess_teams.iterator();
                while(iter.hasNext()){
                    team = iter.next();
                    if(team.getTeam_code() == g){
                        iter.remove();    
                    }
                }
                this.lock.unlock();
                break;
            }
            this.lock.unlock();

            try {
                this.lock_change.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Iterator<team_change> iter_ch = team_changes.iterator();
            team_change team_ch;
            while(iter_ch.hasNext()){
                team_ch = iter_ch.next();
                if(team_ch.getTeam_code() == g){
                    iter_ch.remove();
                    break;
                }
            }
            this.lock_change.unlock();
        }

                                                                              //vgazoume to group apo ta group mas 
        return ;
        
    }


    int grp_recv(int g, deliver_mesage msg, int block){
     

         if(block == 1){
             while(true){
                try {
                    this.lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if(indexof(g,mess_teams) == -1){
                    this.lock.unlock();
                    return -2;
                }
                if(mess_teams.get(indexof(g,mess_teams)).getApp_delivered() != null && mess_teams.get(indexof(g,mess_teams)).getApp_delivered().size() > 0){
          
                    byte[] message = mess_teams.get(indexof(g,mess_teams)).getApp_delivered().get(0).getMsg();

                    mids current_mid = new mids();
                    current_mid.setId(mess_teams.get(indexof(g,mess_teams)).getApp_delivered().get(0).getId());
         
                    
            
                    current_mid.setSeqno(mess_teams.get(indexof(g,mess_teams)).getApp_delivered().get(0).getSeqno());
          
                    mess_teams.get(indexof(g,mess_teams)).getTaken_messages().add(current_mid);
                  
            
                    mess_teams.get(indexof(g,mess_teams)).getApp_delivered().remove(0);
                    int max_seqno =0;
                   // int next_seqno = mess_teams.get(indexof(g,mess_teams)).getDpid().get(indexof_mids(current_mid.getId(), mess_teams.get(indexof(g,mess_teams)).getDpid())).getSeqno()+1;
                    
                   Iterator<mids> taken_it =  mess_teams.get(indexof(g,mess_teams)).getTaken_messages().iterator();
                   mids taken_node = new mids();
                   while(taken_it.hasNext()){
                        taken_node = taken_it.next();
                        if(taken_node.getId().equals(current_mid.getId()) && taken_node.getSeqno() > max_seqno){
                            max_seqno = taken_node.getSeqno();
                        }

                   }
              
                   Iterator<deliver> iter_dl = mess_teams.get(indexof(g,mess_teams)).getDelivered().iterator();
                    deliver dl;
                    while(iter_dl.hasNext()){
                        dl = iter_dl.next();
                        if(dl.getSeqno()== max_seqno+1 && dl.getId().equals(current_mid.getId())){

                              mess_teams.get(indexof(g,mess_teams)).getApp_delivered().add(dl);

                            iter_dl.remove();
                        }
                    }
                    msg.setMessage(message);
                    msg.setLength(message.length);
                    this.lock.unlock();
                    return 0 ;
                }
                this.lock.unlock();
             }
         }else{
            try {
                this.lock.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(indexof(g,mess_teams) == -1){
                this.lock.unlock();
                return -2;
            }
            if(mess_teams.get(indexof(g,mess_teams)).getApp_delivered() != null && mess_teams.get(indexof(g,mess_teams)).getApp_delivered().size() > 0){
                byte[] message = mess_teams.get(indexof(g,mess_teams)).getApp_delivered().get(0).getMsg();

                mids current_mid = new mids();
                current_mid.setId(mess_teams.get(indexof(g,mess_teams)).getApp_delivered().get(0).getId());
     
                
        
                current_mid.setSeqno(mess_teams.get(indexof(g,mess_teams)).getApp_delivered().get(0).getSeqno());
      
                mess_teams.get(indexof(g,mess_teams)).getTaken_messages().add(current_mid);
              
        
                mess_teams.get(indexof(g,mess_teams)).getApp_delivered().remove(0);
                int max_seqno =0;
               // int next_seqno = mess_teams.get(indexof(g,mess_teams)).getDpid().get(indexof_mids(current_mid.getId(), mess_teams.get(indexof(g,mess_teams)).getDpid())).getSeqno()+1;
                
               Iterator<mids> taken_it =  mess_teams.get(indexof(g,mess_teams)).getTaken_messages().iterator();
               mids taken_node = new mids();
               while(taken_it.hasNext()){
                    taken_node = taken_it.next();
                    if(taken_node.getId().equals(current_mid.getId()) && taken_node.getSeqno() > max_seqno){
                        max_seqno = taken_node.getSeqno();
                    }

               }
          
               Iterator<deliver> iter_dl = mess_teams.get(indexof(g,mess_teams)).getDelivered().iterator();
                deliver dl;
                while(iter_dl.hasNext()){
                    dl = iter_dl.next();
                    if(dl.getSeqno()== max_seqno+1 && dl.getId().equals(current_mid.getId())){

                          mess_teams.get(indexof(g,mess_teams)).getApp_delivered().add(dl);

                        iter_dl.remove();
                    }
                }
                msg.setMessage(message);
                msg.setLength(message.length);
                this.lock.unlock();
                return 0 ;
            }else{
                this.lock.unlock();
                return -1;
            }
         }
    }

    public void printlist(List <mids>list){
        Iterator <mids>iter = list.iterator();
        mids z ;
        while(iter.hasNext()){
            z = iter.next();
            System.out.println("------>TAKEN: "+ z.getId() + z.getSeqno());
        }


    }


    
}