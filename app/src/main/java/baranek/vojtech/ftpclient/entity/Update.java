package baranek.vojtech.ftpclient.entity;

import java.io.Serializable;

/**
 * Created by Frank on 16/10/9.
 */
public class Update implements Serializable {

    public String IsLast;
    public String Ver;
    public String UpdateContent;
    public String UpdateTime;
    public String FileSize;
    public String FileUrl;

}
