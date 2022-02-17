package com.scit.stauc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import database.SQLGateway;
import model.SearchFilter;
import util.AppUtils;

public class SearchFiltersActivity extends AppCompatActivity implements SearchFiltersEditFragment.ActivityHelper {
    public static final String EXTRA_FILTER = "extra_search_filter";
    private static final String EXTRA_FILTER_PRICEMIN = "extra_filter_pricemin";
    private static final String EXTRA_FILTER_PRICEMAX = "extra_filter_pricemax";
    private static final String EXTRA_FILTER_CATEGORY = "extra_filter_category";
    private static final String EXTRA_FILTER_CODE = "extra_filter_code";
    private ViewPager viewPager;
    private TabLayout mTabLayout;
    private Object fragmentObject = null; // save this on rotate
    private SearchFilter searchFilter = null;

    public static Intent newIntent(Context context, SearchFilter searchFilter){
        Intent i = new Intent(context, SearchFiltersActivity.class);
        if(searchFilter != null) {
            i.putExtra(EXTRA_FILTER_CATEGORY, searchFilter.getCategory());
            i.putExtra(EXTRA_FILTER_PRICEMIN, searchFilter.getPriceMin());
            i.putExtra(EXTRA_FILTER_PRICEMAX, searchFilter.getPriceMax());
            i.putExtra(EXTRA_FILTER_CODE, searchFilter.getCheckedCode());
        }
        return i;
    }

    @Override
    public SearchFilter getSearchFilter(){
        return (SearchFilter) fragmentObject;
    }

    @Override
    public void setBadge(int tabIndex){
        if(mTabLayout != null){
            Objects.requireNonNull(mTabLayout.getTabAt(tabIndex)).getOrCreateBadge().clearNumber();
            Objects.requireNonNull(mTabLayout.getTabAt(tabIndex)).getOrCreateBadge()
                    .setBackgroundColor(
                    getResources().getColor(R.color.dodger_blue)
            );
        }
    }

    public void clearBadge(int tabIndex){
        if(mTabLayout != null){
            BadgeDrawable badgeDrawable = Objects.requireNonNull(mTabLayout.getTabAt(tabIndex))
                    .getOrCreateBadge();
            badgeDrawable.setVisible(false);
        }
    }

    public void scrollToTab(int tabIndex){
        if(viewPager != null && mTabLayout != null){
            viewPager.setCurrentItem(tabIndex);
        }
    }

    public void scrollToTab(int tabIndex, Object object){
        if(viewPager != null && mTabLayout != null){
            fragmentObject = object;
            viewPager.setCurrentItem(tabIndex);
        }
    }

    public void setObject(Object object){
        fragmentObject = (SearchFilter) object;
    }

    public void clearFilters(){
        fragmentObject = null;
        ArrayList<SearchFilter> filters = SQLGateway.get(this).getSearchFilters();
        for (SearchFilter filter : filters){
            SQLGateway.get(this).deleteSearchFilter(filter);
        }
        viewPager.setAdapter(new FiltersPagerAdapter(getSupportFragmentManager()));
        Snackbar.make(this, viewPager, getString(R.string.filters_cleared),
                Snackbar.LENGTH_SHORT).show();
    }

    public void returnResult(Object object){
        searchFilter = (SearchFilter) object;
        Intent outIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putInt("0", searchFilter.getPriceMin());
        bundle.putInt("1", searchFilter.getPriceMax());
        bundle.putString("2", searchFilter.getCategory());
        bundle.putInt("3", searchFilter.getCheckedCode());
        outIntent.putExtra(EXTRA_FILTER, bundle);
        setResult(Activity.RESULT_OK, outIntent);
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);
        Toolbar toolbar = findViewById(R.id.m_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
            actionBar.setTitle(R.string.filters);
        }

        searchFilter = new SearchFilter(null,
                getIntent().getStringExtra(EXTRA_FILTER_CATEGORY),
                getIntent().getIntExtra(EXTRA_FILTER_PRICEMIN, AppUtils.MIN_ACCEPTED_CURRENCY_VALUE),
                getIntent().getIntExtra(EXTRA_FILTER_PRICEMAX, AppUtils.MAX_ACCEPTED_CURRENCY_VALUE)
        );
        searchFilter.setCheckedCode((byte) getIntent().getIntExtra(EXTRA_FILTER_CODE, 1));
        setObject(searchFilter);

        mTabLayout = findViewById(R.id.m_tablayout);

        viewPager = findViewById(R.id.fragment_viewpager);
        viewPager.setAdapter(new FiltersPagerAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                if(position == 1){ clearBadge(position); }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.clear_filters_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        final int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            finish();
        }else if(itemId == R.id.action_clear_filters){
            clearFilters();
        }
        return true;
    }

    private class FiltersPagerAdapter extends FragmentStatePagerAdapter{

        public FiltersPagerAdapter(FragmentManager fm){
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount(){ return 2; }

        @Override
        public Fragment getItem(int position){
            switch (position){
                case 0:
                    return new SearchFiltersEditFragment(searchFilter);
                case 1:
                    return SearchFiltersSavedFragment.newInstance();
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position){
            switch (position){
                case 0:
                    return getString(R.string.edit);
                case 1:
                    return getString(R.string.saved);
            }
            return null;
        }

    }

}
