public class thread_data {
    private int team_code;
    private int thread_code;
    private String run_command;
    private int err_line;
    private long position;
    public int getTeam_code() {
        return team_code;
    }
    public void setTeam_code(int team_code) {
        this.team_code = team_code;
    }
    public int getThread_code() {
        return thread_code;
    }
    public void setThread_code(int thread_code) {
        this.thread_code = thread_code;
    }
    public String getRun_command() {
        return run_command;
    }
    public void setRun_command(String run_command) {
        this.run_command = run_command;
    }
    public int getErr_line() {
        return err_line;
    }
    public void setErr_line(int err_line) {
        this.err_line = err_line;
    }
    public long getPosition() {
        return position;
    }
    public void setPosition(long position) {
        this.position = position;
    }
    
}
