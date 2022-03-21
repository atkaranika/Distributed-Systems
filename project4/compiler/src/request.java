public class request {
    private int reqid ;  
    private String newFileName;
    private int acked ;
    private int port ;
    private teams_data migrate_team;
    public request() {
        migrate_team = new teams_data();
    }
    public int getReqid() {
        return reqid;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setReqid(int reqid) {
        this.reqid = reqid;
    }
    public String getNewFileName() {
        return newFileName;
    }
    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }
    public int getAcked() {
        return acked;
    }
    public void setAcked(int acked) {
        this.acked = acked;
    }
    public teams_data getMigrate_team() {
        return migrate_team;
    }
    public void setMigrate_team(teams_data migrate_team) {
        this.migrate_team = migrate_team;
    }
   
  
}