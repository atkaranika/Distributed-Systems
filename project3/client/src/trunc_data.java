public class trunc_data {
    int seqno;
    int return_value;
    int codename;
    int messages ;
    
    public int getCodename() {
        return codename;
    }
    public void setCodename(int codename) {
        this.codename = codename;
    }
    trunc_data(){
        return_value = -10;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public int getReturn_value() {
        return return_value;
    }
    public void setReturn_value(int return_value) {
        this.return_value = return_value;
    }

    
}