import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class Server {
  
  private static Serverapi server ;


  public  void thread_run() throws InterruptedException{
    byte[] received_data ;
    int num ;
    int service ;
    serverdata buffer = new serverdata();
    Thread.currentThread().setPriority(10);
    int reqid = 0 ;
    int counter = 0 ;
    

   
    while(true){
      
      try {
       
        reqid = server.getRequest (1 ,buffer);
      } catch (InterruptedException | IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      received_data = buffer.getData();
      
      
       // byte []buffer_send  = ByteBuffer.allocate(4).putInt(0).array();
       // server.sendReply (reqid, buffer_send, buffer_send.length);
       // continue ;
      
      
      
      
    
    if (counter == 10){
      try {
        System.out.println("server is slow");
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
   
     counter++ ;
    num = ByteBuffer.wrap(received_data).getInt(); 


    int out = 0;
    for (int i = 2; i <= num / 2; ++i) {
      // condition for nonprime number
      if (num % i == 0) {
        out = 1;
        break;
      }
    }

    if (out == 0)
      System.out.println(num + " is a prime number.");
    else
      System.out.println(num + " is not a prime number.");

     byte []buffer_send  = ByteBuffer.allocate(4).putInt(out).array();
  
    server.sendReply (reqid, buffer_send, buffer_send.length);
  }

  }
    public static void main(String[] args) throws SocketException{

     
      Scanner sc = new Scanner(System.in);
      System.out.println("give your mac");
      server = new Serverapi(sc.next());
      System.out.println("SERVER START");
      server.init() ;
      server.register(1);

      //////MIDENIKI
      server.register(0);


     


      Thread thread_1 = new Thread(new Runnable(){public void run() {try {
        new Server().thread_run();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }}});
      thread_1.start();


      Thread thread_2 = new Thread(new Runnable(){public void run() {try {
        new Server().thread_run();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }}});
      thread_2.start();
      
      Thread thread_3 = new Thread(new Runnable(){public void run() {try {
        new Server().thread_run();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }}});
      thread_3.start();
  


    }
       
       
        
}