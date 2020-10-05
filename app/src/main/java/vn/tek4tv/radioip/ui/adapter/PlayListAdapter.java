package vn.tek4tv.radioip.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.tek4tv.radioip.R;
import vn.tek4tv.radioip.model.Playlist;

public class PlayListAdapter extends  RecyclerView.Adapter<PlayListAdapter.ViewHolder>{
    private Context context;
    public void setLstDevices(List<Playlist> lstDevices) {
        this.lstDevices = lstDevices;
    }

    private List<Playlist> lstDevices;
    private OnChooseDevice onChooseDevice;
    public  interface OnChooseDevice{
        void onChooseDevice(Playlist loginDevice);
    }

    public PlayListAdapter(Context context, List<Playlist> lstDevices, OnChooseDevice onChooseDevice) {
        this.context = context;
        this.lstDevices = lstDevices;
        this.onChooseDevice = onChooseDevice;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return  new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_play_list, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist loginDevice = lstDevices.get(position);
        if(loginDevice.isCheck()){
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorAccent, null));
        }else{
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorWhite, null));
        }
        holder.tvstart.setText(loginDevice.getStart());
        holder.tvDes.setText(loginDevice.getName());
        holder.itemView.setOnClickListener(view -> {
            onChooseDevice.onChooseDevice(loginDevice);
        });
    }

    @Override
    public int getItemCount() {
        return lstDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvstart, tvDes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvstart =(TextView)itemView.findViewById(R.id.tvstart);
            tvDes =(TextView)itemView.findViewById(R.id.tvDes);
        }
    }
}
