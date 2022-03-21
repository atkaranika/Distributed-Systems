import java.util.LinkedList;
import java.util.List;

public class messenger_teams {
    public int current_view;
    public int next_view;
    public List<Member_info_send>  current_team;
    public List<Member_info_send> next_team;
    public List<mids>  mids_list; 
    public List<deliver> mbuf;//messages for next view
    public List<deliver> delivered;//messages for deliver check//before fifo or catoc
    public List<deliver> app_delivered;//messages for the app after fifo and catoc
    public List<mids> taken_messages;//messages that the app already have taken we want to be equal with vids when we change view
    public List<mids> dpid; //last message of each message //delete this on change view
    public List<mids> vids; //messages that i have send in a view
    public int  team_code;
    public int counter ;
    public String group_name ;
    public int seqno;//reset in each view
    messenger_teams(){
        counter = 0 ;
        current_team= new LinkedList<>() ;
        next_team= new LinkedList<>() ;
        mids_list= new LinkedList<>() ; 
        mbuf= new LinkedList<>() ;//messages for next view
        taken_messages = new LinkedList<>() ;
        app_delivered = new LinkedList<>() ;
        delivered= new LinkedList<>() ;
        dpid = new LinkedList<>() ;
        vids = new LinkedList<>(); //messages that i ha
        seqno = 0;
    }
    
    public int getCurrent_view() {
        return current_view;
    }
    public void setCurrent_view(int current_view) {
        this.current_view = current_view;
    }
    public int getNext_view() {
        return next_view;
    }
    public void setNext_view(int next_view) {
        this.next_view = next_view;
    }
    public int getTeam_code() {
        return team_code;
    }
    public void setTeam_code(int team_code) {
        this.team_code = team_code;
    }
    public List<Member_info_send> getCurrent_team() {
        return current_team;
    }
    public void setCurrent_team(List<Member_info_send> current_team) {
        this.current_team = current_team;
    }
    public List<Member_info_send> getNext_team() {
        return next_team;
    }
    public void setNext_team(List<Member_info_send> next_team) {
        this.next_team = next_team;
    }
    public List<mids> getMids_list() {
        return mids_list;
    }
    public void setMids_list(List<mids> mids_list) {
        this.mids_list = mids_list;
    }
    public List<deliver> getMbuf() {
        return mbuf;
    }
    public void setMbuf(List<deliver> mbuf) {
        this.mbuf = mbuf;
    }
    public List<deliver> getDelivered() {
        return delivered;
    }
    public void setDelivered(List<deliver> delivered) {
        this.delivered = delivered;
    }
    public List<mids> getVids() {
        return vids;
    }
    public void setVids(List<mids> vids) {
        this.vids = vids;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + team_code;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        messenger_teams other = (messenger_teams) obj;
        if (team_code != other.team_code)
            return false;
        return true;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public int getSeqno() {
        return seqno;
    }

    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }

    public List<deliver> getApp_delivered() {
        return app_delivered;
    }

    public void setApp_delivered(List<deliver> app_delivered) {
        this.app_delivered = app_delivered;
    }

    public List<mids> getTaken_messages() {
        return taken_messages;
    }

    public void setTaken_messages(List<mids> taken_messages) {
        this.taken_messages = taken_messages;
    }

    public List<mids> getDpid() {
        return dpid;
    }

    public void setDpid(List<mids> dpid) {
        this.dpid = dpid;
    }
    
    
}