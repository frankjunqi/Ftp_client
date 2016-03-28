package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class MainActivity extends Activity {
    @Bind(R.id.etCesta)
    TextView etCesta;
    @Bind(R.id.btnPridatAdr)
    Button btnPridatAdr;
    @Bind(R.id.btnNahratSoubor)
    Button btnNahratSoubor;

    private String FTP_HOST = "27.54.248.35", FTP_USER = "MES", FTP_PASS = "";
    @Bind(R.id.recyclerViewMain)
    RecyclerView recyclerViewMain;
    @Bind(R.id.progress)
    MaterialProgressBar progress;

    private MyRecyclerAdapter adapter;
    private FTPFile[] files;
    private String strPath = "/EWI";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        recyclerViewMain.setLayoutManager(new LinearLayoutManager(this));
        btnPridatAdr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadFile("merge_request.pdf");
            }
        });
        btnNahratSoubor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MBActivity.class);
                startActivity(intent);
            }
        });

    }

    private void changeButtonsEnability(boolean val) {
        btnNahratSoubor.setEnabled(val);
        btnPridatAdr.setEnabled(val);
    }


    /**
     * Download file method
     */
    private void DownloadFile(String strDirName) {
        new AssyncFtpTaskActions().execute("DOWNLOAD", strDirName);
    }


    /**
     * Assync task for ftp directory browsing
     */
    public class AssyncFtpTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            FTPClient mFTPClient = new FTPClient();
            Integer result = 0;

            try {

                /**
                 Establish connection to ftp, set passive mod
                 */
                mFTPClient.setControlEncoding("UTF-8");
                mFTPClient.connect(FTP_HOST, 21);
                mFTPClient.login(FTP_USER, FTP_PASS);
                mFTPClient.enterLocalPassiveMode();

                if (params[0].equals("/..")) {
                    /**
                     Move to previous directory
                     */
                    int i = 0;
                    i = strPath.lastIndexOf("/");
                    strPath = strPath.substring(0, i);
                } else {
                    /**
                     Open directory
                     */
                    strPath = strPath + "/" + params[0];
                }
                mFTPClient.changeWorkingDirectory(strPath);
                strPath = mFTPClient.printWorkingDirectory();
                files = mFTPClient.listFiles();
                result = 1;
                mFTPClient.disconnect();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        /**
         * Show progress bar
         */
        @Override
        protected void onPreExecute() {
            recyclerViewMain.animate().alpha(0.5f);
            progress.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(true);
        }

        /**
         * Hide progress bar, show files, if successful, show toast if not.
         */
        @Override
        protected void onPostExecute(Integer integer) {
            progress.setVisibility(View.GONE);
            recyclerViewMain.animate().alpha(1.0f);
            if (integer == 1) {
                adapter = new MyRecyclerAdapter(getApplicationContext(), files, MainActivity.this);
                recyclerViewMain.setAdapter(adapter);
                etCesta.setText(strPath);
                changeButtonsEnability(true);

            } else {
                Toast.makeText(getApplicationContext(), R.string.data_load_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Assync task  for operations with files
     */
    public class AssyncFtpTaskActions extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            FTPClient mFTPClient = new FTPClient();
            Integer result = 0;

            try {
                /**
                 Establish connection
                 */
                mFTPClient.setControlEncoding("UTF-8");
                mFTPClient.connect(FTP_HOST, 6888);
                mFTPClient.login(FTP_USER, FTP_PASS);
                mFTPClient.enterLocalPassiveMode();
                mFTPClient.changeWorkingDirectory(strPath);

                /**
                 Action with files
                 */

                switch (params[0]) {
                    case "DELETE": {
                        mFTPClient.deleteFile(params[1]);

                        break;
                    }
                    case "RENAME": {
                        mFTPClient.rename(params[1], params[2]);
                        break;
                    }
                    case "DOWNLOAD": {
                        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "EWI");
                        file.mkdir();
                        File pdffile = new File(Environment.getExternalStorageDirectory() + File.separator + "EWI" + File.separator + params[1]);
                        if(pdffile.exists()){
                            break;
                        }
                        pdffile.createNewFile();
                        FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + "EWI" + File.separator + params[1]);
                        mFTPClient.retrieveFile(strPath + "/" + params[1], fos);
                        fos.flush();
                        fos.close();
                        break;
                    }
                    case "DELETEDIR": {
                        boolean rem = mFTPClient.removeDirectory(params[1]);

                        break;
                    }
                    case "CREATEDIR": {

                        if (mFTPClient.changeWorkingDirectory(params[1])) {
                            strPath = strPath + "/" + params[1];
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), R.string.folderalreadyexists, Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            mFTPClient.makeDirectory(params[1]);
                        }
                        break;
                    }
                    case "UPLOADFILE": {
                        File f = new File(params[1]);
                        InputStream input = new FileInputStream(f);
                        if (mFTPClient.getReplyCode() != 550) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), R.string.filealreadyExiists, Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            mFTPClient.storeFile(strPath + "/" + f.getName().toString(), input);
                        }
                        break;
                    }
                }
                files = mFTPClient.listFiles();
                result = 1;

                mFTPClient.disconnect();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        /**
         * show progress bar
         */

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(true);
            recyclerViewMain.animate().alpha(0.5f);
        }

        /**
         * Show changed files or fail message
         */

        @Override
        protected void onPostExecute(Integer integer) {
            progress.setVisibility(View.GONE);
            recyclerViewMain.animate().alpha(1.0f);
            if (integer == 1) {

                adapter = new MyRecyclerAdapter(getApplicationContext(), files, MainActivity.this);
                recyclerViewMain.setAdapter(adapter);
                etCesta.setText(strPath);

                Intent intent = new Intent(MainActivity.this, SampleActivity.class);
                startActivity(intent);

            } else {
                Toast.makeText(getApplicationContext(), R.string.action_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }


}
