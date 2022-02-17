package com.scit.stauc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

public class CategoryViewActivity extends SingleFragmentActivity{
    private static final String CATEGORY_EXRA = "category.extra";
    private static final String CATEGORY_INDEX = "category.index";

    public static Intent newIntent(Context context, String category, int categoryIndex){
        Intent intent = new Intent(context, CategoryViewActivity.class);
        intent.putExtra(CATEGORY_EXRA, category);
        intent.putExtra(CATEGORY_INDEX, categoryIndex);
        return intent;
    }

    @Override
    public Fragment createFragment(){
        String category = getIntent().getStringExtra(CATEGORY_EXRA);
        int categoryIndex = getIntent().getIntExtra(CATEGORY_INDEX, 0);
        return CategoryViewFragment.newInstance(category, categoryIndex);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        String category = getIntent().getStringExtra(CATEGORY_EXRA);
        if(actionBar != null) {
            actionBar.setTitle(category);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.search_refresh_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        final int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
