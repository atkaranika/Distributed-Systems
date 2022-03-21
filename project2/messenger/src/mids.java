import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class mids  implements Serializable{
    private String id;
    private int seqno ;
    private List <String>members_send;

    public mids(){
        members_send= new LinkedList<String>();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public int equals(mids mid){
        if(id.equals(mid.getId()) && mid.getSeqno() == this.seqno){
            return 1 ;
        }
        return 0 ;

    }
    // public String hashcode(){
    //     return this.id+this.seqno;
    // }
    public void add_member(String member_id){
        members_send.add(member_id);
    }
    public List<String> getMembers_send() {
        return members_send;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + seqno;
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
		mids other = (mids) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (seqno != other.seqno)
			return false;
		return true;
	}


    
}