package com.example.productcalc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static android.database.DatabaseUtils.queryNumEntries;

public class MainActivity extends AppCompatActivity {

    public static final int PLACES = 2;
    public static final String TAG = "TAG";
    public static final long NOT_ID = -1;
    public static final int DEFAULT_UNITS = 100;
    static final String ET_NAME_SIS = "ET_NAME_SIS";
    static final String ET_ANOTHER_SIS = "ET_ANOTHER_SIS";
    static final String ET_WEIGHT_SIS = "ET_WEIGHT_SIS";
    static final String ET_PRICE_SIS = "ET_PRICE_SIS";
    static final String OPTIONS_SIS = "OPTIONS_SIS";
    static final String PRODUCT_ID_SIS = "PRODUCT_ID_SIS";
    static final String BUFFER_ET_SIS = "BUFFER_ET_SIS";
    static final String SETTINGS_SP = "SETTINGS_SP";
    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;
    String measure;
    TextView customInfoGrid, header;
    ListView listView;
    DatabaseHelper dbHelper;
    SQLiteDatabase db;
    Cursor cursor, cursor_local;
    SimpleCursorAdapter productAdapter;
    private boolean VIEW_ALL_ELEMENT;
    private FloatingActionButton btnSend, btnReCalc, btnCloseEdit;
    private EditText eName, ePrice, eWeight, eAnother;
    private long productId = NOT_ID;
    private String[] bufferEditText = new String[]{"", "", "", ""};

