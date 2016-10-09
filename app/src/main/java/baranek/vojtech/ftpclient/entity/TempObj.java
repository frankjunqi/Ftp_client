package baranek.vojtech.ftpclient.entity;

import java.io.Serializable;

/**
 * Created by Frank on 16/10/9.
 */
public class TempObj implements Serializable {
    public String __type;
    public Temp Data = new Temp();
    public String Msg;
    public String MsgList;
    public String Status;
}
