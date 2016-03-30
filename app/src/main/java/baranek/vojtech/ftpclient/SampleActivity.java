package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;

/**
 * Created by selim_tekinarslan on 10.10.2014.
 */
public class SampleActivity extends Activity {
    private static final String TAG = "SampleActivity";
    private static final String FILE_PATH = "filepath";
    private PdfFragment fragment;
    private static Context context;

    private String path = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = SampleActivity.this;
        path = getIntent().getStringExtra("path");
        openPdfWithFragment();

    }

    public void openPdfWithFragment() {
        fragment = new PdfFragment();
        Bundle args = new Bundle();
        args.putString(FILE_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + path);
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

}
