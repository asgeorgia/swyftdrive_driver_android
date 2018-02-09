package com.swyftpartner.driver.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.swyftpartner.driver.R;
import com.swyftpartner.driver.httpRequester.AsyncTaskCompleteListener;
import com.swyftpartner.driver.httpRequester.MultiPartRequester;
import com.swyftpartner.driver.httpRequester.VollyRequester;
import com.swyftpartner.driver.model.Uploads;
import com.swyftpartner.driver.utils.AndyUtils;
import com.swyftpartner.driver.utils.Commonutils;
import com.swyftpartner.driver.utils.Const;
import com.swyftpartner.driver.utils.PreferenceHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import id.zelory.compressor.Compressor;

/**
 * Created by user on 1/26/2017.
 */

public class UploadAdapter extends RecyclerView.Adapter<UploadAdapter.typesViewHolder> implements AsyncTaskCompleteListener {

    private Activity mContext;
    Context context;
    private List<Uploads> itemsuploadList;
    private File cameraFile;
    private String filepath = "";
    private Uri uri = null;
    private String document_id = "";


    public UploadAdapter(Activity context, List<Uploads> itemsuploadList) {
        mContext = context;

        this.itemsuploadList = itemsuploadList;

    }

    @Override
    public UploadAdapter.typesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.upload_item, null);
        context = parent.getContext();
        UploadAdapter.typesViewHolder holder = new UploadAdapter.typesViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final UploadAdapter.typesViewHolder holder, int position) {
        final Uploads upload_itme = itemsuploadList.get(position);

        if (upload_itme != null) {

            holder.tv_upload_name.setText(upload_itme.getUpload_name());

            //Glide.with().load(upload_itme.getUpload_img()).into(holder.iv_upload);
            switch (position) {
                case 0:
                    Glide.with(mContext).load(upload_itme.getUpload_img())
                            .error(R.drawable.driving_lisence)
                            .fitCenter().into(holder.iv_upload);
                    break;
                case 1:
                    Glide.with(mContext).load(upload_itme.getUpload_img())
                            .error(R.drawable.lisence_plate)
                            .fitCenter().into(holder.iv_upload);
                    break;
                case 2:
                    Glide.with(mContext).load(upload_itme.getUpload_img())
                            .error(R.drawable.vehicle_registration)
                            .fitCenter().into(holder.iv_upload);
                    break;
            }


        }


        holder.iv_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showPictureDialog();
                document_id = upload_itme.getUpload_id();

            }
        });

    }

    @Override
    public int getItemCount() {
        return itemsuploadList.size();
    }

    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        switch (serviceCode) {
            case Const.ServiceCode.UPLOAD_DOC:
                Log.d("mahi", "upload doc" + response);

                if (response != null) {
                    try {
                        JSONObject job = new JSONObject(response);
                        if (job.getString("success").equals("true")) {
                            Commonutils.progressdialog_hide();
                            getDoc();
                        } else {
                            Commonutils.progressdialog_hide();
                            Commonutils.showtoast(context.getResources().getString(R.string.txt_upload_fail), context);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case Const.ServiceCode.GET_DOC:
                Log.d("mahi", "get doc" + response);

                if (response != null) {
                    try {
                        JSONObject job = new JSONObject(response);
                        if (job.getString("success").equals("true")) {
                            Commonutils.progressdialog_hide();
                            itemsuploadList.clear();

                            JSONArray jarray = job.getJSONArray("documents");
                            if (jarray.length() > 0) {
                                for (int i = 0; i < jarray.length(); i++) {
                                    JSONObject docjob = jarray.getJSONObject(i);
                                    Uploads upload = new Uploads();
                                    upload.setUpload_id(docjob.getString("id"));
                                    upload.setUpload_name(docjob.getString("name"));
                                    upload.setUpload_img(docjob.getString("document_url"));
                                    itemsuploadList.add(upload);

                                }

                                notifyDataSetChanged();
                            }

                        } else {
                            Commonutils.progressdialog_hide();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                break;

        }

    }

    private void getDoc() {
        if (!AndyUtils.isNetworkAvailable(mContext)) {
            return;
        }
        Commonutils.progressdialog_show(context, "Loading...");
        HashMap<String, String> map = new HashMap<>();
        map.put(Const.Params.URL, Const.ServiceType.GET_DOC + Const.Params.ID + "="
                + new PreferenceHelper(mContext).getUserId() + "&" + Const.Params.TOKEN + "="
                + new PreferenceHelper(mContext).getSessionToken());

        new VollyRequester(mContext, Const.GET, map, Const.ServiceCode.GET_DOC, this);
    }

    public class typesViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv_upload;
        private TextView tv_upload_name;

        public typesViewHolder(View itemView) {
            super(itemView);
            iv_upload = (ImageView) itemView.findViewById(R.id.iv_upload);
            tv_upload_name = (TextView) itemView.findViewById(R.id.tv_upload_name);

        }
    }

    private void showPictureDialog() {
        /*AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle(mContext.getResources().getString(R.string.txt_slct_option));
        String[] items = {mContext.getResources().getString(R.string.txt_gellery), mContext.getResources().getString(R.string.txt_cameray), mContext.getResources().getString(R.string.txt_warning)};

        dialog.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                switch (which) {
                    case 0:
                        //choosePhotoFromGallary();
                        break;
                    case 1:
                        //takePhotoFromCamera();
                        break;

                }
            }
        });
        dialog.show();*/

        final Dialog dialog = new Dialog(mContext, R.style.DialogSlideAnim_leftright);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.fade_drawable));
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.choose_picture_dialog);
        TextView btn_ok = (TextView) dialog.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void choosePhotoFromGallary() {
        try {
            Intent i = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            ((Activity) context).startActivityForResult(i, 201);
        } catch (Exception e) {
            e.printStackTrace();
            Commonutils.showtoast("Gallery not found!", context);
        }

    }

    private void takePhotoFromCamera() {
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), cal.getTimeInMillis() + ".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        uri = Uri.fromFile(photo);
        ((Activity) context).startActivityForResult(intent, 101);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != mContext.RESULT_OK) {
            return;
        }

        switch (requestCode) {

            case 101:

                if (uri != null) {
                    filepath = getRealPathFromURI(uri);
                    Bitmap compressedImageFile = Compressor.getDefault(context).compressToBitmap(new File(filepath));

                    Uri uri2 = getImageUri(context, compressedImageFile);
                    filepath = getRealPathFromURI(uri2);
                    uploadfile(document_id, filepath);

                } else {
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.txt_img_error),
                            Toast.LENGTH_LONG).show();
                }

                break;
            case 201:
                if (data != null) {

                    uri = data.getData();
                    if (uri != null) {
                        filepath = getRealPathFromURI(uri);
                        Bitmap compressedImageFile = Compressor.getDefault(context).compressToBitmap(new File(filepath));

                        Uri uri3 = getImageUri(context, compressedImageFile);
                        filepath = getRealPathFromURI(uri3);
                        uploadfile(document_id, filepath);

                    } else {
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.txt_img_error),
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;

        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void uploadfile(String document_id, String filepath) {
        Commonutils.progressdialog_show(context, "Uploading...");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Params.URL, Const.ServiceType.UPLOAD_DOC);
        map.put(Const.Params.ID, new PreferenceHelper(mContext).getUserId());
        map.put(Const.Params.TOKEN, new PreferenceHelper(mContext).getSessionToken());
        map.put(Const.Params.DOC_URL, filepath);
        map.put("document_id", document_id);

        Log.d("mahi", map.toString());
        new MultiPartRequester(mContext, map, Const.ServiceCode.UPLOAD_DOC,
                this);

    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = mContext.getContentResolver().query(contentURI, null,
                null, null, null);

        if (cursor == null) { // Source is Dropbox or other similar local file
            // path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor
                    .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

}
