package com.scit.stauc;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import database.SQLGateway;
import model.SearchFilter;

public class SearchFiltersSavedFragment extends Fragment {
    private RecyclerView savedRecyclerView;
    private TextView nothingTextView;
    private SearchFilterAdapter searchFilterAdapter;
    private ArrayList<SearchFilter> searchFilters = new ArrayList<>();

    public static SearchFiltersSavedFragment newInstance(){
        return new SearchFiltersSavedFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_search_filters_saved, container, false);
        savedRecyclerView = v.findViewById(R.id.saved_recycler_view);
        nothingTextView = v.findViewById(R.id.nothing_here_view);
        savedRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        searchFilterAdapter = new SearchFilterAdapter(getActivity());

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();
        handleFilterView();
    }

    private void handleFilterView(){
        searchFilters = SQLGateway.get(getActivity()).getSearchFilters();
        if(searchFilters.isEmpty()){
            nothingTextView.setVisibility(View.VISIBLE);
            savedRecyclerView.setVisibility(View.GONE);
            return;
        }else{
            searchFilterAdapter.notifyDataSetChanged();
        }
        nothingTextView.setVisibility(View.GONE);
        savedRecyclerView.setVisibility(View.VISIBLE);
        savedRecyclerView.setAdapter(searchFilterAdapter);
    }

    private class SearchFilterHolder extends RecyclerView.ViewHolder{
        private final TextView mNameTextView;
        private SearchFilter searchFilter;

        public SearchFilterHolder(View view){
            super(view);
            mNameTextView = itemView.findViewById(R.id.name_view);
            View deleteView = itemView.findViewById(R.id.delete_filter_view);
            deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(searchFilter != null){
                        SQLGateway.get(getActivity()).deleteSearchFilter(searchFilter);
                        searchFilterAdapter.notifyDataSetChanged();
                    }
                }
            });
            mNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(getActivity() != null) {
                        ((SearchFiltersEditFragment.ActivityHelper) getActivity()).
                                scrollToTab(0, searchFilter);
                    }
                }
            });
        }

        public void bindSearchFilter(SearchFilter searchFilter){
            mNameTextView.setText(searchFilter.getName());
            this.searchFilter = searchFilter;
        }

    }

    private class SearchFilterAdapter extends RecyclerView.Adapter<SearchFilterHolder>{
        private final Context context;

        public SearchFilterAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getItemCount(){
            return searchFilters.size();
        }

        @NonNull
        @Override
        public SearchFilterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.search_filter_item,
                    parent, false);
            return new SearchFilterHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchFilterHolder holder, int position) {
            holder.bindSearchFilter(searchFilters.get(position));
        }

    }

}
