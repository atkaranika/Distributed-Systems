import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;
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
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class coordinator {
    ////
    public static  DatagramSocket socket;
    
    public static int serv_counter = 0 ;

    public void read_from_socket() throws FileNotFoundException{
        
        byte []buf;
        char type_message ;

        while(true){
            byte[] packet_receive = new byte[1024];
           
            DatagramPacket receivingPacket = new DatagramPacket(packet_receive,1024);

            try {
                System.out.println("Before receive somethig");
                socket.receive(receivingPacket);
                System.out.println("After receive somethig");

            } catch (IOException e) {
                    e.printStackTrace();
            }
            buf = new byte[1024];
            buf = receivingPacket.getData(); 

            type_message = (char)buf[0];
          
            if(type_message == 'T'){
                
                byte[] team_message = new byte[5];
                team_message[0]= 'T';
                byte [] sendTeam = ByteBuffer.allocate(4).putInt(serv_counter).array();
                team_message[1] = sendTeam[0];
                team_message[2] = sendTeam[1];
                team_message[3] = sendTeam[2];
                team_message[4] = sendTeam[3];
                DatagramPacket dgram;
                try {
                    dgram = new DatagramPacket(team_message, team_message.length, receivingPacket.getAddress(), receivingPacket.getPort());
                    System.out.println("Send to"+receivingPacket.getAddress()+"    "+serv_counter);

                    socket.send(dgram);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            serv_counter++;
        }
       
    }


    public static void main(String[] args) throws Exception {
        socket = new DatagramSocket(2020,InetAddress.getByName("10.0.0.1"));
    
        Thread thread_1 = new Thread(new Runnable(){public void run() {try {
            new coordinator().read_from_socket();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }}});
            thread_1.start();

        
    }

}
