import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class client_api{
    //stoixeia gia server
    public static  DatagramSocket server_socket;
    public static  InetSocketAddress server_address;
    public Thread thread_read ; 
    public int seqno;
    public static LinkedList <hidden_memory> hidden_memory_list = new LinkedList<hidden_memory>();

    
    public static List<client_files>client_files_list ;
    public static List<read_data> read_requests ;
    public static List<seek_data> seek_requests;
    public static List<write_data> write_requests;
    public static List<trunc_data> trunc_requests;
    public static Lock lock_clientf = new Lock();
    public static Lock lock_req = new Lock();
    public static Lock lock_seqno = new Lock();
    public static Lock lock_seek = new Lock();
    public static Lock lock_write = new Lock();
    public static Lock lock_trunc = new Lock();
    public static Lock lock_fd = new Lock();
    public static Lock hm_lock = new Lock();
    public int fd_local = 0;
    
    public static int cacheblocks;
    public static int blocksize;
    public static int freshT;
    public void  read_from_socket(){
        int br = 0 ;
        
        while(true){
            if(br ==1){
                break ;
            }
            byte[] packet_receive = new byte[1024];
            DatagramPacket receivingPacket = new DatagramPacket(packet_receive,1024);
            System.out.println("before receive something");
            try {
                server_socket.receive(receivingPacket);
                System.out.println("thread just receive something");
            } catch (IOException e) {
                    e.printStackTrace();
    
            }

            byte [] buf = new byte[1024];
            buf = receivingPacket.getData(); 

            String data = new String(buf,StandardCharsets.UTF_8);
            String type = data.substring(0,1);

            ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(buf, 1,17)); // big-endian by default

            
            wrapped.rewind();
            int fd = wrapped.getInt();
            System.out.println("fd "+ fd);
           
            if(type.equals("O")){
                int file_code = 0 ;
                if(fd != -1){
                    file_code = wrapped.getInt();
                }
                   
                int seqno_file = wrapped.getInt();
                System.out.println("file_code"+ file_code+ "seqno"+ seqno_file);
                try {
                    this.lock_clientf.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Iterator <client_files>iter = client_files_list.iterator();
                client_files z ;
                while(iter.hasNext()){
                    z = iter.next();
                    if(z.getSeqno() == seqno_file){
                        System.out.println("mpika " + seqno_file);
                        if(z.getFd()<0){
                            try {
                                this.lock_fd.lock();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            fd_local++;
                            z.setFd(fd_local);
                        
                            this.lock_fd.unlock();
                            z.setFilecode(file_code);
                            
                        }  
                        print_list_client_files(client_files_list);
                        break;
            
                    }
                }
                this.lock_clientf.unlock();
                
            }
            if(type.equals("R")){
            
               
                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int read_seqno = wrapped.getInt();
                int check  = wrapped.getInt();
                System.out.println("thread api check " + check +" read_seqno "+ read_seqno);
                if(check == -1){
                   
                    int size = -1;
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == read_seqno){
                            z.messages++;
                            z.setSize(size);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();

                }else if (check == 0){
                    int size = 0;
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                   
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == read_seqno){
                            z.messages++;
                            z.setSize(size);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    //change data in hidden memory
                    Iterator<hidden_memory> hm_iter = hidden_memory_list.iterator();
                    hidden_memory  hm = new hidden_memory();
                    while(hm_iter.hasNext()){
                        hm = hm_iter.next();
                        if(hm.getSeqno() == read_seqno){
                            hm_iter.remove();
                            break;
                        }
                    }
                    this.hm_lock.unlock();
                }else if(check == -3){
                    System.out.println("PHRA -3  read_seqno " + read_seqno);
                    int size = -3;

                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == read_seqno){
                            z.messages++;
                            ByteBuffer codename_buf = ByteBuffer.wrap(Arrays.copyOfRange(buf, 9,13));
                            z.setData(codename_buf);
                            z.setSize(size);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                    //remove from hidden memory

                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    //change data in hidden memory
                    Iterator<hidden_memory> hm_iter = hidden_memory_list.iterator();
                    hidden_memory  hm = new hidden_memory();
                    while(hm_iter.hasNext()){
                        hm = hm_iter.next();
                        if(hm.getSeqno() == read_seqno){
                            hm_iter.remove();
                            break;
                        }
                    }
                    this.hm_lock.unlock();

                }
                else{
                    int filecode = check;
                    int size = wrapped.getInt();
                    int tmod = wrapped.getInt();
                    String dataa = new String(Arrays.copyOfRange(buf,17, buf.length));

                   System.out.println("thread api data "+ dataa +  dataa.length());
                    ByteBuffer wrapped_data = ByteBuffer.wrap(Arrays.copyOfRange(buf,17, 17+size));
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                    z = iter.next();
                        if(z.getSeqno() == read_seqno){
                            z.messages++;
                            z.setData(wrapped_data);
                            z.setSize(size);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    //change data in hidden memory
                    Iterator<hidden_memory> hm_iter = hidden_memory_list.iterator();
                    hidden_memory  hm = new hidden_memory();
                    while(hm_iter.hasNext()){
                        hm = hm_iter.next();
                        if(hm.getSeqno() == read_seqno){
                            hm.setTmod(tmod);
                            hm.setBlock_data(wrapped_data);
                            hm.setFreshness(System.currentTimeMillis());
                            break;
                        }
                    }
                    this.hm_lock.unlock();
                }

            }else if (type.equals("S")){
                wrapped.rewind();
                System.out.println(wrapped.remaining());
                int seek_seqno = wrapped.getInt();
                int length  = wrapped.getInt();
          
                try {
                    this.lock_seek.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Iterator <seek_data>iter_s = seek_requests.iterator();
                seek_data z ;
                while(iter_s.hasNext()){
                z = iter_s.next();
                    if(z.getSeqno() == seek_seqno){
                       z.setFile_length(length);
                       if(length == -3){
                           z.setFileCode(wrapped.getInt());
                           z.messages++;
                       }
                       break;
            
                    }
                }
                this.lock_seek.unlock();
                

                
            }else if(type.equals("W")){
                wrapped.rewind();
                int write_seqno = wrapped.getInt();
                int check  = wrapped.getInt();
                if(check == -3){
                    System.out.println("PHRA -3");
                    int size = -3;
                    Iterator <write_data>iter = write_requests.iterator();
                    write_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == write_seqno){
                            z.setCodename(wrapped.getInt());
                            z.setSize_written(size);
                            z.messages++;
                            break;
                
                        }
                    }

                }
                else{
                    int codename = check;
                    int length  = wrapped.getInt();
            
                    try {
                        this.lock_write.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <write_data>iter_w = write_requests.iterator();
                    write_data z ;
                    while(iter_w.hasNext()){
                    z = iter_w.next();
                        if(z.getSeqno() == write_seqno){
                        z.setSize_written(length);
                        break;
                
                        }
                    }
                    this.lock_write.unlock();
            }
            }else if(type.equals("T")){
                wrapped.rewind();
                int trunc_seqno = wrapped.getInt();
                int return_trunc  = wrapped.getInt();
                
          
                try {
                    this.lock_trunc.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

             
                Iterator <trunc_data>iter_t = trunc_requests.iterator();
                trunc_data z ;
                while(iter_t.hasNext()){
                z = iter_t.next();
                    if(z.getSeqno() == trunc_seqno){
                       z.setReturn_value(return_trunc);
                       if(return_trunc == -3){
                            z.setCodename(wrapped.getInt());
                            z.messages++;
                       }
                       
                       break;
                    }
                }
                this.lock_trunc.unlock();
            }else if(type.equals("M")){

///////////////////////////////////////////////////////////////////////////////////////////
                wrapped.rewind();
                int tmod = wrapped.getInt();
                int bytes_readden  = wrapped.getInt();
                int mod_seqno  = wrapped.getInt();
                int fileCode = wrapped.getInt();
                if(tmod == -1){
                    System.out.println("thread: tmod -1 apotixia read");
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    int size = -1;
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == mod_seqno){
                            z.messages++;
                            z.setSize(size);
                            break;
                
                        }
                    }
                this.lock_req.unlock();
                }else if (tmod == 0 && bytes_readden == -1){
                    
                    //tmod is the same
                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    //change data in hidden memory
                    Iterator<hidden_memory> hm_iter = hidden_memory_list.iterator();
                    hidden_memory  hm = new hidden_memory();
                    ByteBuffer memory_data = null;
                    while(hm_iter.hasNext()){
                        hm = hm_iter.next();
                        if(hm.getSeqno() == mod_seqno){
                            hm.setFreshness(System.currentTimeMillis());
                           hm.setCodename(fileCode);
                            memory_data =  ByteBuffer.allocate( hm.getBlock_data().position(0).remaining());
                            for(int m=0; m< hm.getBlock_data().position(0).remaining(); m++){
                                memory_data.put(hm.getBlock_data().get(m));
                            }
                            break;
                        }
                    }
                    this.hm_lock.unlock();

                    int size = memory_data.position(0).remaining();
                    System.out.println("thread: elegksa gia tmod kai einai swsto " + " size " + memory_data.position(0).remaining());
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                  //  System.out.println("mod_seqno " + mod_seqno+ " size " + size+"gia na ksekolhsei o allos");
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == mod_seqno){
                            z.messages++;
                            z.setSize(size);
                            z.setData(memory_data);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                }else if(tmod == -3){
                    System.out.println("PHRA -3");
                    int size = -3;
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == mod_seqno){
                            z.messages++;
                            ByteBuffer codename_buf = ByteBuffer.wrap(Arrays.copyOfRange(buf, 13,17));
                            z.setData(codename_buf);
                            z.setSize(size);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                }else if(bytes_readden == 0){

                    int size = 0;
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                        z = iter.next();
                        if(z.getSeqno() == mod_seqno){
                            z.messages++;
                            z.setSize(size);
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    //change data in hidden memory
                    Iterator<hidden_memory> hm_iter = hidden_memory_list.iterator();
                    hidden_memory  hm = new hidden_memory();
                    while(hm_iter.hasNext()){
                        hm = hm_iter.next();
                        if(hm.getSeqno() == mod_seqno){
                            hm_iter.remove();
                            break;
                        }
                    }
                    this.hm_lock.unlock();


                }
                else{
                   
                    String dataa = new String(Arrays.copyOfRange(buf,17, buf.length));

                   System.out.println("thread api data "+ dataa);
                    ByteBuffer wrapped_data = ByteBuffer.wrap(Arrays.copyOfRange(buf,17, buf.length));
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Iterator <read_data>iter = read_requests.iterator();
                    read_data z ;
                    while(iter.hasNext()){
                    z = iter.next();
                        if(z.getSeqno() == mod_seqno){
                            z.messages++;
                            z.setData(wrapped_data);
                            z.setSize(wrapped_data.position(0).remaining());
                            break;
                
                        }
                    }
                    this.lock_req.unlock();
                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    //change data in hidden memory
                    Iterator<hidden_memory> hm_iter = hidden_memory_list.iterator();
                    hidden_memory  hm = new hidden_memory();
                    while(hm_iter.hasNext()){
                        hm = hm_iter.next();
                        if(hm.getSeqno() == mod_seqno){
                            hm.setTmod(tmod);
                            hm.setBlock_data(wrapped_data);
                            hm.setFreshness(System.currentTimeMillis());
                            break;
                        }
                    }
                    this.hm_lock.unlock();
                }


            }

        }
     
    }
    
    public int mynfs_init(String address , int port, int cacheblocks, int blocksize, int freshT){
        try {
            server_socket = new DatagramSocket();
            server_address = new InetSocketAddress(InetAddress.getByName(address), port);
            client_files_list = new LinkedList<client_files>();
            read_requests = new LinkedList<read_data>();
            seek_requests = new LinkedList<seek_data>();
            write_requests = new LinkedList<write_data>();
            trunc_requests = new LinkedList<trunc_data>();
            //hidden_memory_list = new LinkedList<>();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.cacheblocks = cacheblocks;
        this.blocksize = blocksize;
        this.freshT = freshT;
        Thread thread_1 = new Thread(new Runnable(){public void run() {new client_api().read_from_socket();}});
        thread_1.start();

        return 0 ;
    }

    //open
    public int  mynfs_open(String filename,  List <String>flags, num_messages counter_open){
        try {
            this.lock_seqno.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        int open_seqno = seqno;
        seqno++;
        this.lock_seqno.unlock();
        int readcount = 0 ;
        //1 byte for character 0 , 4 bytes for filename ,  4 bytes for number of flags
        byte[] send_open = new byte[13+ flags.size() * 4  + filename.length()];
       
    
        String type_open  = "O";
        byte[] type_open_bytes = type_open.getBytes();
        ByteBuffer send_open_lengths = ByteBuffer.allocate(8);
        send_open_lengths.putInt(filename.length()).putInt(flags.size());
    
        send_open_lengths.rewind();
        byte [] lengths_byte = new byte[send_open_lengths.remaining()];
        send_open_lengths.position(0);
        send_open_lengths.get(lengths_byte);

        byte[] filename_bytes = filename.getBytes();
       
        System.out.print("List of  flags: ");
        print_list_string(flags);
        System.out.println();
        List <Integer> flags_list = convert_to_int(flags);
        System.out.print("List of numbered flags: ");
        print_list_int(flags_list);
        if(flags_list.contains(7)){
            System.out.println("\n You gave wrong flag");
            return (-1);
        }
        
        System.out.println();
        Iterator <Integer>iter = flags_list.iterator();
        int flag ;
        int counter = 0 ;
        client_files new_file = new client_files(); 

        new_file.setSeqno(open_seqno);
        new_file.setFilename(filename);
        new_file.setFlags(flags);

        print_list_string(flags);
        
        new_file.setFd(-10);
        
        try {
            this.lock_clientf.lock();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        client_files_list.add(new_file);
        this.lock_clientf.unlock();
        ByteBuffer flags_buf = ByteBuffer.allocate(4*flags_list.size());
     
        while (iter.hasNext()){
            counter++;
            flag = iter.next();
            flags_buf.putInt(flag);
        }

        flags_buf.rewind();
        byte [] flags_byte = new byte[flags_buf.remaining()];
        flags_buf.position(0);
        flags_buf.get(flags_byte);

    
        ByteBuffer seqno_buf = ByteBuffer.allocate(4);
        seqno_buf.putInt(open_seqno);
        seqno_buf.rewind();

       
        byte [] seqno_byte = new byte[seqno_buf.remaining()];
        seqno_buf.position(0);
        seqno_buf.get(seqno_byte);


       
        byte[] open_send_bytes = new byte[type_open_bytes.length + lengths_byte.length+filename_bytes.length+flags_byte.length+ seqno_byte.length];
           
        System.arraycopy(type_open_bytes, 0, open_send_bytes, 0, type_open_bytes.length );
        System.arraycopy(lengths_byte, 0, open_send_bytes, type_open_bytes.length , lengths_byte.length  );
        System.arraycopy(filename_bytes , 0, open_send_bytes, lengths_byte.length + type_open_bytes.length , filename_bytes.length);
        System.arraycopy(flags_byte , 0, open_send_bytes,filename_bytes.length + lengths_byte.length + type_open_bytes.length, flags_byte.length);
        System.arraycopy(seqno_byte , 0, open_send_bytes,filename_bytes.length + lengths_byte.length + type_open_bytes.length+flags_byte.length ,seqno_byte.length);        
        DatagramPacket packet = new DatagramPacket(open_send_bytes, open_send_bytes.length ,server_address.getAddress() , server_address.getPort());
        try {
            System.out.println("Send the open packet to server with seqno: "+ open_seqno);
            if(counter_open != null)
                counter_open.counter++ ;
            server_socket.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int reopen = 0;
        while(true){
            try {
                this.lock_clientf.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            reopen++;
            Iterator<client_files> iter_fd = client_files_list.iterator();
            client_files z ;
            while(iter_fd.hasNext()){
                z = iter_fd.next();
                if(z.getSeqno() == open_seqno){
                    if(z.getFd() == -1){
                        this.lock_clientf.unlock();
                        iter_fd.remove();
                        return(-1);
                    }
                    if(z.getFd()>-2){
                        int return_fd  =z.getFd();
                        z.setCurr_pos(0);
                        this.lock_clientf.unlock();
                        return(return_fd);
                    }else{
                        if(reopen == 100000000){
                            //try to open again 
                            try {
                                System.out.println("Send the open packet to server ");
                                counter_open.counter++ ;
                                server_socket.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            reopen = 0;
                        }
                    } 
                    
        
                }
            } 
            this.lock_clientf.unlock();
        }       
     
    }


///read 
    public int mynfs_read(int fd ,rw_data data, num_messages counter_read){
        int read_codename;
        int read_pos;
        int read_end_pos;

        try {
            this.lock_seqno.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        int read_seqno = seqno;
        seqno ++;
        this.lock_seqno.unlock();
        try {
            this.lock_clientf.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Iterator <client_files> iter_r = client_files_list.iterator();
        client_files read_file;
        int found = 0;
        read_codename = 0 ;
        read_pos = 0 ;
        while(iter_r.hasNext()){
            read_file = iter_r.next();
            if(fd == read_file.getFd()){
                found = 1;
                print_list_string(read_file.getFlags());
                if(read_file.getFlags().contains("O_RDONLY") || read_file.getFlags().contains("O_RDWR")){
                    System.out.println("You have read access");
                    read_codename = read_file.getFilecode();
                    read_pos = read_file.getCurr_pos();

                }else{
                    System.out.println("You dont have read access to "+read_file.getFilename());
                    this.lock_clientf.unlock();
                    return -2;
                }
                break;
            }
        }
        this.lock_clientf.unlock();
        if(found == 0){
            System.out.println("Wrong fd");
            return -2;
        }
       

       
  //calculate in which block the data are and ask this blocks

        int start_block = read_pos/blocksize;
        int end_block = (read_pos + data.getSize())/blocksize;
        

        //construct nodes for each blosk with a different seqno  + add read request on list
     
        for(int i = 0; i<=end_block - start_block; i++){
             
                // add read request on list

            read_data new_r_request = new read_data();
            new_r_request.setSeqno(read_seqno+i);
            try {
                this.lock_seqno.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            seqno ++;
            this.lock_seqno.unlock();
        
            if(counter_read != null){
                new_r_request.messages =  counter_read.counter + 1;
                counter_read.counter = new_r_request.messages;
            }
            else {
                new_r_request.messages += 1;
            }

            try {
                this.lock_req.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            
            read_requests.add(new_r_request);

            this.lock_req.unlock();

           
        }
        //check if the blocks that we want is on hidden memory
        List<packet_couples> packet_couples_list = new LinkedList<>();
        Iterator<hidden_memory> hm_iter  = hidden_memory_list.iterator();
        hidden_memory hm = new hidden_memory();
        for (int j =0; j <= end_block - start_block; j++){
            found = 0;
            try {
                this.hm_lock.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
            hm_iter= hidden_memory_list.iterator(); 
            hm = new hidden_memory();
            int searchfor = start_block+j;
            
                while(hm_iter.hasNext() ){
                    hm = hm_iter.next();
                    //System.out.println("searchfor " + searchfor + " hm block "+ hm.getNum_of_block() + " codename " + read_codename);
                    if(searchfor == hm.getNum_of_block() && read_codename == hm.getCodename()){
                        found =1;
                        //already exist in memory

                        //check if the freshness is expired
                        if ((System.currentTimeMillis()  - hm.getFreshness())*0.001   >= freshT){

                            //send M message to server 
                            byte[] send_mod= new byte[21];
                        
                            String type_mod = "M";
                            byte[] type_mod_bytes = type_mod.getBytes();
                        
                            ByteBuffer send_mod_data = ByteBuffer.allocate(20);
                            send_mod_data.putInt(read_codename).putInt((start_block+j)*blocksize).putInt(blocksize).putInt(hm.getTmod()).putInt(read_seqno+j);
        
        
                            send_mod_data.rewind();
                            byte [] mod_data_bytes = new byte[send_mod_data.remaining()];
                            send_mod_data.position(0);
                            send_mod_data.get(mod_data_bytes);
        
                            byte[] mod_send_bytes = new byte[21];
                                
                            System.arraycopy(type_mod_bytes, 0, mod_send_bytes, 0, type_mod_bytes.length );
                            System.arraycopy(mod_data_bytes, 0, mod_send_bytes, type_mod_bytes.length , mod_data_bytes.length  );
                    
                            hm.setSeqno(read_seqno+j);
                            DatagramPacket packet = new DatagramPacket(mod_send_bytes, mod_send_bytes.length ,server_address.getAddress() , server_address.getPort());
                            try {
                                System.out.println("Send the mod packet to server ");
        
                                server_socket.send(packet);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            //save the packet to the packet couples list for  retransmition 
                            packet_couples pc= new packet_couples();
                            pc.setSeqno(j+read_seqno);
                            pc.setPacket(packet);
                            packet_couples_list.add(pc);


                        }else{
                            //the tmod is not expired so take the data and add them to the request

                            try {
                                this.lock_req.lock();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            Iterator<read_data> rd_iter = read_requests.iterator();
                            read_data rd= new read_data();
                            while (rd_iter.hasNext()){
                                rd = rd_iter.next();
                                if(rd.getSeqno() == read_seqno+j){

                                    rd.setData(hm.getBlock_data());
                                    rd.setSize(hm.getBlock_data().position(0).remaining());
                                    break;
                                }
                                
                            }

                            this.lock_req.unlock();

                        }
                        //z.setTime(System.currentTimeMillis() );

                    }

                }
                this.hm_lock.unlock();
                if(found == 0){
                    System.out.println("den vrika stin krifi mnimi add sthn hm codename " + read_codename + " seqno " + (read_seqno +j)+ " start block  " + (start_block +j));
                    //add request on hidden , if there is no space skip  a block with fifo algorithm
                    //make a read request and send packet 
                    //add the packet on the packet list
                    //send data to server
                    byte[] send_read = new byte[17];
                        
                    String type_read  = "R";
                    byte[] type_read_bytes = type_read.getBytes();
                    ByteBuffer send_read_data = ByteBuffer.allocate(16);
                    send_read_data.putInt(read_codename).putInt((start_block+j)*blocksize).putInt(blocksize).putInt(read_seqno+j);


                    send_read_data.rewind();
                    byte [] read_data_bytes = new byte[send_read_data.remaining()];
                    send_read_data.position(0);
                    send_read_data.get(read_data_bytes);
                    byte[] read_send_bytes = new byte[17];
                        
                    System.arraycopy(type_read_bytes, 0, read_send_bytes, 0, type_read_bytes.length );
                    System.arraycopy(read_data_bytes, 0, read_send_bytes, type_read_bytes.length , read_data_bytes.length  );
             
                    //add request on hidden memory
                    try {
                        this.hm_lock.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    if(hidden_memory_list.size() > cacheblocks){
                        print_hidden_memory();
                        //memory is full --> remove the first block
                        
                        
                        hidden_memory_list.removeFirst();
                        System.out.println("===remove ");
                        print_hidden_memory();
                        hidden_memory hm_new = new hidden_memory();
                        hm_new.setCodename(read_codename);
                        hm_new.setSeqno(read_seqno+j);
                        hm_new.setNum_of_block(start_block+j);
                        hidden_memory_list.addLast(hm_new);

                    }else{
                        hidden_memory hm_new = new hidden_memory();
                        hm_new.setCodename(read_codename);
                        hm_new.setSeqno(read_seqno+j);
                        hm_new.setNum_of_block(start_block+j);
                        hidden_memory_list.addLast(hm_new);
                    }
                    this.hm_lock.unlock();


                    DatagramPacket packet = new DatagramPacket(read_send_bytes, read_send_bytes.length ,server_address.getAddress() , server_address.getPort());
                    try {
                        System.out.println("Send the read packet to server " +"Seqno "+ (read_seqno+j));

                        server_socket.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                  
                    packet_couples pc= new packet_couples();
                    pc.setSeqno(j+read_seqno);
                    pc.setPacket(packet);
                    packet_couples_list.add(pc);

                }

        }
       
        //wait till get the data
        int reread = 0;
        int counter_resend =0;
        int end = 0 ;
        while(counter_resend <packet_couples_list.size() ){
            //GIA OLAta paketa pou steilame
            for(int c = 0; c<packet_couples_list.size(); c++){
            reread = 0;
            end = 0 ;
                while(true){
                    reread++;
                    try {
                        this.lock_req.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                
                    Iterator<read_data> iter_rd = read_requests.iterator();
                    read_data get_read = new read_data();

                    while(iter_rd.hasNext()){
                
                        get_read = iter_rd.next();
                        //request seqno na einai idio me tou paketou
                        if(get_read.getSeqno() == packet_couples_list.get(c).getSeqno()){
                           
                        
                            if(get_read.getData() != null || get_read.getSize() != -10){
                                counter_resend++;
                                //read the data
                                if(get_read.getSize() == -1 || get_read.getSize() == 0){
                                    
                                        get_read.setData(ByteBuffer.allocate(0)) ;
                                        end = 1;
                                }else if(get_read.getSize() == -3){
                                   
                                    int old_fd=0;
                                    int old_codename =  get_read.getData().getInt();
                                    int old_pos = 0;
                                    int old_messages = get_read.messages;
                                
                                   // System.out.println("old messages : " + old_messages);

                                    List <String> old_flags = new LinkedList< >();
                                    String old_filename="";

                                    //remove all request for this read call

                                    for (int r =0; r<= end_block - start_block; r++){
                                        iter_rd = read_requests.iterator();
                                        while(iter_rd.hasNext()){
                                            get_read = iter_rd.next();
                                            if(get_read.getSeqno() == read_seqno+r){
                                                iter_rd.remove();
                                                break;
                                            }
                                        }
                                    }
                                
                                    this.lock_req.unlock();
                                    try {
                                        this.lock_clientf.lock();
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                
                                    Iterator<client_files> f_iter = client_files_list.iterator();
                                    client_files curr_file ;
                                    while(f_iter.hasNext()){
                                        
                                        curr_file  = f_iter.next();
                                        if(curr_file.getFilecode() == old_codename){
                                            old_pos = curr_file.getCurr_pos();
                                            old_filename = curr_file.getFilename();
                                            old_fd = curr_file.getFd();
                                            
                                            for(int i=0; i< curr_file.getFlags().size(); i++){
                                                old_flags.add(curr_file.getFlags().get(i));
                                            }
                                            f_iter.remove();
                                            break;
                                        }
                                        
                                    }
                                    this.lock_clientf.unlock();

                                    num_messages messages = new num_messages();
                                    //reopen
                                    int new_fd = mynfs_open(old_filename,  old_flags, messages);
                                   // System.out.println("open: " + messages.counter);
                                
                                        try {
                                        this.lock_clientf.lock();
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    f_iter = client_files_list.iterator();
                                    int new_codename = 0;
                                    while(f_iter.hasNext()){
                                        
                                        curr_file  = f_iter.next();
                                        if(curr_file.getFd() == new_fd){
                                            curr_file.setFd(old_fd);
                                            curr_file.setCurr_pos(old_pos);
                                            new_codename = curr_file.getFilecode();
                                            break;
                                        }
                                        
                                    }
                                    this.lock_clientf.unlock();
                                    messages.counter = old_messages + messages.counter ;
                                    //change the codename in hidden memory 
                                    try {
                                        this.hm_lock.lock();
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    
                                    hm_iter= hidden_memory_list.iterator();
                                    hm = new hidden_memory();
                                    while(hm_iter.hasNext()){
                                        hm = hm_iter.next();
                                        if(hm.getCodename() == old_codename){
                                            hm.setCodename(new_codename);
                                            hm.setTmod(0);
                                            if(hm.getBlock_data() == null){
                                                hm_iter.remove();
                                            }
                                         
                                        }
                                    }
                                    this.hm_lock.unlock();


                                    int return_value = mynfs_read(fd , data, messages);
                                    System.out.println(" Return Num of Messages gia read: " + messages.counter);
                            
                                    return return_value;

                                }
                                else {
                                    packet_couples_list.remove(c);
                                    c--;
                                    end = 1 ;
                                    break ;
                                }
                             
                            }
                            else {
                                if(reread == 1000000){
                                    reread = 0;
                                    get_read.messages++;
                                    try {
                                      //  System.out.println("------>Now " + get_read.messages);
                                        System.out.println("Send the read packet to server Retransmit");
                                
                                        server_socket.send(packet_couples_list.get(c).getPacket());
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                            break ;
                        }
                    
                    }
                    this.lock_req.unlock();
                    if(end == 1){
                        break ;
                    }
                }
               
            }
            
        }
        //found the bytes that app really want to read and return
        ByteBuffer temp = ByteBuffer.allocate(data.getSize());
        
        int counter = 0;

        try {
            this.lock_req.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int wanted_size = data.getSize();
       // System.out.println("from , end"+ (end_block-start_block));
        for(int i = 0; i<=end_block- start_block; i++){
            Iterator<read_data> rd_iter = read_requests.iterator();
            read_data rd;
            int from =0;
            int to =0;
            
            while (rd_iter.hasNext()){
                rd = rd_iter.next();
                if(rd.getSeqno() == read_seqno + i){
                   
                     from = read_pos-((start_block+i)*blocksize);
                    if(wanted_size > (blocksize -from) ){

                         to  = blocksize-1 ;
                    }else{
                         to  = from +wanted_size-1;
                    }
                    rd.getData().rewind();
                    int size_buf = rd.getData().position(0).remaining();
                    System.out.println("size_buf " + size_buf + " read seqno " + (read_seqno+i));
                    System.out.println("from "+ from + " to " + to + "temp size " + temp.remaining());
                    for(int j =from; j<=to && j < size_buf; j++){
                        counter++;
                        temp.put(rd.getData().get(j));
                        wanted_size--;
                    }
                }
            }
            read_pos = read_pos+(to-from)+1;
        }
        System.out.println("counter " + counter);
        ByteBuffer return_data = ByteBuffer.allocate(counter);
        temp.rewind();
        for(int j = 0 ; j< counter; j++){
        
            return_data .put(temp.get(j));
        }
        this.lock_req.unlock();
        return_data.rewind();
        data.setData(return_data);
        data.setSize(return_data.position(0).remaining());
        try {
            this.lock_clientf.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Iterator<client_files> cf_iter = client_files_list.iterator();
        client_files cf = new client_files();
        while(cf_iter.hasNext()){
            cf = cf_iter.next();
            if(cf.getFilecode() == read_codename){
                cf.setCurr_pos(cf.getCurr_pos()+data.getSize());
            }
        }

        this.lock_clientf.unlock();
        System.out.println("return ");
        return(return_data.position(0).remaining());
        

    }


    public int mynfs_seek(int fd , int offset , String whence, num_messages counter_seek){
        try {
            this.lock_clientf.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Iterator<client_files> iter = client_files_list.iterator();
        client_files seek_file ;
        int find = 0;
        int return_value = 0;
        int file_length = -1;
        int reseek = 0 ;
        System.out.println("seek locks lock_clientf");
        while(iter.hasNext()){
            seek_file = iter.next();
            if(seek_file.getFd() == fd){
                find = 1;
                if(whence.equals("SEEK_CUR")){
                    seek_file.setCurr_pos(seek_file.getCurr_pos()+offset);
                    return_value= seek_file.getCurr_pos()+offset;
                    this.lock_clientf.unlock();
                    break;
                }else if (whence.equals("SEEK_SET")){
                    seek_file.setCurr_pos(offset);
                    return_value = offset;
                    this.lock_clientf.unlock();
                    break;
                }else if (whence.equals("SEEK_END")){
                    int codename = seek_file.getFilecode();
                    this.lock_clientf.unlock();
                    try {
                        this.lock_seqno.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    int seek_seqno = seqno;
                    seqno ++;
                    this.lock_seqno.unlock();

                    //send seek data 

                    byte[] send_seek = new byte[9];
       
                    String type_seek  = "S";
                    byte[] type_seek_bytes = type_seek.getBytes();
                    ByteBuffer send_seek_data = ByteBuffer.allocate(8);
                    send_seek_data.putInt(seek_seqno).putInt(codename);
                
            
                    send_seek_data.rewind();
                    byte [] seek_data_bytes = new byte[send_seek_data.remaining()];
                    send_seek_data.position(0);
                    send_seek_data.get(seek_data_bytes);
            
                    byte[] seek_send_bytes = new byte[9];
                       
                    System.arraycopy(type_seek_bytes, 0, seek_send_bytes, 0, type_seek_bytes.length );
                    System.arraycopy(seek_data_bytes, 0, seek_send_bytes, type_seek_bytes.length ,seek_data_bytes.length  );
                    System.out.println("Send the seek packet to server SIZE "+ seek_send_bytes.length);
                    //add to seel list 
                    seek_data new_seek = new seek_data();
                    new_seek.setSeqno(seek_seqno);
                    try {
                        this.lock_seek.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(counter_seek != null){
                        new_seek.messages =  counter_seek.counter + 1;
                        counter_seek.counter = new_seek.messages;
                    }
                    else {
                        new_seek.messages += 1;
                    }

                    seek_requests.add(new_seek);
                    this.lock_seek.unlock();
                    DatagramPacket packet = new DatagramPacket(seek_send_bytes, seek_send_bytes.length ,server_address.getAddress() , server_address.getPort());
                    try {
                        System.out.println("Send the read packet to server ");
                        server_socket.send(packet);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    while(true){
                        try {
                            this.lock_seek.lock();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        Iterator<seek_data> seek_iter = seek_requests.iterator();
                        seek_data curr_seek;
                        while(seek_iter.hasNext() ){
                            curr_seek = seek_iter.next();
                            if(curr_seek.getSeqno() == seek_seqno){
                                if(curr_seek.getFile_length() > -1){
                                    file_length = curr_seek.getFile_length();
                                    seek_iter.remove();
                               
                                    break;
                                }
                                else if(curr_seek.getFile_length() == -3){
                                    System.out.println("my_func seek " + "-3 reopen ");
                                        
                                    int old_fd=0;
                                    int old_codename =  curr_seek.getFileCode();
                                    int old_pos = 0;
                                    int old_messages = curr_seek.messages;
                                    List <String> old_flags = new LinkedList< >();
                                    String old_filename="";
                                    seek_iter.remove();
                                    this.lock_seek.unlock();
                                    try {
                                        this.lock_clientf.lock();
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                
                                    Iterator<client_files> f_iter = client_files_list.iterator();
                                    client_files curr_file ;
                                    while(f_iter.hasNext()){
                                        
                                        curr_file  = f_iter.next();
                                        if(curr_file.getFilecode() == old_codename){
                                            old_pos = curr_file.getCurr_pos();
                                            old_filename = curr_file.getFilename();
                                            old_fd = curr_file.getFd();
                                            
                                            for(int i=0; i< curr_file.getFlags().size(); i++){
                                                old_flags.add(curr_file.getFlags().get(i));
                                            }
                                            f_iter.remove();
                                            break;
                                        }
                                        
                                    }
                                    this.lock_clientf.unlock();

                                    num_messages messages = new num_messages();
                                    int new_fd = mynfs_open(old_filename,  old_flags, null);
                                    System.out.println("open: " + messages.counter);

                                    try {
                                        this.lock_clientf.lock();
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    f_iter = client_files_list.iterator();
                                    while(f_iter.hasNext()){
                                        
                                        curr_file  = f_iter.next();
                                        if(curr_file.getFd() == new_fd){
                                            curr_file.setFd(old_fd);
                                            curr_file.setCurr_pos(old_pos);
                                            break;
                                        }
                                        
                                    }
                                    this.lock_clientf.unlock();
                                    messages.counter = old_messages + messages.counter ;
                                    return_value = mynfs_seek( fd ,offset,whence ,messages);
                                    System.out.println("Num of Messages gia seek: " + messages.counter);
                                    return return_value;

                                }
                            }
                            else {
                                if(reseek == 1000000){
                                   
                                   reseek = 0;
                                    try {
                                        System.out.println("Send the seek packet to server retransmit");
                                        server_socket.send(packet);
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                            
                        }

                        this.lock_seek.unlock();
                        if(file_length > -1){
                            break;
                        }
                    }
             
                }else{
                    //error whence
                    System.out.println("Wrong whence");
                    this.lock_clientf.unlock();
                    return -1;
                }
                break;
            }
        }

        if(file_length > -1){

            try {
                this.lock_clientf.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            iter = client_files_list.iterator();
            while(iter.hasNext()){
                seek_file = iter.next();
                if(seek_file.getFd() == fd){
                    seek_file.setCurr_pos(file_length+offset);
                    return_value = file_length+offset;
                    break;
                }
            }
            this.lock_clientf.unlock();
        }
       
        if(find == 0){
            System.out.println("You haven't open file");
            this.lock_clientf.unlock();
            return -1;
        }
    
        return return_value;
    }

///////////////////////WRITE
public int mynfs_write(int fd , rw_data data, num_messages counter_write){
    //take the request seqno
    try {
        this.lock_seqno.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    
    int write_seqno = seqno;
    seqno ++;
    this.lock_seqno.unlock();

    //go to list with files
    try {
        this.lock_clientf.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    Iterator <client_files> iter_w = client_files_list.iterator();
    client_files write_file;
    int found = 0;
    int write_codename = 0 ;
    int write_pos = 0 ;
    while(iter_w.hasNext()){
        write_file = iter_w.next();
        if(fd == write_file.getFd()){
            found = 1;
            print_list_string(write_file.getFlags());
            if(write_file.getFlags().contains("O_WRONLY") || write_file.getFlags().contains("O_RDWR")){
                System.out.println("You have read access");
                write_codename = write_file.getFilecode();
                write_pos = write_file.getCurr_pos();

            }else{
                System.out.println("You dont have read access to "+write_file.getFilename());
                this.lock_clientf.unlock();
                return -2;
            }
            break;
        }
    }
    this.lock_clientf.unlock();



    if(found == 0){
        System.out.println("Wrong fd");
        return -2;
    }


    // add write request on list

    write_data new_w_request = new  write_data();
    new_w_request.setSeqno(write_seqno);
    try {
        this.lock_write.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
   
    if(counter_write!= null){
        new_w_request.messages =  counter_write.counter + 1;
        counter_write.counter = new_w_request.messages;
    }
    else {
        new_w_request.messages++;
    }
    write_requests.add(new_w_request);


    this.lock_write.unlock();


    //send data to server
  
   
    String type_write  = "W";
    byte[] type_write_bytes = type_write.getBytes();
  
    ByteBuffer send_write_data = ByteBuffer.allocate(16);
    send_write_data.putInt(write_codename).putInt(write_pos).putInt(data.getSize()).putInt(write_seqno);


    send_write_data.rewind();
    byte [] write_data_bytes = new byte[send_write_data.remaining()];
    send_write_data.position(0);
    send_write_data.get(write_data_bytes);

    data.getData().rewind();
    byte[] data_to_write = new byte[data.getData().remaining()];
    data.getData().position(0);
    data.getData().get(data_to_write);
    byte[] write_send_bytes = new byte[17+data.getSize()];
       
    System.arraycopy(type_write_bytes, 0, write_send_bytes, 0, type_write_bytes.length );
    System.arraycopy(write_data_bytes, 0, write_send_bytes, type_write_bytes.length , write_data_bytes.length  );
    System.arraycopy(data_to_write, 0, write_send_bytes, type_write_bytes.length +write_data_bytes.length, data_to_write.length);
    System.out.println("Send the read packet to server SIZE "+ write_send_bytes.length);

    DatagramPacket packet = new DatagramPacket(write_send_bytes, write_send_bytes.length ,server_address.getAddress() , server_address.getPort());
    try {
        System.out.println("Send the write packet to server ");
        server_socket.send(packet);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }

    //wait till get ack 
    int rewrite = 0;
   
    while(true){
    rewrite++;
    try {
        this.lock_write.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }


    Iterator<write_data> iter_wr = write_requests.iterator();
    write_data get_write = new write_data();

        while(iter_wr.hasNext()){
    
            get_write = iter_wr.next();
            if(get_write.getSeqno() == write_seqno){
                if(get_write.getSize_written() != -10){
                   
                    if(get_write.getSize_written() == -1){
                        //error
                        iter_wr.remove();
                        this.lock_write.unlock();
                        System.out.println("return;");
                        return get_write.getSize_written();
                    }else if(get_write.getSize_written() == -3){
                           
                        int old_fd=0;
                        int old_codename =  get_write.getCodename();
                        int old_pos = 0;
                        int old_messages = get_write.messages ;
                        List <String> old_flags = new LinkedList< >();
                        String old_filename="";
                        iter_wr.remove();
                        this.lock_write.unlock();
                        try {
                            this.lock_clientf.lock();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                       
                        Iterator<client_files> f_iter = client_files_list.iterator();
                        client_files curr_file ;
                        while(f_iter.hasNext()){
                            
                            curr_file  = f_iter.next();
                            if(curr_file.getFilecode() == old_codename){
                                old_pos = curr_file.getCurr_pos();
                                old_filename = curr_file.getFilename();
                                old_fd = curr_file.getFd();
                                
                                for(int i=0; i< curr_file.getFlags().size(); i++){
                                    old_flags.add(curr_file.getFlags().get(i));
                                }
                                f_iter.remove();
                                break;
                            }
                            
                        }
                        this.lock_clientf.unlock();

                        num_messages messages = new num_messages();
                      
                        int new_fd = mynfs_open(old_filename,  old_flags,messages);
                        messages.counter = old_messages + messages.counter ;
                        

                        try {
                            this.lock_clientf.lock();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        f_iter = client_files_list.iterator();
                        while(f_iter.hasNext()){
                            
                            curr_file  = f_iter.next();
                            if(curr_file.getFd() == new_fd){
                                curr_file.setFd(old_fd);
                                curr_file.setCurr_pos(old_pos);
                                break;
                            }
                            
                        }
                        this.lock_clientf.unlock();
                        int return_value = mynfs_write(fd , data, messages);
                        System.out.println("Num of Messages gia write: " + messages.counter);

                        return return_value;
                    }
              
                    Iterator<client_files> f_iter = client_files_list.iterator();
                    client_files curr_file ;
                    while(f_iter.hasNext()){
                        
                        curr_file  = f_iter.next();
                        System.out.println(curr_file.getFilecode()+" ------------- "+"success"+write_codename);
                        if(curr_file.getFilecode() == write_codename){
                            curr_file.setCurr_pos(curr_file.getCurr_pos()+get_write.getSize_written());
                            
                            try {
                                this.hm_lock.lock();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            int start_block = write_pos/blocksize;
                            int end_block = (write_pos + data.getSize())/blocksize;
                            Iterator <hidden_memory>iter_hm = hidden_memory_list.iterator();
                            hidden_memory z_hm ;
                            int wanted_size = data.getSize();
                            int index = 0 ;
                            int from = 0;
                            int to = 0 ;
                            while(iter_hm.hasNext()){
                                z_hm = iter_hm.next();
                                if(z_hm.getCodename() == curr_file.getFilecode()){
                                    for (int i = 0 ; i <= end_block - start_block ; i++){
                                        if (z_hm.getNum_of_block() == i+start_block){
                                            from = write_pos-((start_block+i)*blocksize);
                                            if(wanted_size > (blocksize -from) ){

                                                to  = blocksize-1 ;
                                            }else{
                                                to  = from +wanted_size-1;
                                            }
                                            for(int r = from ;r <=to ; r++){
                                                z_hm.getBlock_data().put(r, data.getData().get(index));
                                                index++;
                                                System.out.println("index " + r + " egine "+ (char)(z_hm.getBlock_data().get(r)));
                                            }
                                            
                                        }
                                    }
                                } 
                            }

                            
                            this.hm_lock.unlock();
                            iter_wr.remove();
                            break;
                        }
                        
                    }
                    this.lock_write.unlock();
                    System.out.println("return;");
                    return get_write.getSize_written();
                }
                else {
                    if(rewrite == 1000000){
                        rewrite = 0;
                        get_write.messages++;
                        try {
                            System.out.println("------>"+ get_write.messages);
                            System.out.println("Send the write packet to server " );
                            server_socket.send(packet);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
           
        }
        this.lock_write.unlock();
    }

}


/////////////////////TRUNCATE
public int mynfs_ftrancate(int fd, int len, num_messages counter_trunc){
    int trunc_codename = 0;
    try {
        this.lock_seqno.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    
    int trunc_seqno = seqno;
    seqno ++;
    this.lock_seqno.unlock();

    //go to list with files
    try {
        this.lock_clientf.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
  
    int found =0;
    Iterator <client_files> iter_t = client_files_list.iterator();
    client_files trunc_file  ;
    while(iter_t.hasNext()){
        trunc_file = iter_t.next();
        if(fd == trunc_file.getFd()){
            found = 1;
            print_list_string(trunc_file.getFlags());
            if(trunc_file.getFlags().contains("O_TRUNC")){
                System.out.println("You have trunc access");
                 trunc_codename = trunc_file.getFilecode();
                 this.lock_clientf.unlock();
            }else{
                System.out.println("You dont have trunc access to "+trunc_file.getFilename());
                this.lock_clientf.unlock();
                return -1;
            }
            break;
        }
    }
    if(found == 0){
        System.out.println("You gave wrong fd");
        return(-1);
    }

        // add write request on list

        trunc_data new_t_request = new  trunc_data();
        new_t_request.setSeqno(trunc_seqno);
        try {
            this.lock_trunc.lock();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(counter_trunc!= null){
            new_t_request.messages =  counter_trunc.counter + 1;
            counter_trunc.counter = new_t_request.messages;
        }
        else {
            new_t_request.messages++;
        }

        trunc_requests.add(new_t_request);
    
        this.lock_trunc.unlock();

    String type_trunc  = "T";
    byte[] type_trunc_bytes = type_trunc.getBytes();
  
    ByteBuffer send_trunc_data = ByteBuffer.allocate(16);

    send_trunc_data.putInt(trunc_codename).putInt(len).putInt(trunc_seqno);


    send_trunc_data.rewind();
    byte [] trunc_data_bytes = new byte[send_trunc_data.remaining()];
    send_trunc_data.position(0);
    send_trunc_data.get(trunc_data_bytes);

 
    byte[] trunc_send_bytes = new byte[17];
       
    System.arraycopy(type_trunc_bytes, 0, trunc_send_bytes , 0, type_trunc_bytes.length );
    System.arraycopy(trunc_data_bytes, 0, trunc_send_bytes , type_trunc_bytes.length , trunc_data_bytes.length  );

    System.out.println("Send the trunc packet to server SIZE "+ trunc_send_bytes.length);

    DatagramPacket packet = new DatagramPacket(trunc_send_bytes, trunc_send_bytes.length ,server_address.getAddress() , server_address.getPort());
    try {
        System.out.println("Send the trunc packet to server ");
        server_socket.send(packet);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }



     //wait till get ack 
     int retrunc = 0;
   
     while(true){
    retrunc++;
     try {
         this.lock_trunc.lock();
     } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
     }
 
     Iterator<trunc_data> iter_tr = trunc_requests.iterator();
     trunc_data get_trunc = new trunc_data();
 
         while(iter_tr.hasNext()){
     
            get_trunc = iter_tr.next();
           
             if(get_trunc.getSeqno() == trunc_seqno){
                 
                if(get_trunc.getReturn_value() != -10 && get_trunc.getReturn_value() != -3){
                    
             
                    iter_tr.remove();
                    this.lock_trunc.unlock();
                    System.out.println("return;");
                    return get_trunc.getReturn_value();
                
                 }
                 else if(get_trunc.getReturn_value() == -3){
                     System.out.println("my_func trunc " + "-3 reopen ");
                           
                    int old_fd=0;
                    int old_codename =  get_trunc.getCodename();
                    int old_messages = get_trunc.messages ;
                    int old_pos = 0;
                    List <String> old_flags = new LinkedList< >();
                    String old_filename="";
                    iter_tr.remove();
                    this.lock_trunc.unlock();
                    try {
                        this.lock_clientf.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                   
                    Iterator<client_files> f_iter = client_files_list.iterator();
                    client_files curr_file ;
                    while(f_iter.hasNext()){
                        
                        curr_file  = f_iter.next();
                        if(curr_file.getFilecode() == old_codename){
                            old_pos = curr_file.getCurr_pos();
                            old_filename = curr_file.getFilename();
                            old_fd = curr_file.getFd();
                            
                            for(int i=0; i< curr_file.getFlags().size(); i++){
                                old_flags.add(curr_file.getFlags().get(i));
                            }
                            f_iter.remove();
                            break;
                        }
                        
                    }
                    this.lock_clientf.unlock();
                    num_messages messages = new num_messages();
                    int new_fd = mynfs_open(old_filename,  old_flags, null);
                     messages.counter = old_messages + messages.counter ;
                    try {
                        this.lock_clientf.lock();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    f_iter = client_files_list.iterator();
                    while(f_iter.hasNext()){
                        
                        curr_file  = f_iter.next();
                        if(curr_file.getFd() == new_fd){
                            curr_file.setFd(old_fd);
                            curr_file.setCurr_pos(old_pos);
                            break;
                        }
                        
                    }
                    this.lock_clientf.unlock();
                    return(mynfs_ftrancate( fd , len, messages));
                }
                 else {
                     if(retrunc == 1000000){
                        get_trunc.messages++;
                        retrunc = 0;
                         try {
                             System.out.println("Send the trunc packet to server ");
                             server_socket.send(packet);
                         } catch (IOException e) {
                             // TODO Auto-generated catch block
                             e.printStackTrace();
                         }
                     }
                 }
             }
            
         }
         this.lock_trunc.unlock();
     }
 

}

public int close(int fd){
    try {
        this.lock_clientf.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    Iterator <client_files>iter_files = client_files_list.iterator();
    client_files z_file ;
    int filecode  = 0 ;
    int found = 0 ;
    while(iter_files.hasNext()){
        z_file = iter_files.next();

        if(z_file.getFd() ==fd){
            filecode = z_file.getFilecode();
            iter_files.remove();
            found = 1;
            break ;
        }
        
    } 
    this.lock_clientf.unlock();
    try {
        this.hm_lock.lock();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    Iterator <hidden_memory>iter_hm = hidden_memory_list.iterator();
    hidden_memory z_hm ;

    while(iter_hm.hasNext()){
        z_hm = iter_hm.next();
        if(z_hm.getCodename() == filecode){
            iter_hm.remove();
        }
        
    } 
    
    this.hm_lock.unlock();
    if(found == 0 ){
        return -1 ;
    }
    return 0 ;
}
public List<Integer> convert_to_int(List <String>list){

    List <Integer>ret_list = new LinkedList<Integer>();
    Iterator <String>iter = list.iterator();
    String z ;
    while (iter.hasNext()){
        int num ;
        z = iter.next();

        if(z.equals("O_CREAT")){
            ret_list.add(1);
            continue ;
        }else if(z.equals("O_EXCL")){
            ret_list.add(2);
            continue ;
        }else  if(z.equals("O_TRUNC")){
            ret_list.add(3);
            continue ;
        }else if(z.equals("O_RDWR")){
            ret_list.add(4);
            continue ;
        }else if(z.equals("O_RDONLY")){
            ret_list.add(5);
            continue ;
        }else if(z.equals("O_WRONLY"))
        {
            ret_list.add(6);
            continue ;
        }else{
            ret_list.add(7);
            continue ;
        }

        
    }
    return ret_list ;
    }


    public void print_list_string(List <String> list){
        Iterator <String> iter = list.iterator();
        String current ;
        while(iter.hasNext()){
            current = iter.next();
            System.out.println(current);
        }
    }
    public void print_list_int(List <Integer> list){
        Iterator <Integer> iter = list.iterator();
        Integer current ;
        while(iter.hasNext()){
            current = iter.next();
            System.out.println(current);
        }
    }
    public void print_hidden_memory(){
        Iterator <hidden_memory> iter = hidden_memory_list.iterator();
        hidden_memory z ; 
        System.out.println("====HIDDEN MEMORY=======");
        while(iter.hasNext()){
            z = iter.next();
            
          
            System.out.println("codename" + "---->" + z.getCodename());
            System.out.println("num block" + "---->" +z.getNum_of_block() );
         

            
          
        }
    }
    public void print_list_client_files(List <client_files> list){
        Iterator <client_files> iter = list.iterator();
        client_files z ; 
        while(iter.hasNext()){
            z = iter.next();
            System.out.println("--->filename "+ z.getFilename());
            System.out.println("--->fd "+ z.getFd());
            System.out.println("--->filecode  "+ z.getFilecode() );
        }
    }

}