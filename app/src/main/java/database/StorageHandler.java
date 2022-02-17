package database;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class StorageHandler {
    private static final String TAG = "StorageHandler";
    public static final String PROFILEPATH = "profileImages";
    public static final String ITEMPATH = "itemImages";
    private final FirebaseStorage storage;
    private UploadTask uploadTask = null;

    public StorageHandler(){
        storage = FirebaseStorage.getInstance();
    }

    public void uploadBytes(byte[] bytes, String id, String filename, String refPath){
        if(bytes == null){
            onFinish();
            onGetDownloadUri(null);
            return;
        }
        onStart();
        StorageReference storageReference = storage.getReference(refPath);
        StorageReference subfolderReference = storageReference.child(id);
        StorageReference imageRef = subfolderReference.child(filename);

        uploadTask = imageRef.putBytes(bytes);
        Log.i("RequestFormActivity", "uploading...");
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    Log.i("RequestFormActivity", "success upload");
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Log.i("RequestFormActivity", "@uri sucess:"+uri.toString());
                            onGetDownloadUri(uri);
                            StorageHandler.this.onSuccess();
                            onFinish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("RequestFormActivity", "@uri failed :"+e.getMessage());
                            onFail();
                            onFinish();
                        }
                    });
                }else if(task.isCanceled()){
                    onCancel();
                }
                if(task.isComplete() && !task.isSuccessful()){
                    uploadTask = null;
                    onFinish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("RequestFormActivity", "failed :"+e.getMessage());
                onFail();
                onFinish();
            }
        });
    }

    public void onStart(){ }
    public void onFail(){ }
    public void onSuccess(){ }
    public void onCancel(){ }
    public void onFinish(){ }
    public void onGetDownloadUri(Uri downloadUri){ }

    public void cancel(){
        if (uploadTask != null){
            uploadTask.cancel();
        }
    }

    public void deleteBytes(String url){
        onStart();
        StorageReference ref = storage.getReferenceFromUrl(url);
        ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                StorageHandler.this.onSuccess();
                onFinish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                onFail();
                onFinish();
            }
        }).addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                onCancel();
                onFinish();
            }
        });
    }

}
