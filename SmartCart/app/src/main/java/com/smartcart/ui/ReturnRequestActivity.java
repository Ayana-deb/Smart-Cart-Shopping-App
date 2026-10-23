package com.smartcart.ui;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.AppConfig;
import com.smartcart.logic.ReturnEligibilityManager;
import com.smartcart.model.Constants;
import com.smartcart.model.Order;
import com.smartcart.model.OrderItem;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Customer return request form. All policy checks run in ReturnEligibilityManager. */
public class ReturnRequestActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private final List<Order> orders = new ArrayList<>();
    private List<OrderItem> items = new ArrayList<>();
    private int qty = 1;
    private long pendingProduct;
    private Spinner spOrder, spProduct;
    private TextView tvQty, tvError;
    private ImageView ivItemPhoto;
    private String evidencePath;
    private String cameraTempPath;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_return_request);
        Util.toolbar(this, "Request a return", true);
        DBHelper db = DBHelper.get(this);
        long uid = Session.of(this).userId();
        spOrder = findViewById(R.id.spOrder);
        spProduct = findViewById(R.id.spProduct);
        Spinner spReason = findViewById(R.id.spReason);
        tvQty = findViewById(R.id.tvQty);
        tvError = findViewById(R.id.tvError);
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
        TextView tvInfo = findViewById(R.id.tvInfo);

        for (Order o : db.getOrders(uid)) if (Constants.DELIVERED.equals(o.status)) orders.add(o);
        int window = AppConfig.get(this, AppConfig.RETURN_WINDOW_DAYS);
        if (orders.isEmpty()) {
            tvInfo.setText("You have no delivered orders yet. Once an order is delivered you can return items within " + window + " days.\n\nTip: open an order and use the demo button to mark it as delivered.");
            findViewById(R.id.llForm).setVisibility(View.GONE);
            return;
        }
        tvInfo.setText("Returns are accepted for delivered orders within " + window + " days of delivery. Pickup and refund are simulated locally.");

        long preOrder = getIntent().getLongExtra("oid", 0);
        pendingProduct = getIntent().getLongExtra("pid", 0);
        List<String> labels = new ArrayList<>();
        int sel = 0;
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            labels.add(o.code + "  \u2022  delivered " + Util.date(o.deliveredAt) + " (" + Util.daysSince(o.deliveredAt) + "d ago)");
            if (o.id == preOrder) sel = i;
        }
        spOrder.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        spOrder.setSelection(sel);
        spOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { loadItems(db, pos); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        spReason.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Constants.REASONS));

        findViewById(R.id.btnMinus).setOnClickListener(v -> { if (qty > 1) { qty--; tvQty.setText(String.valueOf(qty)); } });
        findViewById(R.id.btnPlus).setOnClickListener(v -> { if (qty < 99) { qty++; tvQty.setText(String.valueOf(qty)); } });

        TextInputEditText etDesc = findViewById(R.id.etDesc);
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btnRemovePhoto).setOnClickListener(v -> removePhoto());
        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            int op = spOrder.getSelectedItemPosition(), pp = spProduct.getSelectedItemPosition();
            if (op < 0 || pp < 0 || pp >= items.size()) { showError("Please select an order and a product."); return; }
            if (evidencePath == null || evidencePath.trim().isEmpty()) {
                showError("Take a photo of the item before submitting your return request.");
                return;
            }
            ReturnEligibilityManager.Result r = new ReturnEligibilityManager(this).submit(
                    uid, orders.get(op).id, items.get(pp).productId, qty,
                    (String) spReason.getSelectedItem(), etDesc.getText().toString(), evidencePath);
            if (!r.ok) { showError(r.message); return; }
            new AlertDialog.Builder(this)
                    .setTitle("Request #" + r.requestId + " submitted")
                    .setMessage(r.message)
                    .setCancelable(false)
                    .setPositiveButton("Track my return", (d, w) -> {
                        startActivity(new Intent(this, ReturnHistoryActivity.class));
                        finish();
                    })
                    .show();
        });
    }

    private void loadItems(DBHelper db, int orderPos) {
        items = db.getOrderItems(orders.get(orderPos).id);
        List<String> labels = new ArrayList<>();
        int sel = 0;
        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            labels.add(it.emoji + " " + it.name + "  (bought " + it.quantity + ")");
            if (it.productId == pendingProduct) sel = i;
        }
        pendingProduct = 0;
        spProduct.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        spProduct.setSelection(sel);
        qty = 1;
        tvQty.setText("1");
    }

    private void showError(String msg) {
        tvError.setText("\u26A0\uFE0F  " + msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void takePhoto() {
        try {
            File directory = new File(getFilesDir(), "return-evidence");
            if (!directory.exists() && !directory.mkdirs())
                throw new IOException("Could not create the photo storage folder.");
            File photo = File.createTempFile("capture-", ".jpg", directory);
            cameraTempPath = photo.getAbsolutePath();
            Uri output = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) == null) {
                photo.delete();
                cameraTempPath = null;
                showError("No camera app is available. A camera photo is required to submit a return request.");
                return;
            }
            try {
                startActivityForResult(intent, REQUEST_CAMERA);
            } catch (ActivityNotFoundException e) {
                deleteCameraTemp();
                showError("No camera app is available. A camera photo is required to submit a return request.");
            }
        } catch (IOException | IllegalArgumentException e) {
            showError("Could not start the camera: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAMERA) return;
        if (resultCode != RESULT_OK) {
            deleteCameraTemp();
            return;
        }

        try {
            if (cameraTempPath == null) {
                showError("Could not read the camera photo. Please try again.");
                return;
            }
            Bitmap bitmap;
            try {
                bitmap = decodeSampledBitmap(cameraTempPath);
            } finally {
                deleteCameraTemp();
            }
            if (bitmap == null) {
                showError("The camera photo could not be read. Please take another photo.");
                return;
            }
            File directory = new File(getFilesDir(), "return-evidence");
            if (!directory.exists() && !directory.mkdirs())
                throw new IOException("Could not create the photo storage folder.");
            File destination = new File(directory, UUID.randomUUID() + ".jpg");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(destination)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out))
                    throw new IOException("Could not save the photo.");
            } finally {
                bitmap.recycle();
            }
            evidencePath = destination.getAbsolutePath();
            showPhoto();
        } catch (IOException e) {
            showError("Could not attach the photo: " + e.getMessage());
        }
    }

    private Bitmap decodeSampledBitmap(Uri uri) throws IOException {
        android.graphics.BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Unable to open the selected photo.");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        return decodeSampledBitmap(bounds, () -> getContentResolver().openInputStream(uri));
    }

    private Bitmap decodeSampledBitmap(String path) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        return decodeSampledBitmap(bounds, () -> new java.io.FileInputStream(path));
    }

    private Bitmap decodeSampledBitmap(BitmapFactory.Options bounds, StreamOpener opener) throws IOException {
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) sampleSize *= 2;
        options.inSampleSize = sampleSize;
        try (java.io.InputStream in = opener.open()) {
            if (in == null) throw new IOException("Unable to open the selected photo.");
            return BitmapFactory.decodeStream(in, null, options);
        }
    }

    private interface StreamOpener { java.io.InputStream open() throws IOException; }

    private void showPhoto() {
        ivItemPhoto.setImageBitmap(decodePreview(evidencePath));
        ivItemPhoto.setVisibility(View.VISIBLE);
        findViewById(R.id.btnRemovePhoto).setVisibility(View.VISIBLE);
    }

    private Bitmap decodePreview(String path) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) sampleSize *= 2;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(path, options);
    }

    private void removePhoto() {
        if (evidencePath != null) new File(evidencePath).delete();
        evidencePath = null;
        ivItemPhoto.setImageDrawable(null);
        ivItemPhoto.setVisibility(View.GONE);
        findViewById(R.id.btnRemovePhoto).setVisibility(View.GONE);
    }

    private void deleteCameraTemp() {
        if (cameraTempPath != null) new File(cameraTempPath).delete();
        cameraTempPath = null;
    }
}
