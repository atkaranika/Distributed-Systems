import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class client_app{

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
    
        client_api api = new client_api();
        Scanner scaner = new Scanner(System.in);
        List <String> flags = new LinkedList<String>();
        int end = 0 ;
      
        while(true){
            String input = scaner.nextLine();
            String[] words=input.split(" ");
        switch(words[0])
            {
		    case "I":
                    System.out.println("Init ");
                    
                    int cacheblocks  = Integer.parseInt(words[1]);
                    int blocksize =  Integer.parseInt(words[2]);
                    int freshT =  Integer.parseInt(words[3]);
                    api.mynfs_init("10.0.0.1", 4040,cacheblocks,blocksize,freshT);
         
                    break;
                case "O":
                    System.out.println("You wanna open "+words[1]);
                    for (int i = 2 ; i < words.length ; i++){
                        flags.add(words[i]);
                    }
                    int fd  = api.mynfs_open(words[1], flags, null);
                    if(fd == -1){
                        System.out.println("Fail to open file ...try again");
                    }else{
                        System.out.println("fd for "+words[1]+" is "+fd);
                    }
                    flags.removeAll(flags);
                    break;
                case "R":
                    System.out.println("You wanna read "+words[1]);
                   
                    int fdr  = Integer.parseInt(words[1]);
                    rw_data data = new rw_data();
                    data.setSize(Integer.parseInt(words[2]));
                    int size = api.mynfs_read(fdr, data, null);
                    if(size == -1){

                        System.out.println("Wrong on read");
                    }else{
                    
                        data.getData().rewind();
                        byte[] str = new byte[data.getData().remaining()];
                        data.getData().get(str);
                        String stri = new String(str);
                        System.out.println("--->App size " + size + " data "+ stri);

                    }
                    break;
                case "W":
                        System.out.println("You wanna write ");
                        int fdw  = Integer.parseInt(words[1]);
                       // int length = Integer.parseInt(words[2]);
                        String write_data = input.substring(words[0].length()+words[1].length()+2);
                        System.out.println("\n  ----------------------------- \n"+write_data);

                        rw_data data_to_write = new rw_data();
                        byte[] write_data_bytes = write_data.getBytes();
                        ByteBuffer write_data_buf = ByteBuffer.wrap(write_data_bytes);
                        data_to_write.setData(write_data_buf);
                        data_to_write.setSize(write_data.length());

                        int write_ckeck = api.mynfs_write(fdw, data_to_write, null);
                        System.out.println("Write return : "+write_ckeck);
                        break;                      

                case "S":
                    System.out.println("You wanna seek ");
                    int fds  = Integer.parseInt(words[1]);
                    int offset = Integer.parseInt(words[2]);
                    String whence = words[3];
                    int seek_ckeck = api.mynfs_seek(fds, offset, whence, null);
                    System.out.println("Seek return : "+seek_ckeck);
                    break;
                case "T" :
                    int fdt  = Integer.parseInt(words[1]);
                    int len = Integer.parseInt(words[2]);
                    int return_trunc = api.mynfs_ftrancate(fdt, len, null);
                    System.out.println("Truncate return : "+return_trunc);
                    break;
                case "X":
                    end = 1;
                
                    break;
                case "C":
                    int fdc  = Integer.parseInt(words[1]);
                    int r = api.close(fdc);
                    if(r == 0){
                        System.out.println("closed : " + fdc);
                    }
                    else {
                        System.out.println("fail sto close wrong fd : " + fdc);
                    }
                    break ;
                case "P":

                    flags.add("O_RDWR");
                    String stri ="";
                    int fd0  = api.mynfs_open(words[1], flags, null);
                    if(fd0 == -1){
                        System.out.println("Fail to open file ...try again");
                        break ; 
                    }else{
                        System.out.println("fd for "+words[1]+" is "+fd0);
                    }
                    flags.removeAll(flags);

                    flags.add("O_CREAT");
                    flags.add("O_RDWR");

                    int fd1  = api.mynfs_open(words[2], flags, null);
                    if(fd1 == -1){
                        System.out.println("Fail to open file ...try again");
                        break ; 
                    }else{
                        System.out.println("fd for "+words[1]+" is "+fd0);
                    }
                    int i = 1;
                    while(true){
                        rw_data datac = new rw_data();
                        datac.setSize(200);
                        int sizer = api.mynfs_read(fd0, datac, null);
                        if(sizer == 0 ){
                            break ;
                        }
                        if(sizer == -1){
    
                            System.out.println("Wrong on read");
                            break ;
                        }else{
                        
                            datac.getData().rewind();
                            byte[] str = new byte[datac.getData().remaining()];
                            datac.getData().get(str);
                            stri = new String(str);
                            System.out.println("--->App size " + sizer + " data "+ stri);
                        }
                        flags.removeAll(flags);


                        System.out.println("You wanna write ");
            
                        // int length = Integer.parseInt(words[2]);
                        String write_datac = stri;
                

                        rw_data data_to_write_c = new rw_data();
                        byte[] write_data_bytes_c = write_datac.getBytes();
                        ByteBuffer write_data_buf_c = ByteBuffer.wrap(write_data_bytes_c);
                        data_to_write_c.setData(write_data_buf_c);
                        data_to_write_c.setSize(write_datac.length());

                        int write_ckeckc = api.mynfs_write(fd1, data_to_write_c, null);
                        System.out.println("Write return : "+write_ckeckc);
                        


              
                }
            if(end ==1){
                break ;
            }
        }


        }   
    }
}