import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class server_api{
    public static DatagramSocket socket_server;
    public Thread thread_read ; 
    public static String current_directory;
    public static List<Server_files> open_files;
    public static int version;
    public static int open_files_no;
    public static int fd = 0;

    public static void init (){
        open_files = new LinkedList<Server_files>();
        try {
            socket_server = new DatagramSocket(4040, InetAddress.getByName("10.0.0.1"));
        } catch (SocketException | UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //scan for root 
        current_directory = System.getProperty("user.dir");
        System.out.println("Current directory : "+current_directory);
        File file = new File("version.txt");
       
        try {
            if (file.createNewFile()) {
                
                System.out.println("File has been created.");
                version = 1;
            } else {
            
                System.out.println("File already exists." );
                try(Scanner scanner = new Scanner(file)){
                    String version_str = scanner.next();

                    version =  Integer.parseInt(version_str) +1;
                }
                
             
            }
            try(Writer wr = new FileWriter("version.txt")){
                wr.write( String.valueOf(version) );

            }
          
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        open_files_no = 0;


    } 

    public void read_from_socket(){
        byte[] packet_receive = new byte[1024];
        DatagramPacket receivingPacket = new DatagramPacket(packet_receive,1024);
        int seqno ;
        int br = 0 ;
        while(true){
           
            try {
                System.out.println("before receive something");
                socket_server.receive(receivingPacket);
                System.out.println("thread just receive something");
            } catch (IOException e) {
                    e.printStackTrace();
            }

            byte [] buf = new byte[1024];
            buf = receivingPacket.getData(); 


            String data = new String(buf,StandardCharsets.UTF_8);
            String type = data.substring(0,1);

            if(type.equals("O")){
                //open case
                System.out.println("mphka sthn open");
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,9)); // big-endian by default
            
                wrapped.rewind();
                int name_length = wrapped.getInt();
                int num_og_flags = wrapped.getInt();

                System.out.println("name_length num_og_flags" +name_length +num_og_flags);
                String filename  = new String(Arrays.copyOfRange(buf, 9, 9+name_length), StandardCharsets.UTF_8);
            
                List<Integer> flags_code = new LinkedList<Integer>();
                ByteBuffer wrapped_flags = ByteBuffer.wrap(Arrays.copyOfRange(buf, 9+name_length,9+name_length+num_og_flags*4));
                for(int i = 0 ; i< num_og_flags; i++){
                    flags_code.add(wrapped_flags.getInt());
                    System.out.println("index " + i + "  flag "+ flags_code.get(i));
                }

                ByteBuffer wrapped_seqno = ByteBuffer.wrap(Arrays.copyOfRange(buf, 9+name_length+num_og_flags*4,9+name_length+num_og_flags*4+4));
                seqno = wrapped_seqno.getInt();
                System.out.println("filename "+ filename+" *******seqno: "+seqno + " thesis from " + 9+name_length+num_og_flags*4 + "  to "+ 9+name_length+num_og_flags*4+4);

                List<String> flags = convert_to_String(flags_code);
                //check if file exist 
                File new_file = new File(current_directory + System.getProperty("file.separator")+filename);
                System.out.println(current_directory + System.getProperty("file.separator")+ filename);
                boolean exists = new_file.exists();
                if(exists){
                    //open the file
                    System.out.println("File exists");
                    if(flags.contains("O_CREAT") && flags.contains("O_EXCL")){
                            //error
                            System.out.println("FAIL TO OPEN FILE");
                            /////////////

                            byte []send_open = new byte[9];
                            ByteBuffer send_open_buffer = ByteBuffer.allocate(9);
                        
                            String type_open  = "O";
                            byte[] type_open_bytes = type_open.getBytes();
                            
        
                            send_open_buffer.putInt(-1);
                            send_open_buffer.putInt(seqno);
        
                            send_open_buffer.rewind();
                            byte [] info = new byte[send_open_buffer.remaining()];
                            send_open_buffer.position(0);
                            send_open_buffer.get(info);
        
                            byte [] send_open_array = new byte[info.length + type_open_bytes.length];
        
                            System.arraycopy(type_open_bytes, 0, send_open_array , 0, type_open_bytes.length );
                            System.arraycopy(info, 0,send_open_array, type_open_bytes.length , info.length );
        
                            DatagramPacket packet = new DatagramPacket(send_open_array, send_open_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                            try {
                                socket_server.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                    }else{
                        //ckeck if already exist in list
                        Iterator<Server_files> iter = open_files.iterator();
                        Server_files current_file ;
                        Server_files existing_file  = null;
                        int already_open = 0;
                        while(iter.hasNext()){
                            current_file = iter.next();
                            if(current_file.getFilename().equals(filename)){
                                existing_file = current_file;
                                already_open = 1;
                                Server_files new_file_node = new Server_files();
                                new_file_node.setFilename(filename);
                            
                                String code_name =current_file.getFilename();
                                new_file_node.setCode_filename(current_file.getCode_filename());
                                new_file_node.setRandom_stream(current_file.getRandom_stream());
                                new_file_node.setFd(current_file.getFd());
                                existing_file = new_file_node;
                                break ;
                            }

                        }
                        if(already_open == 1){
                            System.out.println("already exists file add the client");

                            //add the new client
                            

                        }else{
                            RandomAccessFile random_stream;
                            try {
                                random_stream = new RandomAccessFile(filename, "rw");
                                open_files_no = open_files_no+1;
                                Server_files new_file_node = new Server_files();
                                new_file_node.setFilename(filename);
                            
                                String code_name =Integer.toString(version) + Integer.toString(open_files_no);
                                new_file_node.setCode_filename(Integer.parseInt(code_name));
                                new_file_node.setRandom_stream(random_stream);
                                new_file_node.setFd(fd);
                                fd = fd+1;
                                open_files.add(new_file_node);
                                existing_file = new_file_node;

                               
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            
                        }
                        
                        //send the info
                        byte []send_open = new byte[12];
                        ByteBuffer send_open_buffer = ByteBuffer.allocate(12);
                    
                        String type_open  = "O";
                        byte[] type_open_bytes = type_open.getBytes();
                        

                        send_open_buffer.putInt(existing_file.getFd());
                        
                        send_open_buffer.putInt(existing_file.getCode_filename());

                        send_open_buffer.putInt(seqno);

                        send_open_buffer.rewind();
                        byte [] info = new byte[send_open_buffer.remaining()];
                        send_open_buffer.position(0);
                        send_open_buffer.get(info);

                        System.out.println("send info to client : code_name "+ existing_file.getCode_filename() + " FD "+ existing_file.getFd()+ "  ");

                        byte [] send_open_array = new byte[info.length + type_open_bytes.length];

                        System.arraycopy(type_open_bytes, 0, send_open_array , 0, type_open_bytes.length );
                        System.arraycopy(info, 0,send_open_array, type_open_bytes.length , info.length );

                        DatagramPacket packet = new DatagramPacket(send_open_array, send_open_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                        try {
                            socket_server.send(packet);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
            

                }else{
                    //create the file 
                    System.out.println("File will be created");
                    print_list_string(flags);
                    RandomAccessFile random_stream;
                    try {
                        if(flags.contains("O_CREAT")){
                            random_stream = new RandomAccessFile(filename, "rw");
                            try {
                                random_stream.setLength(0);
                            } catch (IOException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                            open_files_no = open_files_no+1;
                            Server_files new_file_node = new Server_files();
                            new_file_node.setFilename(filename);
                        
                            String code_name =Integer.toString(version) + Integer.toString(open_files_no);
                            new_file_node.setCode_filename(Integer.parseInt(code_name));
                            new_file_node.setRandom_stream(random_stream);
                            open_files.add(new_file_node);
                            new_file_node.setFd(fd);
                            fd++;
                            //send the open info 
                            byte []send_open = new byte[9];
                            ByteBuffer send_open_buffer = ByteBuffer.allocate(12);
                            


                            String type_open  = "O";
                            byte[] type_open_bytes = type_open.getBytes();
                            

                            send_open_buffer.putInt(new_file_node.getFd());
                        
                            send_open_buffer.putInt(new_file_node.getCode_filename());

                            send_open_buffer.putInt(seqno);

                            send_open_buffer.rewind();
                            byte [] info = new byte[send_open_buffer.remaining()];
                            send_open_buffer.position(0);
                            send_open_buffer.get(info);

                            
                        
                            byte [] send_open_array = new byte[info.length + type_open_bytes.length];

                            System.arraycopy(type_open_bytes, 0, send_open_array , 0, type_open_bytes.length );
                            System.arraycopy(info, 0,send_open_array, type_open_bytes.length , info.length );

                            DatagramPacket packet = new DatagramPacket(send_open_array, send_open_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                            try {
                                socket_server.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                    
                        }
                        else{
                            //error
                            System.out.println("FAIL TO OPEN FILE");
                            /////////////

                            byte []send_open = new byte[9];
                            ByteBuffer send_open_buffer = ByteBuffer.allocate(9);

                            String type_open  = "O";
                            byte[] type_open_bytes = type_open.getBytes();
                            

                            send_open_buffer.putInt(-1);
                            send_open_buffer.putInt(seqno);

                            send_open_buffer.rewind();
                            byte [] info = new byte[send_open_buffer.remaining()];
                            send_open_buffer.position(0);
                            send_open_buffer.get(info);

                            byte [] send_open_array = new byte[info.length + type_open_bytes.length];

                            System.arraycopy(type_open_bytes, 0, send_open_array , 0, type_open_bytes.length );
                            System.arraycopy(info, 0,send_open_array, type_open_bytes.length , info.length );

                            DatagramPacket packet = new DatagramPacket(send_open_array, send_open_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                            try {
                                socket_server.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            
                        }
                        
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }

            if(type.equals("R")){
                System.out.println("mphka sthn read");
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,17)); // big-endian by default
            

                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int filecode = wrapped.getInt();
 
                
                int read_pos = wrapped.getInt();
                int read_end_pos = wrapped.getInt();
                int read_seqno = wrapped.getInt();
                System.out.println("filecode " + filecode+ " read_pos "+ read_pos + " read_end_pos "+ read_end_pos + " seqno "+ read_seqno);

                Iterator <Server_files> iter= open_files.iterator();
                Server_files file;
                int curr_version = 0;
                while(iter.hasNext()){
                    file = iter.next();
                    if (file.getCode_filename() == filecode){
                        curr_version = 1;
                        byte [] read_array = new byte[read_end_pos];
                        System.out.println(" vrika"+ filecode );
                       
                        try {
                            file.random_stream.seek(read_pos);
                            //check if file descriptor is out of bounds of file size
                            int read_bytes = file.random_stream.read(read_array, 0 , read_end_pos);

                            if(read_pos >= file.random_stream.length()){
                                System.out.println(" read_pos " + read_pos + " file.random_stream.length() " + file.random_stream.length());
                                //return 0 bytes as readden
                                byte []send_read = new byte[12];
                                ByteBuffer send_read_buffer = ByteBuffer.allocate(12);
    
                                String type_read  = "R";
                                byte[] type_read_bytes = type_read.getBytes();
                                
                                send_read_buffer.putInt(read_seqno);
                                send_read_buffer.putInt(0);
                                send_read_buffer.putInt(file.gettMod());
                                
    
                                send_read_buffer.rewind();
                                byte [] info = new byte[send_read_buffer.remaining()];
                                send_read_buffer.position(0);
                                send_read_buffer.get(info);
    
                                byte [] send_read_array = new byte[info.length + type_read_bytes.length];
    
                                System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                                System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );
    
                                DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                                try {
                                    socket_server.send(packet);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                break ;


                            }else{
                                if(read_bytes == -1){
                                    //fail to readd 
                                    System.out.println("FAIL TO READ FILE");
                                    /////////////
        
                                    byte []send_read = new byte[8];
                                    ByteBuffer send_read_buffer = ByteBuffer.allocate(8);
        
                                    String type_read  = "R";
                                    byte[] type_read_bytes = type_read.getBytes();
                                    
                                    send_read_buffer.putInt(read_seqno);
                                    send_read_buffer.putInt(-1);
                                    
        
                                    send_read_buffer.rewind();
                                    byte [] info = new byte[send_read_buffer.remaining()];
                                    send_read_buffer.position(0);
                                    send_read_buffer.get(info);
        
                                    byte [] send_read_array = new byte[info.length + type_read_bytes.length];
        
                                    System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                                    System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );
        
                                    DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                                    try {
                                        socket_server.send(packet);
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    break;


                                }
                            }
                            System.out.println("bytes readen : "+read_bytes);
                            System.out.println(" array" + new String(read_array, StandardCharsets.UTF_8));
                            byte[] readen_array  = new byte[read_bytes];
                            System.arraycopy(read_array, 0, readen_array, 0, read_bytes );
                            String type_read  = "R";
                            byte[] type_read_bytes = type_read.getBytes();
                        
                            ByteBuffer send_read_data = ByteBuffer.allocate(16);
                            send_read_data.putInt(read_seqno).putInt(filecode).putInt(read_bytes).putInt( file.gettMod());
                        
                    
                            send_read_data.rewind();
                            byte [] read_data_bytes = new byte[send_read_data.remaining()];
                            send_read_data.position(0);
                            send_read_data.get(read_data_bytes);
                    
                            byte[] read_send_bytes = new byte[17 + read_bytes];
                               
                            System.arraycopy(type_read_bytes, 0, read_send_bytes, 0, type_read_bytes.length );
                            System.arraycopy(read_data_bytes, 0, read_send_bytes, type_read_bytes.length , read_data_bytes.length);
                            System.arraycopy(readen_array, 0, read_send_bytes, type_read_bytes.length +  read_data_bytes.length, readen_array.length );

                            System.out.println("Send the read packet to server SIZE "+ read_send_bytes.length);

                            DatagramPacket packet = new DatagramPacket(read_send_bytes, read_send_bytes.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                            try {
                                System.out.println("Send the read packet to server ");
                                socket_server.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }


                    }
                }
                if(curr_version  == 0 ){
                    //return 0 bytes as readden
                    byte []send_read = new byte[12];
                    ByteBuffer send_read_buffer = ByteBuffer.allocate(12);

                    String type_read  = "R";
                    byte[] type_read_bytes = type_read.getBytes();
                    
                    send_read_buffer.putInt(read_seqno);
                 
                    send_read_buffer.putInt(-3);
                    send_read_buffer.putInt(filecode);
                    

                    send_read_buffer.rewind();
                    byte [] info = new byte[send_read_buffer.remaining()];
                    send_read_buffer.position(0);
                    send_read_buffer.get(info);

                    byte [] send_read_array = new byte[info.length + type_read_bytes.length];

                    System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                    System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );

                    DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                    try {
                        socket_server.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;


                }
            }else if (type.equals("S")){

                System.out.println("mphka sthn seek");
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,9)); // big-endian by default
            
                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int seek_seqno = wrapped.getInt();
                int filecode = wrapped.getInt();
               
                Iterator <Server_files> iter_s= open_files.iterator();
                Server_files file_s;
                int curr_version = 0;
                while(iter_s.hasNext()){
                    file_s = iter_s.next();
                    if (file_s.getCode_filename() == filecode){
                        curr_version = 1;
                        long file_length;
                        try {
                            file_length = file_s.getRandom_stream().length();
                        byte []send_read = new byte[9];
                        ByteBuffer send_seek_buffer = ByteBuffer.allocate(9);

                        String type_seek  = "S";
                        byte[] type_seek_bytes = type_seek.getBytes();
                        
                        send_seek_buffer.putInt(seek_seqno);
                        send_seek_buffer.putInt((int)file_length);
                        

                        send_seek_buffer.rewind();
                        byte [] info = new byte[send_seek_buffer.remaining()];
                        send_seek_buffer.position(0);
                        send_seek_buffer.get(info);

                        byte [] send_seek_array = new byte[info.length + type_seek_bytes.length];

                        System.arraycopy(type_seek_bytes, 0, send_seek_array , 0, type_seek_bytes.length );
                        System.arraycopy(info, 0,send_seek_array, type_seek_bytes.length , info.length );

                        DatagramPacket packet = new DatagramPacket(send_seek_array, send_seek_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                        try {
                            socket_server.send(packet);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    }
                   
                }

                if (curr_version == 0){
                    byte []send_read = new byte[13];
                    ByteBuffer send_seek_buffer = ByteBuffer.allocate(13);
                    
                    String type_seek  = "S";
                    byte[] type_seek_bytes = type_seek.getBytes();
                    
                    send_seek_buffer.putInt(seek_seqno);
                    send_seek_buffer.putInt(-3);
                    send_seek_buffer.putInt(filecode);
                    

                    send_seek_buffer.rewind();
                    byte [] info = new byte[send_seek_buffer.remaining()];
                    send_seek_buffer.position(0);
                    send_seek_buffer.get(info);

                    byte [] send_seek_array = new byte[info.length + type_seek_bytes.length];

                    System.arraycopy(type_seek_bytes, 0, send_seek_array , 0, type_seek_bytes.length );
                    System.arraycopy(info, 0,send_seek_array, type_seek_bytes.length , info.length );

                    DatagramPacket packet = new DatagramPacket(send_seek_array, send_seek_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                    try {
                        socket_server.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

               
            }else if(type.equals("W")){
                int curr_version =0;
                System.out.println("mphka sthn WRITE");
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,17)); // big-endian by default
            
                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int filecode = wrapped.getInt();
                int write_pos = wrapped.getInt();
                int data_size = wrapped.getInt();
                int write_seqno = wrapped.getInt();
                ByteBuffer wrapped_data = ByteBuffer.wrap(Arrays.copyOfRange(buf, 17,17+data_size));

                Iterator <Server_files> iter= open_files.iterator();
                Server_files file;
                while(iter.hasNext()){
                    file = iter.next();
                    if (file.getCode_filename() == filecode){
                        curr_version =1;
        
                        wrapped_data.rewind();
                        byte [] data_to_write = new byte[wrapped_data.remaining()];
                        wrapped_data.position(0);
                        wrapped_data.get(data_to_write);
                        System.out.println(" vrika"+ filecode );
                       
                        try {
                            
                            if(write_pos + data_size > file.random_stream.length()){

                                long old_length = file.random_stream.length();
                                long dif = write_pos - file.random_stream.length();
                                System.out.println("extend dif "+ dif);
                                file.random_stream.seek(file.random_stream.length());

                                System.out.println("Size  prin "+ file.random_stream.length());
                                file.random_stream.setLength(write_pos + data_size);
                                System.out.println("Size meta "+ file.random_stream.length());
                                for ( int i = 0 ; i < dif ; i++){
                                    file.random_stream.write("\0".getBytes(),0, 1);
                                }
                            }
                            System.out.println("write pos "+ write_pos + " data size " + data_size);
                            file.random_stream.seek(write_pos);
                            //-------------------------
                            file.settMod(file.gettMod()+1);
                            //-------------------------
                            file.random_stream.write(data_to_write, 0, data_size);
              
                            System.out.println("bytes written : "+data_size);
                            
                            String type_write  = "W";
                            byte[] type_write_bytes = type_write.getBytes();
                        
                            ByteBuffer send_write_data = ByteBuffer.allocate(12);
                            send_write_data.putInt(write_seqno).putInt(filecode).putInt(data_size);
                        
                    
                            send_write_data.rewind();
                            byte [] write_data_bytes = new byte[send_write_data.remaining()];
                            send_write_data.position(0);
                            send_write_data.get(write_data_bytes);
                    
                            byte[] write_send_bytes = new byte[13];
                               
                            System.arraycopy(type_write_bytes, 0, write_send_bytes, 0, type_write_bytes.length );
                            System.arraycopy(write_data_bytes, 0, write_send_bytes, type_write_bytes.length , write_data_bytes.length);

                            DatagramPacket packet = new DatagramPacket(write_send_bytes, write_send_bytes.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                            try {
                                System.out.println("Send the read packet to server ");
                                socket_server.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }
                if(curr_version  == 0 ){
                    //return 0 bytes as readden
                    byte []send_read = new byte[12];
                    ByteBuffer send_read_buffer = ByteBuffer.allocate(12);

                    String type_read  = "W";
                    byte[] type_read_bytes = type_read.getBytes();
                    
                    send_read_buffer.putInt(write_seqno);
                 
                    send_read_buffer.putInt(-3);
                    send_read_buffer.putInt(filecode);
                    

                    send_read_buffer.rewind();
                    byte [] info = new byte[send_read_buffer.remaining()];
                    send_read_buffer.position(0);
                    send_read_buffer.get(info);

                    byte [] send_read_array = new byte[info.length + type_read_bytes.length];

                    System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                    System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );

                    DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                    try {
                        socket_server.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;


                }
            }
            else if (type.equals("T")){
                int curr_version =0;

                System.out.println("mphka sthn TRUNC");
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,13)); // big-endian by default
            
                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int filecode = wrapped.getInt();
                int length = wrapped.getInt();
                int trunc_seqno = wrapped.getInt();
                Iterator <Server_files> iter= open_files.iterator();
                Server_files file;
                while(iter.hasNext()){
                    file = iter.next();
                    if (file.getCode_filename() == filecode){
                        curr_version = 1;
                        try {
                            file.random_stream.setLength(length);
                            file.settMod(file.gettMod()+1);
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                       
                        String type_trunc  = "T";
                        byte[] type_trunc_bytes = type_trunc.getBytes();
                    
                        ByteBuffer send_trunc_data = ByteBuffer.allocate(8);
                        send_trunc_data.putInt(trunc_seqno).putInt(0);
                    
                
                        send_trunc_data.rewind();
                        byte [] trunc_data_bytes = new byte[send_trunc_data.remaining()];
                        send_trunc_data.position(0);
                        send_trunc_data.get(trunc_data_bytes);
                
                        byte[] trunc_send_bytes = new byte[9];
                           
                        System.arraycopy(type_trunc_bytes, 0, trunc_send_bytes, 0, type_trunc_bytes.length);
                        System.arraycopy(trunc_data_bytes, 0, trunc_send_bytes, type_trunc_bytes.length , trunc_data_bytes.length);

                        DatagramPacket packet = new DatagramPacket(trunc_send_bytes, trunc_send_bytes.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                        try {
                            System.out.println("Send the trunc packet to server ");
                            socket_server.send(packet);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
 
                }
                if(curr_version  == 0 ){
                    //return 0 bytes as readden
                    String type_trunc  = "T";
                    byte[] type_trunc_bytes = type_trunc.getBytes();
                
                    ByteBuffer send_trunc_data = ByteBuffer.allocate(12);
                    send_trunc_data.putInt(trunc_seqno).putInt(-3).putInt(filecode);
                
            
                    send_trunc_data.rewind();
                    byte [] trunc_data_bytes = new byte[send_trunc_data.remaining()];
                    send_trunc_data.position(0);
                    send_trunc_data.get(trunc_data_bytes);
            
                    byte[] trunc_send_bytes = new byte[13];
                        
                    System.arraycopy(type_trunc_bytes, 0, trunc_send_bytes, 0, type_trunc_bytes.length);
                    System.arraycopy(trunc_data_bytes, 0, trunc_send_bytes, type_trunc_bytes.length , trunc_data_bytes.length);

                    DatagramPacket packet = new DatagramPacket(trunc_send_bytes, trunc_send_bytes.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                    try {
                        System.out.println("Send the trunc -3 packet to client ");
                        socket_server.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    }
                    continue;


            }
            else if (type.equals("M")){
                int curr_version = 0;
                ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,21)); // big-endian by default
            
                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int filecode = wrapped.getInt();
                int read_pos = wrapped.getInt();
                int read_end_pos = wrapped.getInt();
                int read_tmod = wrapped.getInt();
                int read_seqno = wrapped.getInt();
                System.out.println("--M-- filecode "+ filecode+ " read_pos " + read_pos + " read_end_pos " + read_end_pos + " read_tmod "+ read_tmod + " read_seqno " + read_seqno);
                
                Iterator <Server_files> iter= open_files.iterator();
                Server_files file;
                while(iter.hasNext()){
                    file = iter.next();
                    if (file.getCode_filename() == filecode){
                        curr_version = 1;
                        if(file.gettMod() == read_tmod){
                            byte []send_read = new byte[12];
                            ByteBuffer send_read_buffer = ByteBuffer.allocate(16);

                            String type_read  = "M";
                            byte[] type_read_bytes = type_read.getBytes();
                            send_read_buffer.putInt(0).putInt(-1);
                            send_read_buffer.putInt(read_seqno);
                            send_read_buffer.putInt(filecode);
                         
                            send_read_buffer.rewind();
                            byte [] info = new byte[send_read_buffer.remaining()];
                            send_read_buffer.position(0);
                            send_read_buffer.get(info);
        
                            byte [] send_read_array = new byte[info.length + type_read_bytes.length];
        
                            System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                            System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );
        
                            DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                            try {
                                System.out.println("Send mod packet to client swsto tmod ");
                                socket_server.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        
                        }
                        else {
                            byte [] read_array = new byte[read_end_pos];
                        
                           
                            try {
                                file.random_stream.seek(read_pos);
                                //check if file descriptor is out of bounds of file size
                                int read_bytes = file.random_stream.read(read_array, 0 , read_end_pos);
    
                                if(read_pos >= file.random_stream.length()){
                                   
                                    //return 0 bytes as readden
                                    byte []send_read = new byte[16];
                                    ByteBuffer send_read_buffer = ByteBuffer.allocate(16);
        
                                    String type_read  = "M";
                                    byte[] type_read_bytes = type_read.getBytes();
                                    
                                    send_read_buffer.putInt(file.gettMod());
                                    send_read_buffer.putInt(0);
                                    send_read_buffer.putInt(read_seqno);
                                    send_read_buffer.putInt(filecode);
        
                                    send_read_buffer.rewind();
                                    byte [] info = new byte[send_read_buffer.remaining()];
                                    send_read_buffer.position(0);
                                    send_read_buffer.get(info);
        
                                    byte [] send_read_array = new byte[info.length + type_read_bytes.length];
        
                                    System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                                    System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );
        
                                    DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                                    try {
                                        socket_server.send(packet);
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    break;
    
    
                                }else{
                                    if(read_bytes == -1){
                                        //fail to readd 
                                        System.out.println("FAIL TO READ FILE");
                                        /////////////
            
                                        byte []send_read = new byte[16];
                                        ByteBuffer send_read_buffer = ByteBuffer.allocate(16);
            
                                        String type_read  = "M";
                                        byte[] type_read_bytes = type_read.getBytes();
                                        send_read_buffer.putInt(-1); 
                                        send_read_buffer.putInt(-1);
                                        send_read_buffer.putInt(read_seqno);
                                        send_read_buffer.putInt(filecode);
                                        send_read_buffer.rewind();
                                        byte [] info = new byte[send_read_buffer.remaining()];
                                        send_read_buffer.position(0);
                                        send_read_buffer.get(info);
            
                                        byte [] send_read_array = new byte[info.length + type_read_bytes.length];
            
                                        System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                                        System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );
            
                                        DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                                        try {
                                            socket_server.send(packet);
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                        break;
    
    
                                    }
                                }
                                System.out.println("neo tmod stelnw ston client "+ file.gettMod());
                                System.out.println("bytes readen : "+read_bytes);
                                System.out.println(" array" + new String(read_array, StandardCharsets.UTF_8));
                                byte[] readen_array  = new byte[read_bytes];
                                System.arraycopy(read_array, 0, readen_array, 0, read_bytes );
                                String type_read  = "M";
                                byte[] type_read_bytes = type_read.getBytes();
                            
                                ByteBuffer send_read_data = ByteBuffer.allocate(16);
                                send_read_data.putInt(file.gettMod()).putInt(read_bytes).putInt(read_seqno).putInt(filecode);
                            
                        
                                send_read_data.rewind();
                                byte [] read_data_bytes = new byte[send_read_data.remaining()];
                                send_read_data.position(0);
                                send_read_data.get(read_data_bytes);
                                
                                byte[] read_send_bytes = new byte[17 + read_bytes];
                                   
                                System.arraycopy(type_read_bytes, 0, read_send_bytes, 0, type_read_bytes.length );
                                System.arraycopy(read_data_bytes, 0, read_send_bytes, type_read_bytes.length , read_data_bytes.length);
                                System.arraycopy(readen_array, 0, read_send_bytes, type_read_bytes.length +  read_data_bytes.length, readen_array.length );
    
                                DatagramPacket packet = new DatagramPacket(read_send_bytes, read_send_bytes.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                                try {
                                    System.out.println("Send the MOD packet to client ");
                                    socket_server.send(packet);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
    
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
    
                        

                        }
                    }

                }
                if(curr_version  == 0 ){
                    //return 0 bytes as readden
                    byte []send_read = new byte[12];
                    ByteBuffer send_read_buffer = ByteBuffer.allocate(16);

                    String type_read  = "M";
                    byte[] type_read_bytes = type_read.getBytes();
                    send_read_buffer.putInt(-3);
                    send_read_buffer.putInt(-3);
                    send_read_buffer.putInt(read_seqno);
                 
                    
                    send_read_buffer.putInt(filecode);
                    

                    send_read_buffer.rewind();
                    byte [] info = new byte[send_read_buffer.remaining()];
                    send_read_buffer.position(0);
                    send_read_buffer.get(info);

                    byte [] send_read_array = new byte[info.length + type_read_bytes.length];

                    System.arraycopy(type_read_bytes, 0, send_read_array , 0, type_read_bytes.length );
                    System.arraycopy(info, 0,send_read_array, type_read_bytes.length , info.length );

                    DatagramPacket packet = new DatagramPacket(send_read_array, send_read_array.length ,receivingPacket.getAddress() , receivingPacket.getPort());
                    try {
                        socket_server.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;


                }
            
            }
         }
    }
    
    public void print_list_string(List <String> list){
        Iterator <String> iter = list.iterator();
        String current ;
        while(iter.hasNext()){
            current = iter.next();
            System.out.println(current);
        }
    }
    
    public List<String> convert_to_String(List <Integer>list){

        List <String>ret_list = new LinkedList<String>();
        Iterator <Integer> iter = list.iterator();
        int z ;
        while (iter.hasNext()){
            String flag ;
            z = iter.next();

            if(z == 1){
                ret_list.add("O_CREAT");
                continue ;
            }

            if(z == 2 )
            {
                ret_list.add("O_EXCL");
                continue ;
            }

            if(z==3){
                ret_list.add("O_TRUNC");
                continue ;
            }

            if(z == 4){
                ret_list.add("O_RDWR");
                continue ;
            }

            if(z == 5){
                ret_list.add("O_RDONLY");
                continue ;
            }

            if(z == 6)
            {
                ret_list.add("O_WRONLY");
                continue ;
            }

            
        }
        return ret_list ;
    }

    public static void main(String[] args) throws Exception {
        init();
        Thread thread_1 = new Thread(new Runnable(){public void run() {new server_api().read_from_socket();}});
            thread_1.start();

        
    }
}