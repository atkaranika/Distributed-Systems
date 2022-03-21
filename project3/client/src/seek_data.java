public class seek_data {
    
    private int seqno;
    private int file_length;
    private int fileCode ;
    public  int messages ;
    
    public int getMessages() {
        return messages;
    }
    public void setMessages(int messages) {
        this.messages = messages;
    }
    public int getFileCode() {
        return fileCode;
    }
    public void setFileCode(int fileCode) {
        this.fileCode = fileCode;
    }
    public seek_data() {
        seqno = -1;
        file_length=-1;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public int getFile_length() {
        return file_length;
    }
    public void setFile_length(int file_length) {
        this.file_length = file_length;
    }
    
}