    //Округление чисел
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        eName = findViewById(R.id.prod_ed_name);
        eAnother = findViewById(R.id.prod_ed_set_weight);
        eWeight = findViewById(R.id.prod_ed_weight);
        ePrice = findViewById(R.id.prod_ed_price);
        outState.putString(ET_NAME_SIS, eName.getText().toString());
        outState.putString(ET_NAME_SIS, eName.getText().toString());
        outState.putString(ET_ANOTHER_SIS, eAnother.getText().toString());
        outState.putString(ET_WEIGHT_SIS, eWeight.getText().toString());
        outState.putString(ET_PRICE_SIS, ePrice.getText().toString());
        outState.putBoolean(OPTIONS_SIS, VIEW_ALL_ELEMENT);
        outState.putLong(PRODUCT_ID_SIS, productId);
        outState.putStringArray(BUFFER_ET_SIS, bufferEditText);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        eName = findViewById(R.id.prod_ed_name);
        eAnother = findViewById(R.id.prod_ed_set_weight);
        eWeight = findViewById(R.id.prod_ed_weight);
        ePrice = findViewById(R.id.prod_ed_price);
        productId = savedInstanceState.getLong(PRODUCT_ID_SIS);
        bufferEditText = savedInstanceState.getStringArray(BUFFER_ET_SIS);
        eName.setText(savedInstanceState.getString(ET_NAME_SIS));
        eAnother.setText(savedInstanceState.getString(ET_ANOTHER_SIS));
        eWeight.setText(savedInstanceState.getString(ET_WEIGHT_SIS));
        ePrice.setText(savedInstanceState.getString(ET_PRICE_SIS));
        VIEW_ALL_ELEMENT = savedInstanceState.getBoolean(OPTIONS_SIS);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.context_menu_edit:
                onContextItemEditData(info.id);
                return true;
            case R.id.context_menu_delete:
                onContextItemDeleteData(info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        VIEW_ALL_ELEMENT = settings.getBoolean(SETTINGS_SP, false);
        eName = findViewById(R.id.prod_ed_name);
        eAnother = findViewById(R.id.prod_ed_set_weight);
        eWeight = findViewById(R.id.prod_ed_weight);
        ePrice = findViewById(R.id.prod_ed_price);
        customInfoGrid = findViewById(R.id.grid_custom);
        btnSend = findViewById(R.id.mess_btnSend);
        btnReCalc = findViewById(R.id.button_set_weight);
        btnCloseEdit = findViewById(R.id.button_сlose_edit);
        header = findViewById(R.id.header);
        listView = findViewById(R.id.grid_view);
        eWeight.requestFocus();
        //TODO:внедрить вес
        measure = "г";
        eWeight.setOnFocusChangeListener((v, hasFocus) -> {
            //запускаем клавиатуру
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(eWeight, InputMethodManager.SHOW_IMPLICIT);
        });
        registerForContextMenu(listView);
        listView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
            Log.d(TAG, "onCreateContextMenu");
        });
        listView.setOnItemClickListener((parent, view, position, id) -> (MainActivity.this).openContextMenu(view));
        btnSend.setOnClickListener(v -> MainActivity.this.onClickSend());
        btnReCalc.setOnClickListener(v -> (MainActivity.this).onClickReCalc());
        btnCloseEdit.setOnClickListener(v -> {
            (MainActivity.this).finishEditData();
            MainActivity.this.productShowInfo();
        });
        dbHelper = new DatabaseHelper(getApplicationContext());
    }

    private void onClickReCalc() {
        if (eAnother.getText().toString().isEmpty()) {
            Toast.makeText(MainActivity.this, "Заполните поле", Toast.LENGTH_SHORT).show();
            return;
        }
        MainActivity.this.updateCustomData();
        MainActivity.this.productShowInfo();
    }

    private void onClickSend() {
        if (checkInputData()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.KEY_NAME, eName.getText().toString());
            contentValues.put(DatabaseHelper.KEY_WEIGHT, Double.parseDouble(eWeight.getText().toString()));
            contentValues.put(DatabaseHelper.KEY_PRICE, Double.parseDouble(ePrice.getText().toString()));
            contentValues.put(DatabaseHelper.KEY_UNITS, Integer.parseInt(eAnother.getText().toString()));
            contentValues.put(DatabaseHelper.KEY_RES_UNIT, calcUnit());
            contentValues.put(DatabaseHelper.KEY_RES_UNITS, calcUnits());
            contentValues.put(DatabaseHelper.KEY_MEASURE, measure);
            Log.d(TAG, "id: " + productId);
            if (productId == NOT_ID) {
                db.insert(DatabaseHelper.TABLE_PRODUCTS, null, contentValues);
                Arrays.fill(bufferEditText, "");
                eName.setText("");
                ePrice.setText("");
                eWeight.setText("");
            } else {
                db.update(DatabaseHelper.TABLE_PRODUCTS, contentValues, DatabaseHelper.KEY_ID + "=" + productId, null);
                finishEditData();
            }
            productShowInfo();
            eWeight.requestFocus();
        }
    }

    private void finishEditData() {
        productId = NOT_ID;
        Log.d(TAG, "buf name: " + bufferEditText[0]);
        Log.d(TAG, "buf units: " + bufferEditText[1]);
        Log.d(TAG, "buf weight: " + bufferEditText[2]);
        Log.d(TAG, "buf price: " + bufferEditText[3]);
        eName.setText(bufferEditText[0]);
        eAnother.setText(bufferEditText[1]);
        ePrice.setText(bufferEditText[2]);
        eWeight.setText(bufferEditText[3]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        productShowInfo();
    }

    @SuppressLint("SetTextI18n")
    private void productShowInfo() {
        db = dbHelper.getWritableDatabase();
        cursor = db.rawQuery("SELECT * from " + DatabaseHelper.TABLE_PRODUCTS, null);
        if (VIEW_ALL_ELEMENT) {
            //показываем все элементы
            customInfoGrid.setVisibility(View.VISIBLE);
            eAnother.setVisibility(View.VISIBLE);
            //Проверка на режим редактирования
            if (productId == NOT_ID) {
                btnReCalc.setVisibility(View.VISIBLE);
                btnCloseEdit.setVisibility(View.GONE);
            } else {
                btnReCalc.setVisibility(View.GONE);
                btnCloseEdit.setVisibility(View.VISIBLE);
            }
            String[] headers = new String[]{DatabaseHelper.KEY_NAME, DatabaseHelper.KEY_WEIGHT, DatabaseHelper.KEY_PRICE,
                    DatabaseHelper.KEY_RES_UNIT, DatabaseHelper.KEY_UNITS,
                    DatabaseHelper.KEY_RES_UNITS, DatabaseHelper.KEY_MEASURE};
            productAdapter = new SimpleCursorAdapter(this, R.layout.item_product_all, cursor, headers,
                    new int[]{R.id.ip_name, R.id.ip_weight, R.id.ip_price, R.id.ip_res_unit,
                            R.id.ip_units, R.id.ip_res_units, R.id.ip_measure}, 0);
        } else {
            //скрываем столбец и кнопку
            //Проверка на режим редактирования
            if (productId == NOT_ID)
                btnCloseEdit.setVisibility(View.GONE);
            else
                btnCloseEdit.setVisibility(View.VISIBLE);
            customInfoGrid.setVisibility(View.GONE);
            eAnother.setVisibility(View.GONE);
            btnReCalc.setVisibility(View.GONE);
            String[] headers = new String[]{DatabaseHelper.KEY_NAME, DatabaseHelper.KEY_WEIGHT, DatabaseHelper.KEY_PRICE,
                    DatabaseHelper.KEY_RES_UNIT};
            productAdapter = new SimpleCursorAdapter(this, R.layout.item_product, cursor, headers,
                    new int[]{R.id.ip_name, R.id.ip_weight, R.id.ip_price, R.id.ip_res_unit}, 0);
        }
        header.setText("Элементов в таблице: " + cursor.getCount());
        listView.setAdapter(productAdapter);
    }

    @SuppressLint("SetTextI18n")
    private boolean checkInputData() {
        if (ePrice.getText().toString().isEmpty() ||
                eWeight.getText().toString().isEmpty() ||
                (eAnother.getText().toString().isEmpty() && VIEW_ALL_ELEMENT)) {
            if (productId != NOT_ID) {
                onContextItemDeleteData(productId);
                return false;
            }
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Double.parseDouble(eWeight.getText().toString()) == 0) {
            Toast.makeText(this, "Укажите не нулевой вес", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Double.parseDouble(ePrice.getText().toString()) == 0) {
            Toast.makeText(this, "Укажите не нулевую цену", Toast.LENGTH_SHORT).show();
            return false;
        }
        //проверка на скрытое пустое поле
        if (eAnother.getText().toString().isEmpty() && !VIEW_ALL_ELEMENT) {
            eAnother.setText(String.valueOf(DEFAULT_UNITS));
        }
        if (eName.getText().toString().isEmpty()) {
            eName.setText("Продукт");
        }
        return true;
    }

    private void onContextItemEditData(long id) {
        //сохраняем данные введеные до редактироваия
        //условие во избежение многокрантного редактирования
        if (productId == NOT_ID) {
            bufferEditText[0] = eName.getText().toString();
            bufferEditText[1] = eAnother.getText().toString();
            bufferEditText[2] = eWeight.getText().toString();
            bufferEditText[3] = ePrice.getText().toString();
        }
        productId = id;
        cursor = db.rawQuery("select * from " + DatabaseHelper.TABLE_PRODUCTS + " where " +
                DatabaseHelper.KEY_ID + "=?", new String[]{String.valueOf(id)});
        cursor.moveToFirst();
        //заполняем данные из бд
        eName.setText(cursor.getString(1));
        eWeight.setText(cursor.getString(2));
        ePrice.setText(cursor.getString(3));
        eAnother.setText(cursor.getString(5));
        cursor.close();
        //включили кнопку отмены
        btnCloseEdit.setVisibility(View.VISIBLE);
        btnReCalc.setVisibility(View.GONE);
    }

    private void updateCustomData() {
        Log.d(TAG, "Количество строк в таблице = " + queryNumEntries(db, DatabaseHelper.TABLE_PRODUCTS));
        ContentValues updatedValues = new ContentValues();
        cursor.moveToFirst();
        //Цикл по всем элементам в бд
        //TODO: оптимизировать цикл
        for (int rowId = 0; rowId < cursor.getCount(); rowId++) {
            Log.d(TAG, "get id: " + cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID)));
            updatedValues.put(DatabaseHelper.KEY_RES_UNITS, recalcUnits(
                    cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID))));
            updatedValues.put(DatabaseHelper.KEY_UNITS, Integer.parseInt(eAnother.getText().toString()));
            db.update(DatabaseHelper.TABLE_PRODUCTS, updatedValues,
                    DatabaseHelper.KEY_ID + "=" + cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID)), null);
            cursor.moveToNext();
        }
        cursor.close();
    }

    private void onContextItemDeleteData(long id) {
        //диалоговое окно об удалении item
        db = dbHelper.getWritableDatabase();
        cursor = db.rawQuery("select * from " + DatabaseHelper.TABLE_PRODUCTS + " where " +
                DatabaseHelper.KEY_ID + "=?", new String[]{String.valueOf(id)});
        cursor.moveToFirst();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Внимание").setMessage("Удалить элемент: " + cursor.getString(1)).setPositiveButton("да", (dialog, which) -> {
            //Удаляем данные
            try {
                db.delete(DatabaseHelper.TABLE_PRODUCTS, "_id = ?", new String[]{String.valueOf(id)});
                Toast.makeText(MainActivity.this, "Успешно удалено", Toast.LENGTH_SHORT).show();
                //Если пришли из редактирования
                if (productId != NOT_ID) {
                    finishEditData();
                }
                productShowInfo();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Возникла ошибка: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("нет", (dialog, which) -> {
        }).show();
        cursor.close();
    }

    private void deleteAllData() {
        try {
            db.delete(DatabaseHelper.TABLE_PRODUCTS, null, null);
            Toast.makeText(MainActivity.this, "Все записи удалены", Toast.LENGTH_SHORT).show();
            productShowInfo();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Возникла ошибка: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private double calcUnit() {
        return round(Double.parseDouble(ePrice.getText().toString()) / Double.parseDouble(eWeight.getText().toString()), PLACES);
    }

    private double calcUnits() {
        return round(Double.parseDouble(ePrice.getText().toString()) * Integer.parseInt(eAnother.getText().toString()) /
                Double.parseDouble(eWeight.getText().toString()), PLACES);
    }

    private double recalcUnits(long rowId) {
        //Ставим курсор на переданную строку
        cursor_local = db.rawQuery("select * from " + DatabaseHelper.TABLE_PRODUCTS + " where " +
                DatabaseHelper.KEY_ID + "=?", new String[]{String.valueOf(rowId)});
        cursor_local.moveToFirst();
        double res = round(Double.parseDouble(String.valueOf(cursor_local.getDouble(3))) *
                Integer.parseInt(eAnother.getText().toString()) /
                Double.parseDouble(String.valueOf(cursor_local.getDouble(2))), PLACES);
        cursor_local.close();
        return res;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
        cursor.close();
    }

    @SuppressLint({"NonConstantResourceId", "CommitPrefEdits"})
    public void onClickOptionsMenu(MenuItem item) {
        switch (item.getItemId()) {
            //TODO:реализовать сортировку
            /*case R.id.options_menu_sort:
                break;*/
            case R.id.options_menu_settings:
                VIEW_ALL_ELEMENT = !VIEW_ALL_ELEMENT;
                //Сохранение настроек
                prefEditor = settings.edit();
                prefEditor.putBoolean(SETTINGS_SP, VIEW_ALL_ELEMENT);
                prefEditor.apply();
                productShowInfo();
                break;
            case R.id.options_menu_clear:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Внимание").setMessage("Вы хотите безвозвратно удалить все элементы?")
                        .setPositiveButton("да", (dialog, which) -> {
                            //удаляем данные из бд
                            deleteAllData();
                        }).setNegativeButton("нет", (dialog, which) -> {
                }).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (productId != NOT_ID) {
            finishEditData();
            productShowInfo();
            return;
        }
        openQuitDialog();
    }

    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(MainActivity.this);
        quitDialog.setTitle("Выйти из приложения?").setPositiveButton("Выйти", (dialog, which) -> finish()).setNegativeButton("Остаться", (dialog, which) -> {
        }).show();
    }
}