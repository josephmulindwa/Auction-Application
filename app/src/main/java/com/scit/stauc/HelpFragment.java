package com.scit.stauc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import database.FSStoreFetcher;
import model.FAQ;
import util.AppUtils;

public class HelpFragment extends Fragment {
    private static final String TAG = "HelpFragment";
    private SearchView searchView;
    private RecyclerView helpRecyclerView;
    private ViewGroup frameContainer;
    private ProgressBar progressBar;
    private ArrayList<FAQ> faqs =  new ArrayList<>();
    private boolean loading = false;

    public static HelpFragment newInstance(){
        return new HelpFragment();
    }

    @Override
    public void onResume(){
        super.onResume();
        AppUtils.ToolbarChanger toolbarChanger = (AppUtils.ToolbarChanger) getActivity();
        if(toolbarChanger != null){
            toolbarChanger.setTitleText(getString(R.string.help));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_help, container, false);
        searchView = v.findViewById(R.id.help_search_view);
        helpRecyclerView = v.findViewById(R.id.help_recycler_view);
        frameContainer = v.findViewById(R.id.frame_container);
        progressBar = (ProgressBar) inflater.inflate(R.layout.progress_bar_layer, container, false);
        helpRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Type a question...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!loading) {
                    search(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText.length() == 0){
                    search(newText);
                    return true;
                }
                return false;
            }
        });

        FAQFetcher faqFetcher = new FAQFetcher();
        faqFetcher.getAll(faqs);

        return v;
    }

    private void search(String query){// custom search function
        query = query.replace(",", "").replace(".", "");
        ArrayList<FAQ> results = new ArrayList<>();

        if(query.length() == 0){
            FAQAdapter faqAdapter = new FAQAdapter(results);
            helpRecyclerView.setAdapter(faqAdapter);
        }

        String[] keys = query.split(" ");
        for(String key : keys){
            Log.i(TAG, "key = " + key);
        }

        ConcurrentHashMap<Integer, ArrayList<Integer>> hashMap = new ConcurrentHashMap<>();
        int maxMatchCount = 0;
        for(int i=0; i<faqs.size();i++){
            int matchcount = 0;
            for (String key : keys) {
                if (faqs.get(i).getQuestion().contains(key)) {
                    matchcount++;
                }
            }
            if(matchcount > maxMatchCount){
                maxMatchCount = matchcount;
            }
            if(hashMap.get(matchcount) == null){
                hashMap.put(matchcount, new ArrayList<>());
            }
            hashMap.get(matchcount).add(i);
        }
        while(maxMatchCount >= AppUtils.MIN_QUERY_MATCH_THRESHOLD){
            if(hashMap.get(maxMatchCount) != null) {
                for (int i : hashMap.get(maxMatchCount)) {
                    results.add(faqs.get(i));
                }
            }
            maxMatchCount--;
        }

        FAQAdapter faqAdapter = new FAQAdapter(results);
        helpRecyclerView.setAdapter(faqAdapter);
    }

    private class FAQHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView questionTextView;
        private final TextView answerTextView;
        private FAQ mFaq;

        public FAQHolder(View view){
            super(view);
            questionTextView = itemView.findViewById(R.id.query_text_view);
            answerTextView = itemView.findViewById(R.id.answer_text_view);
            itemView.setOnClickListener(this);
        }

        public void bindFAQ(FAQ faq){
            mFaq = faq;
            questionTextView.setText(faq.getQuestion());
            answerTextView.setText(faq.getAnswer());
        }

        @Override
        public void onClick(View view){
            View helpInfoDialogView = LayoutInflater.from(getActivity()).inflate(
                    R.layout.activity_help_view, frameContainer, false);
            TextView dialogQueryTextView = helpInfoDialogView.findViewById(R.id.query_text_view);
            TextView dialogAnswerTextView = helpInfoDialogView.findViewById(R.id.answer_text_view);
            dialogQueryTextView.setText(mFaq.getQuestion());
            dialogAnswerTextView.setText(mFaq.getAnswer());

            if(getActivity() != null) {
                AlertDialog helpInfoDialog = new AlertDialog.Builder(getActivity())
                        .setView(helpInfoDialogView)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                helpInfoDialog.show();
            }
        }
    }

    private class FAQAdapter extends RecyclerView.Adapter<FAQHolder>{
        private final ArrayList<FAQ> mItems;

        public FAQAdapter(ArrayList<FAQ> items){
            mItems = items;
        }

        @Override
        public FAQHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.faq_item_view,
                    frameContainer, false);
            return new FAQHolder(view);
        }

        @Override
        public void onBindViewHolder(FAQHolder holder, int position){
            holder.bindFAQ(mItems.get(position));
        }

        @Override
        public int getItemCount(){
            return mItems.size();
        }

    }

    private class FAQFetcher extends FSStoreFetcher<FAQ>{

        public FAQFetcher(){
            super(AppUtils.MODEL_FAQ, FAQ.class);
        }

        @Override
        public void onStartFetch(){
            if(progressBar != null && frameContainer != null){
                frameContainer.removeView(progressBar);
                frameContainer.addView(progressBar);
            }
            loading = true;
        }

        @Override
        public void onSucceed(){
            if(progressBar != null && frameContainer != null){
                frameContainer.removeView(progressBar);
            }
            loading = false;
        }

        @Override
        public void onFail(){
            if(progressBar != null && frameContainer != null){
                frameContainer.removeView(progressBar);
            }
            loading = false;
        }
    }

}
