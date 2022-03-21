/*
A class for each group that the Manager serve and the info about the members of the group the team code and name and the current view
*/

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;


public class Group_info  implements Serializable{
    String name_of_group;
    int team_code;
    int view_number;
    private   LinkedList<Member_info> member_list = new LinkedList<>();
    private   LinkedList<Member_info_send> member_list_send = new LinkedList<>();



    public String getName_of_group() {
        return name_of_group;
    }

    public void setName_of_group(String name_of_group) {
        this.name_of_group = name_of_group;
    }

    public LinkedList<Member_info> getGroup_list() {
        return member_list;
    }

    public void setGroup_list(LinkedList<Member_info> group_list) {
        this.member_list = group_list;
    } 
    public void add_member(Member_info new_member){
        this.member_list.addLast(new_member);
    }
    public void remove_member(Member_info remove_member){

        Iterator <Member_info> iter= member_list.iterator() ;
        Member_info check_member  ; 
        while(iter.hasNext()){
            check_member = iter.next();
            if(check_member.getAddress().equals(remove_member.getAddress()) && check_member.getPort() == remove_member.getPort()){
                iter.remove();
            }

        }

    }

    public  LinkedList<Member_info_send> getMember_list_send() {
        return member_list_send;
    }

    public  void setMember_list_send(LinkedList<Member_info_send> group_list) {
        this.member_list_send = group_list;
    } 
    public  void add_member_send(Member_info_send new_member){
        this.member_list_send.addLast(new_member);
        Iterator<Member_info_send> iter = this.member_list_send.iterator();
        Member_info_send k ;
        System.out.println("-------size : "+this.member_list_send.size());
        while(iter.hasNext()){
            k = iter.next();
            System.out.println("---------list port : "+k.getPort()+" udp port"+k.getUdp_port());
        }

    }

    public  void remove_member_send(Member_info_send remove_member){

        Iterator <Member_info_send> iter= member_list_send.iterator() ;
        Member_info_send check_member  ; 
        while(iter.hasNext()){
            check_member = iter.next();
            if(check_member.getAddress().equals(remove_member.getAddress()) && check_member.getPort() == remove_member.getPort()){
                iter.remove();
            }

        }

    }

    public int getView_number() {
        return view_number;
    }

    public void setView_number(int view_number) {
        this.view_number = view_number;
    }

    public int getTeam_code() {
        return team_code;
    }

    public void setTeam_code(int team_code) {
        this.team_code = team_code;
    }

    
}