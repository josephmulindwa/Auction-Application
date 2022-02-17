package com.scit.stauc;

import java.util.ArrayList;

import model.PostableItem;

public class NestedRecyclerItem {
    private final String category;
    public ArrayList<PostableItem> items;

    public NestedRecyclerItem(String category){
        this.category = category;
        items = new ArrayList<>();
    }

    public String getCategory(){
        return category;
    }
}
