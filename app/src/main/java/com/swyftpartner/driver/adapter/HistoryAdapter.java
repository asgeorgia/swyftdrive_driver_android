package com.swyftpartner.driver.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.model.History;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by user on 1/20/2017.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.typesViewHolder> {

    private Activity mContext;
    private List<History> itemshistroyList;
    SimpleDateFormat simpleDateFormat;
    SimpleDateFormat inputformat;

    public HistoryAdapter(Activity context, List<History> itemshistroyList) {
        mContext = context;
        simpleDateFormat = new SimpleDateFormat("E, MMM, dd, yyyy hh:mm a");
        inputformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
       /* simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+05:30"));
        inputformat.setTimeZone(TimeZone.getTimeZone("GMT"));*/

        this.itemshistroyList = itemshistroyList;

    }

    @Override
    public HistoryAdapter.typesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.history_item, null);
        HistoryAdapter.typesViewHolder holder = new HistoryAdapter.typesViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final HistoryAdapter.typesViewHolder holder, int position) {
        History history_itme = itemshistroyList.get(position);

        if(history_itme != null) {
            String hitory_Date ="";
            try {
                hitory_Date = history_itme.getHistory_date();
                Date date = inputformat.parse(hitory_Date);
                hitory_Date = simpleDateFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            holder.tv_history_car_type.setText(history_itme.getHistory_type());
            holder.tv_history_source.setText(history_itme.getHistory_Sadd());
            if(!history_itme.getHistory_Dadd().equals("")){
                holder.tv_history_destination.setText(history_itme.getHistory_Dadd());
            } else {
                holder.tv_history_destination.setText(mContext.getResources().getString(R.string.txt_not_avialbel));
            }

            holder.tv_history_total.setText(history_itme.getCurrency_unit()+" "+history_itme.getHistory_total());
            holder.tv_history_driver_name.setText(history_itme.getProvider_name());
            holder.tv_history_date.setText(hitory_Date);
            Glide.with(mContext).load(history_itme.getHistory_picture()).into(holder.iv_history_img);
        }

    }

    @Override
    public int getItemCount() {
        return itemshistroyList.size();
    }

    public class typesViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView iv_history_img;
        private TextView tv_history_date,tv_history_car_type,tv_history_source,tv_history_destination,tv_history_total,tv_history_driver_name;

        public typesViewHolder(View itemView) {
            super(itemView);
            iv_history_img = (CircleImageView) itemView.findViewById(R.id.iv_history_img);
            tv_history_date = (TextView) itemView.findViewById(R.id.tv_history_date);
            tv_history_car_type = (TextView) itemView.findViewById(R.id.tv_history_car_type);
            tv_history_source = (TextView) itemView.findViewById(R.id.tv_history_source);
            tv_history_destination = (TextView) itemView.findViewById(R.id.tv_history_destination);
            tv_history_total = (TextView) itemView.findViewById(R.id.tv_history_total);
            tv_history_driver_name = (TextView) itemView.findViewById(R.id.tv_history_driver_name);
        }
    }


}


