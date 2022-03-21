import java.util.List;

public class application_info {
    public Thread thread_for_team;
    public Thread thread_for_receive;
    private int team_code;
    public List<Member_info_send>  current_team;

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
    
    
}
