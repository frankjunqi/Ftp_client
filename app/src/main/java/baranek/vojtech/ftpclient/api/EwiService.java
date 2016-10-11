package baranek.vojtech.ftpclient.api;

import baranek.vojtech.ftpclient.entity.EwiResBody;
import baranek.vojtech.ftpclient.entity.TempResBody;
import baranek.vojtech.ftpclient.entity.UpdaeResBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by kjh08490 on 2016/3/16.
 */
public interface EwiService {

    @GET("/{srv}/{svc}/{queryname}")
    Call<EwiResBody> allMachineList(
            @Path("srv") String srv,
            @Path("svc") String svc,
            @Path("queryname") String queryname,
            @Query("deviceID") String deviceID);

    @GET("/{srv}/{svc}/{queryname}")
    Call<TempResBody> tempRequest(
            @Path("srv") String srv,
            @Path("svc") String svc,
            @Path("queryname") String queryname
    );

    @GET("/{srv}/{svc}/{queryname}")
    Call<UpdaeResBody> updateVersion(
            @Path("srv") String srv,
            @Path("svc") String svc,
            @Path("queryname") String queryname,
            @Query("Code") String code,
            @Query("Ver") String ver
    );

}
