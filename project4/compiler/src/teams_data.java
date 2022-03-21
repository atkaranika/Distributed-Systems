import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;

public class teams_data implements Serializable {
    private int team_code;
    private List<members_data> members;
    teams_data(){
        this.members = new LinkedList<>();
        
    }
    public int getTeam_code() {
        return team_code;
    }
    public void setTeam_code(int team_code) {
        this.team_code = team_code;
    }
    public List<members_data> getMembers() {
        return members;
    }
    public void setMembers(List<members_data> members) {
        this.members = members;
    }
    
}
