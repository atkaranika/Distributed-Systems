import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;



public class Messenger_app implements Serializable{
    private static LinkedList<application_info> teams = new LinkedList<application_info>();
    private static Messenger_api api;
    public static int join_fd;
    private int team_code;
    public static int end=0;
    public static Lock lock_main = new Lock();
    Messenger_app(int g){
        team_code =g;
    }

    private void view_team_change(){
        while(true){
            try {
                api.lock_change.lock();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
            if(indexof_change(team_code,api.team_changes) == -1){
                api.lock_change.unlock();
                return;
            }
            if(api.team_changes.get(indexof_change(team_code,api.team_changes)).getChange() == 1){
                api.team_changes.get(indexof_change(team_code,api.team_changes)).setChange(0);
                api.lock_change.unlock();
                try {
                    api.lock.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            
                try {
                    this.lock_main.lock();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch blo    ck
                    e.printStackTrace();
                }
                if(indexof( team_code,api.mess_teams) == -1 || indexof_app(team_code, teams) == -1){
                    this.lock_main.unlock();
                    api.lock.unlock();
                    return;
                }else{
                    teams.get(indexof_app(team_code, teams)).setCurrent_team(api.mess_teams.get(indexof( team_code,api.mess_teams)).getCurrent_team());
                    this.lock_main.unlock();
                    api.lock.unlock();
                }

            }else{
                api.lock_change.unlock();
            }
        }
    }
    private void group_receive_thread(){
        deliver_mesage new_msg = new deliver_mesage();
        int check = 0;
        while(true){
            check = api.grp_recv(team_code,new_msg,1);
            if(check == -2){
                return ; 
            }
            if(check != -1){
                System.out.println("APP team "+team_code+"  "+new String(new_msg.getMessage()));
            }
        }
    }

    public static int indexof(int g, List<messenger_teams> list){

        Iterator <messenger_teams> iter = list.iterator();
        messenger_teams z ;
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
    public static int indexof_app(int g, List<application_info> list){

        Iterator <application_info> iter = list.iterator();
        application_info z ;
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
    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
      
        int port_udp ;
        String id;
        api = new Messenger_api();
        System.out.println("GIVE A PORT FOR UDP CONNECTION : ");
        port_udp = sc.nextInt();
        api.set_udp_port(port_udp);
        System.out.println("GIVE YOUR NAME  :  ");
        id = sc.next();
        

        System.out.println("##############################\nFor join in a group type J name_og_group\nFor send a message type S group_number message num where num is 0-->fifo ,1-->catoc\n For unjoin type U group_number\nFor exit type X\n###################");


        

        api.init_messenger();
        Scanner scaner = new Scanner(System.in);
        while(true){
            
            String input = scaner.nextLine();
            String[] words=input.split(" ");
        switch(words[0])
            {
                case "J":
                    join_fd = 0;
                    join_fd = api.grp_join(words[1], id);
                    if(join_fd == -1){
                        System.out.println("This name already exists in"+words[1]+" group");
                        return;
                    }else{
                        // add new node on teams
                        application_info new_team = new application_info();
                        new_team.setTeam_code(join_fd);
                        new_team.thread_for_team= new Thread(new Runnable(){public void run() {new Messenger_app(join_fd).view_team_change();}});
                        new_team.thread_for_team.start();
                
                        new_team.thread_for_receive= new Thread(new Runnable(){public void run() {new Messenger_app(join_fd).group_receive_thread();}});
                        new_team.thread_for_receive.start();
                        lock_main.lock();
                        teams.add(new_team);
                        lock_main.unlock();
                      
                    }
                    System.out.println("You joined on "+words[1]+" with team code : "+ join_fd);
                    break;
                case "S":
                    System.out.println("Sending "+ words[2]);
                    api.grp_send(Integer.parseInt(words[1]) , words[2].getBytes(), words[2].getBytes().length ,  Integer.parseInt(words[3]) );
                    break;
                case "U":
                    System.out.println("Leaving from team ");

                    api.grp_leave(Integer.parseInt(words[1]));
                    lock_main.lock();
                    teams.get(indexof_app(Integer.parseInt(words[1]), teams)).thread_for_team.interrupt();
                  
                    Iterator<application_info> app_iter = teams.iterator();
                    application_info remove_team_app;
                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    while(app_iter.hasNext()){
                        remove_team_app = app_iter.next();
                        if(remove_team_app.getTeam_code() ==Integer.parseInt(words[1]) ){
                            app_iter.remove();
                            break;
                        }

                    }
                    lock_main.unlock();
                    System.out.println("bghka");
                    break;
                case "X":
                    end=1;
                    break;
                default:
                    System.out.println("Invalid input.Try again ....");
            }
            if(end == 1){
                break;
            }
        }


        }
    


}