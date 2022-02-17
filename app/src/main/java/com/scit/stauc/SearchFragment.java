package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import database.FSStoreFetcher;
import database.SQLGateway;
import model.PostableItem;
import model.SearchFilter;
import model.SearchHistory;
import util.AdapterUtil;
import util.AppUtils;
import util.FirebaseHandleUtil;
import util.PreferenceUtils;
import util.Storage;
import util.TimeUtil;

import static util.AppUtils.hideKeyboard;

public class SearchFragment extends Fragment{
    private static final String TAG = "SearchFragment";
    public static final int FILTER_REQUEST_CODE = 23;
    private RecyclerView itemsRecycler;
    private TextView nothingHereView;
    private TextView searchHistoryTextView;
    private ChipGroup historyChipGroup;
    private SearchView mainSearchView;
    private View filterLabelLayout;
    private View filterDataLayout;
    private TextView advancedClickableView;
    private ImageView filterLabelImageView;
    private CheckBox ascendingStateToggleView;
    private RadioButton priceRadioButton;
    private RadioButton nameRadioButton;
    private RadioButton uploadDateRadioButton;
    private RadioButton endDateRadioButton;
    private LayoutInflater inflater;
    private ViewGroup container;
    private boolean onHistoryView;
    private SearchFilter searchFilter;
    private SearchItemAdapter searchItemAdapter;
    private ArrayList<SearchHistory> searchHistories = new ArrayList<>();
    private ArrayList<PostableItem> searchedItems = new ArrayList<>();
    private boolean filterLayoutOpen = false;
    private boolean ascending = false;

    public static SearchFragment newInstance(){
        return new SearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        onHistoryView = false;
        generateGenericFilter();
        searchHistories = SQLGateway.get(getActivity()).getSearchHistory();
    }

    public void generateGenericFilter(){
        StringBuilder cat = new StringBuilder();
        for (PostableItem.CATEGORY category : PostableItem.CATEGORY.values()){
            cat.append(category.toString()).append("_");
        }
        searchFilter = new SearchFilter(null,
                cat.toString(),
                AppUtils.MIN_ACCEPTED_CURRENCY_VALUE,
                AppUtils.MAX_ACCEPTED_CURRENCY_VALUE);
        searchFilter.setCheckedCode((byte)1); // ALL
    }

    private class PostableFetcher extends FSStoreFetcher<PostableItem> {
        public PostableFetcher(){
            super(AppUtils.MODEL_POSTABLE, PostableItem.class);
        }

        @Override
        public void onStartFetch(){
            Storage.itemsLoading = true;
            Storage.postableItems.clear();
        }

        @Override
        public boolean validateCondition(PostableItem item){
            boolean valid = FirebaseHandleUtil.filter(item);
            if(valid){
                for(int i=0; i<HomeFragment.categoryBitmapIds.length;i++){
                    if (HomeFragment.categories[i].toString().equals(item.category)){
                        if(HomeFragment.categoryBitmapIds[i] == null && item.images != null && !item.images.isEmpty()){
                            HomeFragment.categoryBitmapIds[i] = item.images.get(0);
                        }
                        break;
                    }
                }
            }
            return valid;
        }

        @Override
        public void onFail(){
            Storage.itemsLoading = false;
        }

        @Override
        public void onFinish(){
            Storage.itemsLoading = false;
            // FirebaseHandleUtil.deleteExpiredBids();
            if(itemsRecycler != null && searchItemAdapter != null){
                applyFilter(searchFilter);
                searchItemAdapter = new SearchItemAdapter(getActivity(), searchedItems);
                itemsRecycler.setAdapter(searchItemAdapter);
            }
        }
    }

