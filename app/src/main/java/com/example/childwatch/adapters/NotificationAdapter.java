package com.example.childwatch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.childwatch.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public static class NotiItem {
        public String app;
        public String title;
        public String text;
        public String time;
        public NotiItem(String app,String title,String text,String time){
            this.app=app;this.title=title;this.text=text;this.time=time;
        }
    }

    private List<NotiItem> data;

    public NotificationAdapter(List<NotiItem> data){
        this.data=data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType){
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h,int pos){
        NotiItem n=data.get(pos);
        h.app.setText(n.app);
        h.title.setText(n.title);
        h.text.setText(n.text);
        h.time.setText(n.time);
    }

    @Override
    public int getItemCount(){ return data.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView app,title,text,time;
        public ViewHolder(View v){
            super(v);
            app=v.findViewById(R.id.noti_app);
            title=v.findViewById(R.id.noti_title);
            text=v.findViewById(R.id.noti_text);
            time=v.findViewById(R.id.noti_time);
        }
    }
}
