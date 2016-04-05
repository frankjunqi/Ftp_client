package baranek.vojtech.ftpclient.entity;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by kjh08490 on 2016/3/24.
 */
public class EwiObj implements Serializable {
    public String __type;
    public Ewi Data = new Ewi();
    public String Status;
    public String Msg;
    public ArrayList<String> MsgList = new ArrayList<>();
}
