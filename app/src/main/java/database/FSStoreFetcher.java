package database;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import util.AppUtils;

public class FSStoreFetcher<T> {
    private static final String TAG = "StoreFetcher";
    private final Class<T> type;
    private final FirebaseFirestore db;
    private final CollectionReference ref;

    public FSStoreFetcher(String collectionName, Class<T> type){
        db = FirebaseFirestore.getInstance();
        this.type = type;
        ref = db.collection(collectionName);
    }

    protected void onStartFetch(){}
    protected void onSucceed(){}
    protected void onFail(){}
    protected void onFinish(){}
    protected void onFind(T object){}
    protected boolean validateCondition(T object){ return true; }
    protected boolean endFetchCondition(){ return false; }

    public void getAll(List<T> items){
        onStartFetch();
        ref.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for (QueryDocumentSnapshot doc : task.getResult()){
                        if(endFetchCondition()){ break; }
                        T temp = doc.toObject(type);
                        if(validateCondition(temp)) {
                            items.add(temp);
                        }
                        if(AppUtils.MAX_FETCHABLE_ITEMS != -1){
                            if(items.size() >= AppUtils.MAX_FETCHABLE_ITEMS){
                                break;
                            }
                        }
                    }
                    onSucceed();
                }else{
                    onFail();
                }
                onFinish();
            }
        });
    }

    public void query(List<T> items){
        // add only those that meet a custom condition : see where()
        onStartFetch();
        ref.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for (QueryDocumentSnapshot doc : task.getResult()){
                        if(endFetchCondition()){ break; }
                        T temp = doc.toObject(type);
                        if(validateCondition(temp)) {
                            onFind(temp);
                            if(items != null) {
                                items.add(temp);
                            }
                        }
                    }
                    onSucceed();
                }else{
                    onFail();
                }
                onFinish();
            }
        });
    }

    public void where(List<T> items, String key, String value){
        // add those where key == value
        onStartFetch();
        ref.whereEqualTo(key, value)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(DocumentSnapshot doc : task.getResult()){
                        if(endFetchCondition()){ break; }
                        T temp = doc.toObject(type);
                        onFind(temp);
                        if(items != null) {
                            items.add(temp);
                        }
                    }
                    onSucceed();
                }else{
                    onFail();
                }
                onFinish();
            }
        });
        //Source surce = Source.CACHE
        //.get(source)
    }

}
