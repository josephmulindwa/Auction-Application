package com.scit.stauc;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class SingleFragmentActivity extends AppCompatActivity{

    public boolean withToolbar(){
        return false;
    }

    @LayoutRes
    public int getLayoutResId(){
        return R.layout.activity_fragment;
    }

    public Fragment createFragment(){
        return new Fragment();
    }
    public boolean hasToolbar(){ return true; }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        Toolbar toolbar = findViewById(R.id.m_toolbar);
        if(hasToolbar()){
            setSupportActionBar(toolbar);
        }else{
            toolbar.setVisibility(View.GONE);
        }
        Fragment fragment = createFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}
