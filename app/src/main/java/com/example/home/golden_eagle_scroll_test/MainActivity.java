package com.example.home.golden_eagle_scroll_test;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {


    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    TextView textView;
    private File[] fileList;
    private String[] filenameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.choose_file);
        textView = (TextView) findViewById(R.id.file_name);

        if (checkPermission()) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFileListDialog(Environment.getExternalStorageDirectory().toString());
                }
            });

        } else {
            requestPermission();
        }


    }


    private File[] loadFileList(String directory) {
        File path = new File(directory);

        Log.v("loadFileList", directory);
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    //add some filters here, for now return true to see all files
                    File file = new File(dir, filename);
                    return filename.contains(".tar.gz") || file.isDirectory();
                    //return true;
                }
            };

            //if null return an empty array instead
            File[] list = path.listFiles(filter);

            if (list != null) {
                Log.v("loadFileList", "list is not null");
                return list;
            } else {
                Log.v("loadFileList", "list is null");
                return new File[0];
            }

            //return list == null ? new File[0] : list;
        } else {
            return new File[0];
        }
    }


    public void showFileListDialog(final String directory) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        File[] tempFileList = loadFileList(directory);

        //if directory is root, no need to up one directory
        if (directory.equals("/")) {
            fileList = new File[tempFileList.length];
            filenameList = new String[tempFileList.length];

            //iterate over tempFileList
            for (int i = 0; i < tempFileList.length; i++) {
                fileList[i] = tempFileList[i];
                filenameList[i] = tempFileList[i].getName();
            }
        } else {
            fileList = new File[tempFileList.length + 1];
            filenameList = new String[tempFileList.length + 1];

            //add an "up" option as first item
            fileList[0] = new File(upOneDirectory(directory));
            filenameList[0] = "..";

            //iterate over tempFileList
            for (int i = 0; i < tempFileList.length; i++) {
                fileList[i + 1] = tempFileList[i];
                filenameList[i + 1] = tempFileList[i].getName();
            }
        }

        builder.setTitle("Choose your file: " + directory);

        builder.setItems(filenameList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                File chosenFile = fileList[which];
                textView.setText(chosenFile.toString());
                ExtractPackage(chosenFile.toString());
                if (chosenFile.isDirectory()) {
                    showFileListDialog(chosenFile.getAbsolutePath());
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    public String upOneDirectory(String directory) {
        String[] dirs = directory.split(File.separator);
        StringBuilder stringBuilder = new StringBuilder("");

        for (int i = 0; i < dirs.length - 1; i++) {
            stringBuilder.append(dirs[i]).append(File.separator);
        }

        return stringBuilder.toString();
    }

    protected boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Button button = (Button) findViewById(R.id.choose_file);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            showFileListDialog(Environment.getExternalStorageDirectory().toString());

                        }
                    });
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        openApplicationPermissions();
                    } else {
                        openApplicationPermissions();
                    }
                }
            }
        }
    }


    private void openApplicationPermissions() {
        final Intent intent_permissions = new Intent();
        intent_permissions.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent_permissions.addCategory(Intent.CATEGORY_DEFAULT);

        intent_permissions.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent_permissions.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent_permissions.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        MainActivity.this.startActivity(intent_permissions);
    }

    /**
     * Extracting the package from compresses tar.gz file
     *
     * @param basePackageName name of the tar file with extension
     */
    void ExtractPackage(String basePackageName) {
        String packageName = basePackageName;
        File baseLocal = Environment.getExternalStorageDirectory();
        File archive = new File(basePackageName);
        File destination = new File(Environment.getExternalStorageDirectory().toString());
        Log.v("downloading directory", Environment.getExternalStorageDirectory().toString());


        try {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
                    new GzipCompressorInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(archive))));

            TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry();

            while (entry != null) {

                if (entry.isDirectory()) {
                    entry = tarArchiveInputStream.getNextTarEntry();
                    // Log.i(LOGTAG, "Found directory " + entry.getName());
                    continue;
                }

                File currfile = new File(destination, entry.getName());
                File parent = currfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                OutputStream out = new FileOutputStream(currfile);
                IOUtils.copy(tarArchiveInputStream, out);
                out.close();
                //  Log.i(LOGTAG, entry.getName());
                entry = tarArchiveInputStream.getNextTarEntry();
            }
            tarArchiveInputStream.close();
        } catch (Exception e) {
            //  Log.i(LOGTAG, e.toString());
        }


    }


}
