package baranek.vojtech.ftpclient;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.net.ftp.FTPFile;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Farmas on 17.10.2015.
 * <p/>
 * Custom adapter for RecylclerView for displaying files
 */
public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.CustomViewHolder> {

    private ArrayList<FTPFile> files = new ArrayList<>();
    private Context context;
    private FileClickListen fileClickListen;
    private FTPFile defaultFTPFile;

    private int currentPostion = 0;// 默认选中项目

    public interface FileClickListen {
        void filePdfClick(String pdfPath);
    }

    public MyRecyclerAdapter(Context context) {
        this.context = context;
    }

    public void setFiles(FTPFile[] files) {
        this.files.clear();
        if (defaultFTPFile != null) {
            this.files.add(defaultFTPFile);
        }
        for (int i = 0; files != null && i < files.length; i++) {
            this.files.add(files[i]);
        }
        notifyDataSetChanged();
    }

    public void setDefaultFTPFile(FTPFile defaultFTPFile) {
        this.defaultFTPFile = defaultFTPFile;
    }

    public void setOnClickListener(FileClickListen fileClickListen) {
        this.fileClickListen = fileClickListen;
    }

    @Override
    public MyRecyclerAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View View = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row, null);
        CustomViewHolder viewHolder = new CustomViewHolder(View);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final MyRecyclerAdapter.CustomViewHolder holder, final int position) {
        String filename = files.get(position).getName();
        final String link = files.get(position).getLink();

        holder.tv1.setText(String.valueOf(filename.substring(0, filename.lastIndexOf("."))));

        if (position == currentPostion) {
            holder.tv1.setBackgroundResource(R.drawable.bg_tab_sel);
            holder.tv1.setTextColor(context.getResources().getColor(R.color.main_white));
        } else {
            holder.tv1.setBackgroundResource(R.drawable.bg_tab);
            holder.tv1.setTextColor(context.getResources().getColor(R.color.title_dark));
        }

        holder.tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPostion = position;
                if (fileClickListen != null) {
                    fileClickListen.filePdfClick(link);
                }
                notifyDataSetChanged();
            }
        });
        holder.tv1.setTag(holder);
    }

    @Override
    public int getItemCount() {
        if (files == null) {
            return 0;
        }
        int i = files.size();
        return i;
    }

    /**
     * Custom view holder for row
     */

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        protected TextView tv1;

        public CustomViewHolder(View itemView) {
            super(itemView);
            this.tv1 = (TextView) itemView.findViewById(R.id.title);
        }
    }

}

