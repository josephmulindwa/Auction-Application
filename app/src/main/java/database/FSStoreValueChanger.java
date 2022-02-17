package database;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class FSStoreValueChanger<T> {
    private final Class<T> type;
    private final String collectionName;
    private final FirebaseFirestore db;
    private final CollectionReference ref;

    public FSStoreValueChanger(String collectionName, Class<T> type){
        db = FirebaseFirestore.getInstance();
        this.type = type;
        this.collectionName = collectionName;
        ref = db.collection(collectionName);
    }

    protected void onStartChange(){}
    protected void onSucceed(){}
    protected void onFail(){}
    protected void onFinish(){}

    public void set(String id, T object){
        onStartChange();
        ref.document(id).set(object)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

    public  void setMerge(String id, T object){
        onStartChange();
        ref.document(id).set(object, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

    public void update(String id, String key, String value){
        onStartChange();
        ref.document(id).update(key, value)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

    public void update(String id, String key, Object value){
        onStartChange();
        ref.document(id).update(key, value)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

    public void addToList(String id, String listname, String value){
        onStartChange();
        ref.document(id).update(listname, FieldValue.arrayUnion(value))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

    public void removeFromList(String id, String listname, String value){
        onStartChange();
        ref.document(id).update(listname, FieldValue.arrayRemove(value))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

    public void delete(String id){
        onStartChange();
        ref.document(id).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            onSucceed();
                        }else {
                            onFail();
                        }
                        onFinish();
                    }
                });
    }

}
