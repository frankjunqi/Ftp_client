package baranek.vojtech.ftpclient;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Created by Farmas on 17.10.2015.
 * <p/>
 * Custom adapter for RecylclerView for displaying files
 */
public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.CustomViewHolder> {

    private int posicionie;
    private FTPFile[] files;
    private Context c;
    private MainActivity mAct = null;

    public MyRecyclerAdapter(Context c, FTPFile[] files, MainActivity mAct) {
        this.c = c;
        this.files = files;
        this.mAct = mAct;

    }

    @Override
    public MyRecyclerAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View View = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row, null);
        CustomViewHolder viewHolder = new CustomViewHolder(View);
        return viewHolder;


    }

    @Override
    public void onBindViewHolder(final MyRecyclerAdapter.CustomViewHolder holder, final int position) {
        /**
         * Set different images for folder / directory
         */
        if (files[position].isDirectory())
        /**
         * Is directory
         */ {
            holder.imageView.setBackgroundResource(R.drawable.ic_ftp_launcher);
        } else {
            /**
             * is file
             */
            holder.imageView.setBackgroundResource(R.drawable.ic_ftp_launcher);
        }

        holder.tv1.setText(String.valueOf(files[position].getName()));

        /**
         * Actions for long click on directory
         */
        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (files[position].isDirectory())
                /**
                 * Open bottom sheet for directory
                 */ {
                    OpenBottomSheetMenuForDir(position);
                }
                return false;
            }
        };


        holder.tv1.setOnLongClickListener(longClickListener);
        holder.imageView.setOnLongClickListener(longClickListener);

        holder.imageView.setTag(holder);
        holder.tv1.setTag(holder);


    }

    /**
     * Bottom sheet for directories actions
     *
     * @param pos position of clicked directory
     */

    private void OpenBottomSheetMenuForDir(int pos) {
        posicionie = pos;
    }

    /**
     * Open bottom sheet for files options
     *
     * @param pos position of file
     */

    private void OpenBottomSheetMenu(int pos) {

        posicionie = pos;


    }

    /**
     * Show dialog for download method
     */
    private void ShowDownloadDialog() {

    }

    private EditText etNovyNazev;
    private String strNovyNazev;

    /**
     * Show dialog for rename, get name and rename
     */

    private void ShowRenameDialog() {


    }

    @Override
    public int getItemCount() {
        int i = files.length;
        return i;
    }


    /**
     * Custom view holder for row
     */

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        protected TextView tv1;
        protected ImageView imageView;

        public CustomViewHolder(View itemView) {
            super(itemView);
            this.tv1 = (TextView) itemView.findViewById(R.id.title);
            this.imageView = (ImageView) itemView.findViewById(R.id.type);
        }
    }

}

