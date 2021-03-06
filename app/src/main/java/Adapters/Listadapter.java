package Adapters;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;


import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import utils.GlobalClass;
import utils.Listdata;
import com.application.kurukshetrauniversitypapers.R;
import utils.Subjectcode;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import com.google.firebase.storage.StorageReference;

import java.util.List;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class Listadapter extends ArrayAdapter<Listdata> implements ActivityCompat.OnRequestPermissionsResultCallback {

    List<Listdata> subjectlist;
    private Activity context;
    int resource;
    StorageReference storageReference,myref;
    FirebaseStorage firebaseStorage;
    DatabaseReference rootref;
    FirebaseAuth mAuth;
    private final int STORAGE_PERMISSION_CODE = 1;
    public Listadapter(Activity context, int resource, List<Listdata> subjectlist)
    {
        super(context, resource, subjectlist);
        this.context = context;
        this.resource = resource;
        this.subjectlist = subjectlist;
    }
    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(resource, null, false);

        final TextView subjectname = view.findViewById(R.id.subjectname);
        final TextView papercount = view.findViewById(R.id.papercount);
        final Button downloadall = view.findViewById(R.id.download_btn);

        Listdata listdata = subjectlist.get(position);
        subjectname.setText(listdata.getSubjectname());
        papercount.setText(listdata.getPapercount());

        mAuth=FirebaseAuth.getInstance();
        downloadall.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                GlobalClass globalClass = new GlobalClass();
                Subjectcode subjectcode = new Subjectcode();
                subjectcode.setSubjectname(subjectname.getText().toString());
                Log.e(subjectname.getText().toString(), subjectname.getText().toString());
                Log.e("Board", globalClass.getBoard());
                Log.e("branch", globalClass.getBranch());
                Log.e("semester", globalClass.getSemester() + "");
                Log.e("code", subjectcode.getcode());
                Log.e("path", "IN/" + globalClass.getBoard() + "/" + globalClass.getBranch() + "/" + globalClass.getSemester() + "/" + subjectcode.getcode());

                if (papercount.getText().toString().substring(1, 2).equals("1")) {
                    Toast.makeText(context, papercount.getText().toString().substring(1, 2) + " file will be downloaded, see notification panel", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, papercount.getText().toString().substring(1, 2) + " files will be downloaded, see notification panel", Toast.LENGTH_LONG).show();
                }

                download("IN/" + globalClass.getBoard() + "/" + globalClass.getBranch() + "/" + globalClass.getSemester() + "/" + subjectcode.getcode());
            }
            else{
                requestStoragePermission();
            }
        });
        return view;
    }
    public void download(final String directory) {
        storageReference = firebaseStorage.getInstance().getReference(directory);
        rootref= FirebaseDatabase.getInstance().getReference(directory);
        rootref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(final DataSnapshot paper:dataSnapshot.getChildren()) {
                    Log.e("papername",paper.child("name").getValue().toString());
                    Log.e("dir",directory);
                    myref = storageReference.child(paper.child("name").getValue().toString()+".pdf");
                    myref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String url = uri.toString();
                        downloadfiles(getContext(), url);
                        Log.e("inside onSucces","");
                    }).addOnFailureListener(e -> {
                        Toast.makeText(context, "Unable to download now, try later", Toast.LENGTH_SHORT).show();
                        Log.e("inside onfailure", "");
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    public void toast(){Toast.makeText(context, "File will be downloaded, see notification panel", Toast.LENGTH_LONG).show();}
    public void downloadfiles(Context context, String url)
    {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String title = URLUtil.guessFileName(url,null,null);
        request.setTitle(title);
        String cookie = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie",cookie);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title);
        DownloadManager downloadManager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(context)
                    .setTitle("Permission needed")
                    .setMessage("Kindly click OK and then allow Qsol to access your storage, such that required files can be downloaded.")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions( context, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(context, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

