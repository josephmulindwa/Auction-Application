package com.scit.stauc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import database.SQLGateway;
import model.PostableItem;
import model.Profile;
import model.SearchFilter;
import util.AppUtils;
import util.PreferenceUtils;

public class SearchFiltersEditFragment extends Fragment {
    private static final String TAG = "SearchFilters";
    private String filterCategoryCode;
    private int filterPriceMin, filterPriceMax;
    private String filterName;
    private final int PRECISION = 1;
    private boolean loaded;

    private SearchFilter mSearchFilter;
    private EditText editAmountFrom;
    private EditText editAmountTo;
    private Button saveFilterButton;
    private TextView filterNameView;
    private final PostableItem.CATEGORY[] categories = PostableItem.CATEGORY.values();
    private final CheckBox[] categoryCheckBoxes = new CheckBox[categories.length + 1];

    interface ActivityHelper{
        void setBadge(int tabIndex);
        void clearBadge(int tabIndex);
        void scrollToTab(int tabIndex);
        void scrollToTab(int tabIndex, Object object);
        void returnResult(Object object);
        SearchFilter getSearchFilter();
        void setObject(Object object);
        void clearFilters();
    }

    public SearchFiltersEditFragment(SearchFilter filter){
        mSearchFilter = filter;
    }

    @Override
    public void onResume(){
        super.onResume();
        loadFromFilter();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(getActivity() != null){
            mSearchFilter.setPriceMin(filterPriceMin); // last our improv
            mSearchFilter.setPriceMax(filterPriceMax);
            ((ActivityHelper) getActivity()).setObject(mSearchFilter);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        loaded = false;
        filterName = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_search_filters_edit, container, false);

        filterNameView = v.findViewById(R.id.filter_name_view);
        editAmountFrom = v.findViewById(R.id.edit_currency_from);
        editAmountTo = v.findViewById(R.id.edit_currency_to);

        LinearLayout checkBoxLayout = v.findViewById(R.id.check_box_layout);
        CheckBox allCheckbox = new CheckBox(getActivity());
        allCheckbox.setText(R.string.category_all);
        allCheckbox.setChecked(true);
        categoryCheckBoxes[0] = allCheckbox;
        checkBoxLayout.addView(allCheckbox);
        for (int i=0; i < categories.length; i++){
            CheckBox newCheckBox = new CheckBox(getActivity());
            newCheckBox.setText(AppUtils.capitalize(categories[i].toString()));
            categoryCheckBoxes[i+1] = newCheckBox;
            checkBoxLayout.addView(newCheckBox);
        }

        saveFilterButton = v.findViewById(R.id.button_save);
        Button applyFilterButton = v.findViewById(R.id.button_apply);

        filterNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0){
                    filterNameView.setVisibility(View.VISIBLE);
                }else{
                    filterNameView.setVisibility(View.GONE);
                }
            }
        });

        TextView resetView = v.findViewById(R.id.text_reset);
        resetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editAmountFrom.setText(R.string.number_min_price);
                editAmountTo.setText(R.string.number_max_price);
                filterPriceMin = (int)AppUtils.getDoubleFromFormatted(editAmountFrom.getText().toString());
                filterPriceMax = (int)AppUtils.getDoubleFromFormatted(editAmountTo.getText().toString());
                Log.i(TAG, "PriceMin:"+filterPriceMin + ", " + filterPriceMax);
                resetView.setVisibility(View.GONE);
            }
        });

        int errorColor = R.color.red_400;
        editAmountFrom.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(editAmountFrom.getText().length() > 0) {
                    if (!hasFocus) {
                        editAmountFrom.setText(AppUtils.getFormattedNumber(filterPriceMin, PRECISION));
                    }else{
                        String editString = Integer.toString(filterPriceMin);
                        editAmountFrom.setText(editString);
                    }
                }
            }
        });
        editAmountFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(loaded) {
                    resetView.setVisibility(View.VISIBLE);
                    enableSaveButton();
                }
                if (s.length() <= 0){
                    editAmountFrom.setError(getString(R.string.value_is_required));
                    filterPriceMin = 0;
                    return;
                }
                try {
                    filterPriceMin = Integer.parseInt(s.toString());
                }catch (NumberFormatException ignored){ }

                if(!AppUtils.isValidAmount(filterPriceMin)){
                    editAmountFrom.setTextColor(getResources().getColor(errorColor));
                    editAmountFrom.setError("Enter valid amount!");
                    return;
                }else{
                    editAmountFrom.setTextColor(getResources().getColor(R.color.black));
                    editAmountFrom.setError(null);
                }

                if(filterPriceMin > filterPriceMax){
                    editAmountFrom.setTextColor(getResources().getColor(errorColor));
                    editAmountFrom.setError(getString(R.string.from_price_warning));
                }else{
                    editAmountFrom.setTextColor(getResources().getColor(R.color.black));
                    editAmountFrom.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        editAmountTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(editAmountTo.getText().length() > 0) {
                    if (!hasFocus) {
                        editAmountTo.setText(AppUtils.getFormattedNumber(filterPriceMax, PRECISION));
                    } else {
                        String editString = Integer.toString(filterPriceMax);
                        editAmountTo.setText(editString);
                    }
                }
            }
        });
        editAmountTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(loaded) {
                    resetView.setVisibility(View.VISIBLE);
                    enableSaveButton();
                }
                if (s.length() <= 0){
                    editAmountTo.setError(getString(R.string.value_is_required));
                    filterPriceMax = 0;
                    return;
                }
                try {
                    filterPriceMax = Integer.parseInt(s.toString());
                }catch (NumberFormatException ignored){ }

                if(!AppUtils.isValidAmount(filterPriceMax)){
                    editAmountTo.setTextColor(getResources().getColor(errorColor));
                    editAmountTo.setError("Enter valid amount!");
                    return;
                }else{
                    editAmountTo.setTextColor(getResources().getColor(R.color.black));
                    editAmountTo.setError(null);
                }

                if(filterPriceMax < filterPriceMin){
                    editAmountTo.setTextColor(getResources().getColor(errorColor));
                    editAmountTo.setError(getString(R.string.to_price_warning));
                }else{
                    editAmountTo.setTextColor(getResources().getColor(R.color.black));
                    editAmountTo.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        applyFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editAmountFrom.getError() != null || editAmountTo.getError() != null){
                    return;
                }
                if(getActivity() != null) {
                    collectForFilter();
                    ((ActivityHelper) getActivity()).returnResult(mSearchFilter);
                }
            }
        });

        saveFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editAmountFrom.getError() != null || editAmountTo.getError() != null){
                    return;
                }
                if(collectForFilter()){
                    if(filterName == null) { // default save
                        View dView = inflater.inflate(R.layout.dialog_save_filter, container, false);
                        Button cancelButton = dView.findViewById(R.id.cancel_button);
                        Button okButton = dView.findViewById(R.id.ok_button);
                        EditText editFilterNameView = dView.findViewById(R.id.edit_name_view);
                        editFilterNameView.setText(mSearchFilter.getName());

                        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                .setView(dView)
                                .create();
                        alertDialog.show();

                        okButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String name = editFilterNameView.getText().toString();
                                if (name.isEmpty()) {
                                    editFilterNameView.setError(getString(R.string.empty_field));
                                    return;
                                } else if (!AppUtils.isAlphanumeric(name, true)) {
                                    editFilterNameView.setError(getString(R.string.non_alphanumeric));
                                    return;
                                }
                                filterName = mSearchFilter.getName();
                                filterNameView.setText(filterName);

                                handleSave(v, getString(R.string.saved), mSearchFilter);
                                alertDialog.dismiss();
                            }
                        });

                        cancelButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                alertDialog.dismiss();
                            }
                        });
                    }else{
                        handleSave(v, getString(R.string.saved), mSearchFilter);
                    }
                }
            }
        });

        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        return v;
    }

    private void handleSave(View snackRef, String snackText, SearchFilter searchFilter){
        SQLGateway.get(getActivity()).updateSearchFilter(searchFilter);
        if (getActivity() != null) {
            Snackbar.make(getActivity(), snackRef,
                    snackText,
                    Snackbar.LENGTH_SHORT).show();
        }
        disableSaveButton();
        ((ActivityHelper) getActivity()).setBadge(1);
    }

    private void disableSaveButton(){
        if(!saveFilterButton.isEnabled()){ return; }
        saveFilterButton.setBackgroundColor(
                getResources().getColor(R.color.gray_battleship)
        );
        saveFilterButton.setClickable(false);
        saveFilterButton.setEnabled(false);
    }

    private void enableSaveButton(){
        if(saveFilterButton.isEnabled()){ return; }
        saveFilterButton.setBackgroundColor(
                getResources().getColor(R.color.color_primary)
        );
        saveFilterButton.setClickable(true);
        saveFilterButton.setEnabled(true);
    }

    private String getFilterName(){
        // generate a name from current state : default filter_0_999M_0
        StringBuilder stringBuilder = new StringBuilder();
        int cCode = mSearchFilter == null ? 0 : mSearchFilter.getCheckedCode();
        stringBuilder.append("filter_")
                .append(AppUtils.getFormattedNumber(filterPriceMin, PRECISION)).append("_")
                .append(AppUtils.getFormattedNumber(filterPriceMax, PRECISION)).append("_")
                .append(cCode);
        return stringBuilder.toString();
    }

    private void loadFromFilter(){
        // mSearch != null;
        if(getActivity() != null) {
            mSearchFilter = ((ActivityHelper) getActivity()).getSearchFilter();
        }

        if(mSearchFilter == null) {
            StringBuilder stringBuilder = new StringBuilder();
            for (PostableItem.CATEGORY category : PostableItem.CATEGORY.values()) {
                stringBuilder.append(category.toString()).append("_");
            }
            filterCategoryCode = stringBuilder.toString();
            String defaultPriceMin = Integer.toString(AppUtils.MIN_ACCEPTED_CURRENCY_VALUE);
            String defaultPriceMax = Integer.toString(AppUtils.MAX_ACCEPTED_CURRENCY_VALUE);
            filterPriceMin = (int) AppUtils.getDoubleFromFormatted(defaultPriceMin);
            filterPriceMax = (int) AppUtils.getDoubleFromFormatted(defaultPriceMax);
            filterName = null;
            mSearchFilter = new SearchFilter(
                    null, filterCategoryCode, filterPriceMin, filterPriceMax);
            mSearchFilter.setIndexChecked(0, true);
        }else if(mSearchFilter.getName() != null) {
            filterName = mSearchFilter.getName();
            // or handle with save
            disableSaveButton();
        }
        filterNameView.setText(filterName);

        // LOAD CATEGORY CHECKBOXES
        for(int i=0; i < categoryCheckBoxes.length; i++) {
            categoryCheckBoxes[i].setChecked(mSearchFilter.getIndexClicked(i));
            int finalI = i;
            categoryCheckBoxes[i].setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleCheckBoxClicks(categoryCheckBoxes[finalI]);
                        }
                    }
            );
        }

        // LOAD PRICE DATA
        filterPriceMin = mSearchFilter.getPriceMin();
        filterPriceMax = mSearchFilter.getPriceMax();
        editAmountFrom.setText(AppUtils.getFormattedNumber(filterPriceMin, PRECISION));
        editAmountTo.setText(AppUtils.getFormattedNumber(filterPriceMax, PRECISION));

        loaded = true;
    }

    private boolean collectForFilter(){
        if(editAmountFrom.getError() != null){
            editAmountFrom.requestFocus();
            return false;
        } else if(editAmountTo.getError() != null){
            editAmountTo.requestFocus();
            return false;
        }
        if(filterName == null){ // generate name; non provided
            mSearchFilter.setName(getFilterName());
        }else{
            mSearchFilter.setName(filterName); // not necessary
        }
        if(filterCategoryCode == null){
            StringBuilder stringBuilder = new StringBuilder();
            for (PostableItem.CATEGORY category : PostableItem.CATEGORY.values()) {
                stringBuilder.append(category.toString()).append("_");
            }
            filterCategoryCode = stringBuilder.toString();
        }
        mSearchFilter.setCategory(filterCategoryCode);
        mSearchFilter.setPriceMin(filterPriceMin);
        mSearchFilter.setPriceMax(filterPriceMax);
        return true;
    }

    private void handleCheckBoxClicks(CheckBox clickedCheckBox){
        // allow multi to be selected except for "all"
        byte checkedSum = 0;
        if(loaded){
            enableSaveButton();
        }
        for (CheckBox checkBox : categoryCheckBoxes){
            if(checkBox.isChecked()){ checkedSum++; }
        }
        if((clickedCheckBox == categoryCheckBoxes[0]) || (checkedSum >= categoryCheckBoxes.length-1)){
            for(CheckBox checkBox : categoryCheckBoxes){
                checkBox.setChecked(checkBox == categoryCheckBoxes[0]);
            }
            StringBuilder stringBuilder = new StringBuilder();
            for(PostableItem.CATEGORY category : categories){
                stringBuilder.append(category.toString()).append("_");
            }
            filterCategoryCode = stringBuilder.toString();
        }else {
            categoryCheckBoxes[0].setChecked(false);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < categoryCheckBoxes.length; i++) {
                if (i == 0) { continue; }
                if(categoryCheckBoxes[i].isChecked()){
                    stringBuilder.append(categories[i-1].toString()).append("_");
                }
            }
            filterCategoryCode = stringBuilder.toString();
        }
        for(int i=0; i<categoryCheckBoxes.length; i++){
            mSearchFilter.setIndexChecked(i, categoryCheckBoxes[i].isChecked());
        }
    }

}
