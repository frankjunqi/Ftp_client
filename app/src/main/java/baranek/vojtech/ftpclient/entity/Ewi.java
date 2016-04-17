package baranek.vojtech.ftpclient.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kjh08490 on 2016/3/24.
 */
public class Ewi implements Serializable {
    public String __type;
    public String MachineCode;
    public String PN;
    public String PO;// 订单号
    public String CustLogo;
    public String CustName;
    public String MachineName;
    public String FTPPath;
    public String LBPicPath;
    public String MachineDocPath;


    public ArrayList<String> Msg = new ArrayList<>();
}
