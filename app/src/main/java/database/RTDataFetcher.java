package database;


import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class RTDataFetcher<T> {
    private final FirebaseDatabase database;
    private DatabaseReference dRef;
    private DataSnapshot snapshot;
    private ValueEventListener eventListener;
    private final String category;
    private final Class<T> type;

    public RTDataFetcher(String category, Class<T> type){
        this.category = category;
        database = FirebaseDatabase.getInstance();
        dRef = database.getReference(category);
        this.type = type;
    }

    public boolean validateCondition(T object){
        // can be used to filter results
        return true;
    }

    public void onFail() { }
    public void onFind(T object){ }
    public void onFinish(){ }
    public void onStartFetch(){ }
    public void stopListening(){
        if(eventListener != null) {
            dRef.removeEventListener(eventListener);
        }
    }
    public boolean endLoopCondition(){
        return false;
    }

    public final DatabaseReference getRef(){
        return dRef;
    }

    public final void setDatabaseRef(DatabaseReference dRef){
        this.dRef = dRef;
    }

    public final DataSnapshot getCurrentSnapshot(){
        return snapshot;
    }

    public void fetch(List<T> items) {
        onStartFetch();
        dRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventListener = this;
                if(snapshot.exists()){
                    for(DataSnapshot ds : snapshot.getChildren()){
                        RTDataFetcher.this.snapshot = ds;
                        if(endLoopCondition()){
                            onFinish();
                            return;
                        }
                        T inItem = ds.getValue(type);
                        if(inItem == null){ continue; }
                        if(validateCondition(inItem)) {
                            onFind(inItem);
                            if(items == null){ continue; }
                            items.add(inItem);
                        }
                    }
                }
                onFinish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onFail();
            }
        });
    }

    public void listen(List<T> items) {
        onStartFetch();
        dRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                onStartFetch();
                eventListener = this;
                if(snapshot.exists()){
                    for(DataSnapshot ds : snapshot.getChildren()){
                        RTDataFetcher.this.snapshot = ds;
                        if(endLoopCondition()){
                            onFinish();
                            return;
                        }
                        T inItem = ds.getValue(type);
                        if(inItem == null){
                            continue;
                        }
                        if(validateCondition(inItem)) {
                            if(items != null) {
                                items.add(inItem);
                            }
                            onFind(inItem);
                        }
                    }
                }
                onFinish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onFail();
            }
        });
    }
}

