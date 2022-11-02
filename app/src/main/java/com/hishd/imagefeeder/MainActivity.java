package com.hishd.imagefeeder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView imgPreviewImage;
    private EditText txtImageId;
    private Button btnOpen;
    private Button btnSave;
    private Button btnRetrieve;
    private TextView lblData;
    private ActivityResultLauncher<Intent> fetchImageFromGalleryLauncher;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupResources();
        setListeners();
    }

    private void setupResources() {
        imgPreviewImage = findViewById(R.id.imgPreviewImage);
        txtImageId = findViewById(R.id.txtImageId);
        btnSave = findViewById(R.id.btnSave);
        btnRetrieve = findViewById(R.id.btnRetrieve);
        btnOpen = findViewById(R.id.btnOpen);
        lblData = findViewById(R.id.lblData);

        //The onActivityResult is Deprecated, instead using the ActivityResultLauncher API
        fetchImageFromGalleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        if (result.getData() != null) {
                            Uri selectedImageUri = result.getData().getData();
                            bitmap = BitmapFactory.decodeStream(getBaseContext().getContentResolver().openInputStream(selectedImageUri));
                            imgPreviewImage.setImageBitmap(bitmap);
                        }
                    } catch (Exception exception) {
                        Log.e("TAG", "Exception : " + exception.getLocalizedMessage());
                    }
                }
            }
        });
    }

    private void setListeners() {
        btnOpen.setOnClickListener(view -> {
            checkPermissionAndOpenGallery();
        });
        btnSave.setOnClickListener(view -> {
            if (txtImageId.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a valid image id", Toast.LENGTH_SHORT).show();
                return;
            }
            if (bitmap == null) {
                Toast.makeText(MainActivity.this, "Please select a bitmap image", Toast.LENGTH_SHORT).show();
                return;
            }
            storeBitmapImage(txtImageId.getText().toString(), bitmap);
        });
        btnRetrieve.setOnClickListener(view -> {
            if (txtImageId.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a valid image id", Toast.LENGTH_SHORT).show();
                return;
            }
            retrieveImageAndShow(txtImageId.getText().toString());
        });
    }

    private void checkPermissionAndOpenGallery() {
        //If the Platform is >= Q only checks the READ_EXTERNAL_STORAGE permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGalley();
            } else {
                Log.e("Permissions", "Requesting Missing Permissions for android >= 10");
                allowPermissionForStorage();
            }
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGalley();
        } else {
            Log.e("Permissions", "Requesting Missing Permissions");
            allowPermissionForStorage();
        }
    }

    /**
     * Request storage permissions
     * for android Q and above only the READ_EXTERNAL_STORAGE is required because of limited access for other File Uri's
     */
    private void allowPermissionForStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]
                    {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                    }, 2);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                    {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                    }, 2);
        }
    }

    /**
     * Opens the Gallery intent to select the image
     */
    private void openGalley() {
        try {
            Log.e("Gallery", "Opening Gallery....!");
            Intent intent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            } else {
                intent = new Intent();
            }
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            fetchImageFromGalleryLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will convert the Bitmap into a file and save it on the app specific storage
     *
     * @param imageId the image name to be saved
     * @param bitmap  the generated Bitmap
     */
    private void storeBitmapImage(String imageId, Bitmap bitmap) {
        try {
            //Create a empty file on the app specific directory
            //Existing files will be overwritten
            File dest = new File(getFilesDir(), imageId);
            try {
                FileOutputStream out = new FileOutputStream(dest);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Image not saved", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * This method will read the file stored on the app specific directory
     *
     * @param imageName saved image file id
     */
    private void retrieveImageAndShow(String imageName) {
        //Create a empty file instance using the provided imageName
        File file = new File(getFilesDir(), imageName);
        //Check if created file exists
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            if (bitmap == null) {
                Toast.makeText(this, "Could not create bitmap", Toast.LENGTH_SHORT).show();
                return;
            }
            imgPreviewImage.setImageBitmap(bitmap);
        }
    }
}