    public void applyFilter(SearchFilter filter){
        searchedItems = new ArrayList<>();
        for (PostableItem postableItem : Storage.postableItems){
            if(postableItem.topBid >= 0 &&
                    postableItem.topBid <= filter.getPriceMax() &&
                    postableItem.name.toLowerCase().contains(filter.getTitle().toLowerCase()) &&
                    filter.getCategory().toLowerCase().contains(postableItem.category.toLowerCase())){
                searchedItems.add(postableItem);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.search_page_menu, menu);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(getActivity() != null) {
            getActivity().closeOptionsMenu();
        }
        hideKeyboard(getActivity(), mainSearchView);
    }

    @Override
    public void onResume(){
        super.onResume();
        hideKeyboard(getActivity(), mainSearchView);
        AppUtils.ToolbarChanger toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        if(toolbarChanger != null) {
            toolbarChanger.setTitleText(getString(R.string.search));
        }
        if(Storage.postableItems.isEmpty() && !Storage.itemsLoading) {
            PostableFetcher postableFetcher = new PostableFetcher();
            postableFetcher.getAll(Storage.postableItems);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        hideKeyboard(getActivity(), mainSearchView);
        final int itemId = item.getItemId();
        if(itemId == R.id.action_refresh){
            if(!Storage.itemsLoading) {
                PostableFetcher postableFetcher = new PostableFetcher();
                postableFetcher.getAll(Storage.postableItems);
            }

        }else if(itemId == R.id.action_filter){
            Intent i = SearchFiltersActivity.newIntent(getActivity(), searchFilter);
            startActivityForResult(i, FILTER_REQUEST_CODE);
        }else if(itemId == R.id.action_search_history){
            if(mainSearchView != null){
                mainSearchView.setQuery(null, false);
                // clear recycler
            }
            setHistoryVisibility();
        }else if(itemId == R.id.action_clear_history){
            for(SearchHistory history : searchHistories){
                SQLGateway.get(getActivity()).deleteSearchHistory(history);
            }
            String msg = "History Cleared!";
            if(onHistoryView){
                setHistoryVisibility();
            }
            if(getActivity() != null) {
                Snackbar.make(getActivity(), container, msg, BaseTransientBottomBar.LENGTH_SHORT)
                        .show();
            }
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        this.inflater = inflater;
        this.container = container;

        nothingHereView = v.findViewById(R.id.nothing_here_view);
        historyChipGroup = v.findViewById(R.id.postable_category_group);
        searchHistoryTextView = v.findViewById(R.id.search_history_text_view);
        mainSearchView = v.findViewById(R.id.main_search_view);
        itemsRecycler = v.findViewById(R.id.item_recycler);
        filterLabelLayout = v.findViewById(R.id.filter_label_layout);
        advancedClickableView = v.findViewById(R.id.advanced_clickable_view);
        filterDataLayout = v.findViewById(R.id.filter_data_layout);
        filterLabelImageView = v.findViewById(R.id.filter_state_arrow);
        nameRadioButton = v.findViewById(R.id.radio_name);
        endDateRadioButton = v.findViewById(R.id.radio_end_date);
        uploadDateRadioButton = v.findViewById(R.id.radio_upload_date);
        priceRadioButton = v.findViewById(R.id.radio_price);
        ascendingStateToggleView = v.findViewById(R.id.ascending_state_toggle_view);
        itemsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        closeFilterView();
        setupFilterHandles();

        advancedClickableView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = SearchFiltersActivity.newIntent(getActivity(), searchFilter);
                startActivityForResult(i, FILTER_REQUEST_CODE);
            }
        });

        ascendingStateToggleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ascending = ascendingStateToggleView.isChecked();
                Collections.reverse(searchedItems);
                searchItemAdapter = new SearchItemAdapter(getActivity(), searchedItems);
                itemsRecycler.setAdapter(searchItemAdapter);
                setupFilterHandles();
            }
        });

        filterLabelLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(filterLayoutOpen){
                    closeFilterView();
                }else{
                    openFilterView();
                }
            }
        });

        mainSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                nothingHereView.setVisibility(View.GONE);
                historyChipGroup.setVisibility(View.GONE);
                searchHistoryTextView.setVisibility(View.GONE);
                filterLabelLayout.setVisibility(View.VISIBLE);
                itemsRecycler.setVisibility(View.VISIBLE);
                itemsRecycler.scrollToPosition(0);
                onHistoryView = false;
                SQLGateway.get(getActivity()).addSearchHistory(new SearchHistory(query));
                //close keyboard
                hideKeyboard(getActivity(), mainSearchView);
                PreferenceUtils.setMainSearchValue(getActivity(), query);
                searchFilter.setTitle(query);
                applyFilter(searchFilter);
                searchItemAdapter = new SearchItemAdapter(getActivity(), searchedItems);
                itemsRecycler.setAdapter(searchItemAdapter);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                generateGenericFilter();
                if(newText.length() == 0){
                    PreferenceUtils.setMainSearchValue(getActivity(), null);
                    setHistoryVisibility();
                    return true;
                }
                return false;
            }
        });

        // Upon opening...
        // check if history
        String recentSearch = PreferenceUtils.getMainSearchValue(getActivity());
        if(recentSearch == null) {
            // fetch history
            setHistoryVisibility();
        }else{
            mainSearchView.setQuery(recentSearch, true);
            SearchItemAdapter searchItemAdapter = new SearchItemAdapter(getActivity(), searchedItems);
            itemsRecycler.setAdapter(searchItemAdapter);
        }
        return v;
    }

    private void setHistoryVisibility(){
        searchHistories = SQLGateway.get(getActivity()).getSearchHistory();
        if(inflater == null){
            return;
        }
        onHistoryView = true;
        itemsRecycler.setVisibility(View.GONE);
        searchHistoryTextView.setVisibility(View.VISIBLE);
        filterLabelLayout.setVisibility(View.GONE);
        if(searchHistories.size() == 0){
            nothingHereView.setVisibility(View.VISIBLE);
            closeFilterView();
            filterDataLayout.setVisibility(View.GONE);
            historyChipGroup.setVisibility(View.GONE);
        }else{
            historyChipGroup.removeAllViews();
            nothingHereView.setVisibility(View.GONE);
            closeFilterView();
            historyChipGroup.setVisibility(View.VISIBLE);
            for (SearchHistory s :searchHistories){
                Chip chip = (Chip) inflater.inflate(R.layout.search_history_chip, historyChipGroup, false);
                chip.setText(s.value);
                chip.setOnCloseIconClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SQLGateway.get(getActivity()).deleteSearchHistory(s);
                        historyChipGroup.removeView(chip);
                        // add deleteMessageToQueue
                        if(historyChipGroup.getChildCount() == 0){
                            nothingHereView.setVisibility(View.VISIBLE);
                            historyChipGroup.setVisibility(View.GONE);
                        }
                    }
                });
                chip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainSearchView.setQuery(s.value, true);
                    }
                });
                historyChipGroup.addView(chip, 0);
            }
        }
    }

    private void closeFilterView(){
        filterLabelImageView.setImageResource(R.drawable.ic_baseline_ios_arrow_down_24);
        filterDataLayout.setVisibility(View.GONE);
        filterLayoutOpen = false;
    }

    private void openFilterView(){
        filterLabelImageView.setImageResource(R.drawable.ic_baseline_ios_arrow_up_24);
        filterDataLayout.setVisibility(View.VISIBLE);
        filterLayoutOpen = true;
    }

    private void setupFilterHandles(){
        RadioButton[] radioButtons = {uploadDateRadioButton, endDateRadioButton, nameRadioButton, priceRadioButton};
        int[] namesAscending = {R.string.upload_date_ascending, R.string.end_date_ascending,
                R.string.name_ascending, R.string.price_ascending};
        int[] namesDescending = {R.string.upload_date_descending, R.string.end_date_descending,
                R.string.name_descending, R.string.price_descending};
        PostableItem.CRITERIA[] criteria = {PostableItem.CRITERIA.DATE_UPLOAD, PostableItem.CRITERIA.DATE_END,
                PostableItem.CRITERIA.NAME, PostableItem.CRITERIA.PRICE};
        boolean atLeastOneChecked = false;
        for(int i=0; i<radioButtons.length;i++){
            int finalI = i;
            radioButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Storage.ITEM_SORT_CRITERIA = criteria[finalI];
                    Collections.sort(searchedItems);
                    if(!ascending){
                        Collections.reverse(searchedItems);
                    }
                    searchItemAdapter = new SearchItemAdapter(getActivity(), searchedItems);
                    itemsRecycler.setAdapter(searchItemAdapter);
                }
            });
            if(radioButtons[i].isChecked()){
                atLeastOneChecked = true;
            }
            if(ascending){
                radioButtons[i].setText(namesAscending[i]);
            }else{
                radioButtons[i].setText(namesDescending[i]);
            }
        }
        if(!atLeastOneChecked){
            uploadDateRadioButton.setChecked(true);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(data == null){
            return;
        }
        if(requestCode == FILTER_REQUEST_CODE){
            Bundle bundle = data.getBundleExtra(SearchFiltersActivity.EXTRA_FILTER);
            if(bundle != null){
                int priceMin = bundle.getInt("0");
                int priceMax = bundle.getInt("1");
                String cat = bundle.getString("2");
                int checkedCode = bundle.getInt("3");
                searchFilter = new SearchFilter(null, cat, priceMin, priceMax);
                searchFilter.setCheckedCode((byte) checkedCode);
                mainSearchView.setQuery(mainSearchView.getQuery(), true);
            }
        }
    }

    private static class SearchItemAdapter extends AdapterUtil.PostableItemAdapter{
        SearchItemAdapter(Context context, List<PostableItem> items){
            super(context, items);
        }

        @Override
        public void onBindViewHolder(AdapterUtil.PostableItemHolder holder, int position){
            holder.bindPostable(mItems.get(position));
        }

        @Override
        public int getItemCount(){
            return mItems.size();
        }
    }

}
