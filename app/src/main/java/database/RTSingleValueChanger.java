package database;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RTSingleValueChanger<T> {
    private Class<T> type;
    private final FirebaseDatabase database;
    private DatabaseReference dRef;
    private String category;
    private DataSnapshot snapshot;

    public RTSingleValueChanger(String category, Class<T> type){
        database = FirebaseDatabase.getInstance();
        this.type = type;
        this.category = category;
        dRef = database.getReference(category);
    }

    protected void onStartChange(){ }

    protected void onFinish(){ }

    protected void onChange(final DatabaseReference databaseReference){ } // what to do to delete or add

    protected void onFail(){ }

    public final DatabaseReference getRef(){
        return dRef;
    }

    public final void setDatabaseRef(DatabaseReference dRef){
        this.dRef = dRef;
    }

    public final DataSnapshot getCurrentSnapshot(){
        return snapshot;
    }

    public final void change(){
        onStartChange();
        dRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                RTSingleValueChanger.this.snapshot = snapshot;
                if (snapshot.exists()) {
                    onChange(dRef);
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
