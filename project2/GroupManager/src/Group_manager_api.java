/* A class for GroupManager functions*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.DataOutputStream;
import java.io.InputStream;


public class Group_manager_api implements Runnable ,Serializable{

    private static int tcp_port = 2000;//init the first tcp connection with 2000 and then increase the number
    public static int number_new_team; //number for new team .. is the team code that the manager gives to each  team ... like a name is unique
    private static  List<Group_info> group_list = new LinkedList<>(); //list of groups
    public  MulticastSocket multicast_socket;//socket to listen multicast
    public Socket socket; //socket for tcp connection ...each thread gives the appropriate value
   
    public static PrintWriter send_data ;           
    public static BufferedReader read_data;
    public static Lock lock = new Lock();
    //constructors
    Group_manager_api(){
    
    }
    Group_manager_api(Socket socket_communication){
        this.socket= socket_communication;
    }

    Group_manager_api(MulticastSocket multicast_socket){
        this.multicast_socket= multicast_socket;
    }



    //run for threads that read from tcp
    public void run() {
        String mess_id_for_thread  = "";
        InputStream inp = null;
        BufferedReader brinp = null;
        DataOutputStream out = null;
        try {
            inp = socket.getInputStream();
            brinp = new BufferedReader(new InputStreamReader(inp));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        String line;
        while (true) {
            try {
                line = brinp.readLine();
                if ((line == null)) {
                    System.out.println("Close the tc connection for "+mess_id_for_thread);
                    socket.close();
                    return;
                } else {

                    String[] splited  = line.split("--");
                    String grpname = splited[0];
                    String mess_id = splited[1]; 
                    mess_id_for_thread = mess_id;
                    int udp_port = Integer.parseInt(splited[2]); 
                    Iterator <Group_info> iter= group_list.iterator();
                    int found = 0 ; 
                    Group_info z ;
                    int find_group = 0 ;
                    //searchimg if thr group already exist in our list 
                    while(iter.hasNext()){
                        z = iter.next();
                        if(grpname.equals(z.getName_of_group())){
                            //we find it into the list
                            Iterator <Member_info> iter_member = z.getGroup_list().iterator();
                            Member_info z_member;
                            while(iter_member.hasNext()){
                                z_member = iter_member.next();
                                if(z_member.getMember_id().equals(mess_id)){  
                                    //the member already exist in the group so this messsage is a leave message
                                    //we send to all members to let them know the new group consistance 
                                    Iterator<Member_info> send_to_members_iter = z.getGroup_list().iterator();
                                    Member_info send_to_members = new Member_info();   
                                    Iterator<Member_info_send> send_iter = z.getMember_list_send().iterator();
                                    Member_info_send remover;
                                    while(send_iter.hasNext()){
                                        remover = send_iter.next();
                                        if(remover.getMember_id().equals(mess_id)){
                                            send_iter.remove();
                                            break;
                                        }

                                    }
                                    while(send_to_members_iter.hasNext()){
                                        send_to_members = send_to_members_iter.next();
                                        System.out.println(">> Sending confirmation for new view of team(after unjoin to : ) "+ send_to_members.getMember_id());
                                        /////sending the new team to the remaining members
                                        new ObjectOutputStream(send_to_members.getTcp_socket().getOutputStream()).writeObject(z.getMember_list_send());
                                    } 
                                    //when all the members take the confirmation remove this member from the list of the current team
                                    z.remove_member(z_member);   
                                    System.out.println("-- Remove "+mess_id+" to " +grpname + " team ");
 
                                    found = 1 ; 
                                }
                                
                            }
                            if (found == 1){
                                    find_group = 1 ;
                                    break ;
                            }
                            else {
                                //new member add on existing team
                                Member_info new_member = new Member_info();
                                Member_info_send new_member_send = new Member_info_send();
                                //change number of view
                                z.setView_number(z.getView_number()+1);
                                new_member.setAddress((InetSocketAddress)socket.getRemoteSocketAddress());
                                new_member.setMember_id(mess_id);
                                new_member.setPort(socket.getPort());
                                new_member.setUdp_port(udp_port);
                                new_member.setTcp_socket(socket);
                                z.add_member(new_member);

                                z.getMember_list_send().get(z.getMember_list_send().size()-1).setAddress((InetSocketAddress)socket.getRemoteSocketAddress());
                                z.getMember_list_send().get(z.getMember_list_send().size()-1).setMember_id(mess_id);
                                z.getMember_list_send().get(z.getMember_list_send().size()-1).setPort(socket.getPort());
                                z.getMember_list_send().get(z.getMember_list_send().size()-1).setUdp_port(udp_port);
                                
                                new_member_send.setPort(z.getView_number());//vno
                                new_member_send.setUdp_port(z.getTeam_code());//code gor the team
                                z.add_member_send(new_member_send);
                                System.out.println("++Adding "+mess_id+" to " +grpname + " team ");

                                
                                //send messages to all members with the new list
                                Iterator<Member_info> send_to_members_iter = z.getGroup_list().iterator();
                                Member_info send_to_members = new Member_info();
                                while(send_to_members_iter.hasNext()){
                                    send_to_members = send_to_members_iter.next();
                                    System.out.println(">> Sending confirmation for new view of team(after join to : ) "+ send_to_members.getMember_id());
                                    new ObjectOutputStream(send_to_members.getTcp_socket().getOutputStream()).writeObject(z.getMember_list_send());
                                } 

                            } 
                            find_group = 1 ;
                            break ;
                            
                        }
                    }
                    if (find_group == 0){
                        //is the first join on that team so create a new node
                        System.out.println("new member new group address"+ socket.getRemoteSocketAddress());
                        Group_info new_group = new Group_info();
                        Member_info new_member = new Member_info();
                        Member_info_send new_memer_send = new Member_info_send();
                        Member_info_send info_send = new Member_info_send();
                        //increase number of teams
                        number_new_team = number_new_team+1;
                        //member
                        new_member.setAddress((InetSocketAddress)socket.getRemoteSocketAddress());
                        new_member.setMember_id(mess_id);
                        new_member.setPort(socket.getPort());
                        new_member.setUdp_port(udp_port);
                        new_member.setTcp_socket(socket);
                        //member send
                        new_memer_send.setAddress((InetSocketAddress)socket.getRemoteSocketAddress());
                        new_memer_send.setMember_id(mess_id);
                        new_memer_send.setPort(socket.getPort());
                        new_memer_send.setUdp_port(udp_port);
                        //last node for vno and code_team
                        info_send.setPort(0);
                        info_send.setUdp_port(number_new_team);
                    
                        //group
                        new_group.setView_number(0);
                        new_group.setTeam_code(number_new_team);
                        new_group.setName_of_group(grpname);
                        new_group.add_member(new_member);
                        new_group.add_member_send(new_memer_send);
                        new_group.add_member_send(info_send);

                        group_list.add(new_group);
                        System.out.println(new_group.getGroup_list().size());
                        System.out.println(">> Sending confirmation for new team after 1st join ");
                        new ObjectOutputStream(socket.getOutputStream()).writeObject(new_group.getMember_list_send());
        
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    //function for threat that read from multicast socket
    public  void receive_from_multicast(){
        byte []buf;
        char type_message ;

        while(true){
            
            byte[] packet_receive = new byte[1024];
            DatagramPacket receivingPacket = new DatagramPacket(packet_receive,1024);
            try {
                multicast_socket.receive(receivingPacket);
            } catch (IOException e) {
                    e.printStackTrace();
            }
            //read a character
            buf = new byte[1024];
            buf = receivingPacket.getData(); 
            type_message = (char)buf[0];
            if(type_message == 'D'){
                System.out.println("A new messenger discover me. S ending the datat for tcp connection ...");
                //send data for tcp connection to new messenger
                byte[] discover_message = new byte[5];
                discover_message[0] = 'D';
                byte [] sendport = ByteBuffer.allocate(4).putInt(tcp_port).array();
                discover_message[1] = sendport[0];
                discover_message[2] = sendport[1];
                discover_message[3] = sendport[2];
                discover_message[4] = sendport[3];
                DatagramPacket dgram;
                
                try {
                    ServerSocket tcp_socket ;
                    tcp_socket = new ServerSocket();
                   //threads open a tcp connection binds
                    tcp_socket.bind(new InetSocketAddress("10.0.0.1", tcp_port));//put your ip address
                    dgram = new DatagramPacket(discover_message, discover_message.length, receivingPacket.getAddress(), receivingPacket.getPort());
                    //send the port to messenger
                    multicast_socket.send(dgram);
                    tcp_port++;
                    // new thread for a client created
                    try {
                        Socket socket_for_tcp; 
                        socket_for_tcp = tcp_socket.accept();
                        Thread new_thread = new Thread(new Runnable(){public void run() {new Group_manager_api(socket_for_tcp).run();}});
                        new_thread.start();
                    } catch (Exception e) {
                        System.out.println("I/O error: " + e);
                    } 
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
               
            } 
        }
    }
    //init manager function
    public void init_manager(){
        number_new_team =0;//init the number of teams to zero
        NetworkInterface networkInterface;
        MulticastSocket multicast_socket_loc ;
        
        try {
            networkInterface = NetworkInterface.getByName("00:00:00:00:00:01");//give your mac
            multicast_socket_loc = new MulticastSocket(1000);
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("230.0.0.5"), 1000);//set multicast ip
            InetAddress group = InetAddress.getByName("230.0.0.5");
            multicast_socket_loc.joinGroup(addr,  networkInterface);
            Thread thread_1 = new Thread(new Runnable(){public void run() {new Group_manager_api(multicast_socket_loc).receive_from_multicast();}});
            thread_1.start();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       

    }
}
    