package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

import database.FSStoreFetcher;
import model.Profile;
import util.AppUtils;
import util.Storage;

public class BidderViewFragment extends Fragment {
    private ArrayList<LogItem> items = new ArrayList<>();
    private TextView nothingHereView;
    private RecyclerView recyclerView;

    public static BidderViewFragment newInstance(Context context){
        return new BidderViewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_category_view, container, false);
        nothingHereView = v.findViewById(R.id.nothing_here_view);
        recyclerView = v.findViewById(R.id.category_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if(Storage.postableItem.logs != null){
            for(Map.Entry<String, Integer> entry : Storage.postableItem.logs.entrySet()){
                LogItem item = new LogItem(entry.getKey(), entry.getValue());
                // simple sort algorithm
                if(items.isEmpty()) {
                    items.add(item);
                }else{
                    if(item.bid > items.get(0).bid){
                        items.add(0, item);
                    }else{
                        items.add(item);
                    }
                }
            }
        }

        if(items.isEmpty()){
            nothingHereView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }else {
            nothingHereView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            LogItemAdapter logItemAdapter = new LogItemAdapter();
            recyclerView.setAdapter(logItemAdapter);
        }
        return v;
    }

    private class LogItemHolder extends RecyclerView.ViewHolder{
        private final View backgroundView;
        private final ImageView successIndicatorView;
        private final TextView nameView;
        private final TextView amountView;

        public LogItemHolder(View view){
            super(view);
            backgroundView = itemView.findViewById(R.id.linear_container);
            successIndicatorView = itemView.findViewById(R.id.success_state_view);
            nameView = itemView.findViewById(R.id.name_view);
            amountView = itemView.findViewById(R.id.bid_label);
            nameView.setText(R.string.loading_dot);
        }

        public void bindLogItem(LogItem item){
            LogItemHolder.ProfileFetcher profileFetcher = new LogItemHolder.ProfileFetcher(item.id);
            profileFetcher.query(null);
            amountView.setText("UGX " + Integer.toString(item.bid));
            if(item.bid == Storage.postableItem.topBid){
                backgroundView.setBackgroundColor(getResources().getColor(R.color.customblue));
                successIndicatorView.setImageResource(R.drawable.ic_baseline_check_circle_outline_24);
            }else {
                backgroundView.setBackgroundColor(getResources().getColor(R.color.customred));
                successIndicatorView.setImageResource(R.drawable.ic_baseline_close_circle_24);
            }
        }

        private class ProfileFetcher extends FSStoreFetcher<Profile> {
            private boolean found;
            private final String queryId;
            private String name = null;

            public ProfileFetcher(String id){
                super(AppUtils.MODEL_PROFILE, Profile.class);
                this.queryId = id;
            }

            @Override
            public void onStartFetch(){
                found = false;
            }

            @Override
            public boolean validateCondition(Profile profile){
                return profile.getId().equals(queryId);
            }

            @Override
            public void onFind(Profile profile){
                found = true;
                name = profile.getName();
            }

            @Override
            public boolean endFetchCondition(){
                return found;
            }

            @Override
            public void onSucceed(){
                if(nameView != null) {
                    nameView.setText(name);
                }
            }
        }
    }

    private class LogItemAdapter extends RecyclerView.Adapter<LogItemHolder>{

        @Override
        public LogItemHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View view = LayoutInflater.from(getActivity()).
                    inflate(R.layout.log_item_layout, parent, false);
            return new LogItemHolder(view);
        }

        @Override
        public int getItemCount(){
            return items.size();
        }

        @Override
        public void onBindViewHolder(LogItemHolder holder, int position){
            holder.bindLogItem(items.get(position));
        }
    }

    private static class LogItem{
        public String id;
        public Integer bid;

        public LogItem(String id, Integer bid){
            this.id = id;
            this.bid = bid;
        }
    }
}
