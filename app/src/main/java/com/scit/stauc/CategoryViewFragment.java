package com.scit.stauc;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.PostableItem;
import util.AdapterUtil;
import util.AppUtils;
import util.Storage;

public class CategoryViewFragment extends Fragment {
    private static final String TAG = "CategoryFragment";
    private static final String CATEGORY_TITLE = "Category.Extra";
    private static final String CATEGORY_INDEX = "Category.Index";
    private String categoryString;
    private int categoryIndex = 0;
    private RecyclerView recyclerView;
    private TextView nothingHereView;
    private List<PostableItem> mItems = new ArrayList<>();

    public static CategoryViewFragment newInstance(String category, int categoryIndex){
        Bundle args = new Bundle();
        args.putString(CATEGORY_TITLE, category);
        args.putInt(CATEGORY_INDEX, categoryIndex);
        CategoryViewFragment fragment = new CategoryViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            categoryIndex = getArguments().getInt(CATEGORY_INDEX, 0);
        }
        if(categoryIndex < PostableItem.CATEGORY.values().length){
            categoryString = PostableItem.CATEGORY.values()[categoryIndex].toString().toLowerCase();
        }else{
            categoryString = getArguments().getString(CATEGORY_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_category_view, container, false);

        String hint = "Search " + AppUtils.capitalize(categoryString);
        // searchView.setQueryHint(hint);

        recyclerView = v.findViewById(R.id.category_recycler);
        nothingHereView = v.findViewById(R.id.nothing_here_view);
        if(mItems.isEmpty()){
           nothingHereView.setVisibility(View.VISIBLE);
           recyclerView.setVisibility(View.GONE);
        }else{
            nothingHereView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        CategoryItemAdapter parentAdapter = new CategoryItemAdapter(getActivity(), mItems);
        recyclerView.setAdapter(parentAdapter);
        if(categoryIndex < PostableItem.CATEGORY.values().length) {
            filterByCategory(categoryString);
        }else{
            if(categoryString.toLowerCase().contains("top")){
                mItems = HomeFragment.getTopPicks();
            }else{
                mItems = HomeFragment.getRecentPicks();
            }
            updateRecycler();
        }
        return v;
    }

    private class CategoryItemAdapter extends AdapterUtil.PostableItemAdapter {
        public CategoryItemAdapter(Context context, List<PostableItem> items){
            super(context, items);
        }
    }

    private void filterByCategory(String categoryString){
        mItems = new ArrayList<>();
        for(PostableItem item : Storage.postableItems){
            if(item.category.toLowerCase().equals(categoryString.toLowerCase())){
                mItems.add(item);
            }
        }
        updateRecycler();
    }

    private void updateRecycler(){
        if(recyclerView != null && recyclerView.getAdapter() != null) {
            CategoryItemAdapter parentAdapter = new CategoryItemAdapter(getActivity(), mItems);
            recyclerView.setAdapter(parentAdapter);
        }
        if(mItems.isEmpty()){
            nothingHereView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }else{
            nothingHereView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

}
