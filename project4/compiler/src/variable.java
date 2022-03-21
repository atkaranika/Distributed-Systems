import java.util.List;
import java.io.Serializable;
public class variable implements Serializable{
    public variable()   {
    
    }
    public variable(String value_string, String name) {
        this.value_string = value_string;
        this.name = name;
    }
    private String value_string;
    private String name;
 
     public String getName() {
         return name;
     }
     public void setName(String name) {
         this.name = name;
     }
     public String getValue_string() {
         return value_string;
     }
     public void setValue_string(String value_string) {
         this.value_string = value_string;
     }
      
     
     
 }