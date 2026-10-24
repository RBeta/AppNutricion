package com.rubenbetancourt.registroconsumo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(80, 106, 0);
    private static final int LIME = Color.rgb(201, 214, 0);
    private static final int LIGHT_GREEN = Color.rgb(243, 247, 229);

    private final Calendar selectedDay = Calendar.getInstance();
    private final Plan[] plans = new Plan[]{Plan.ruben(), Plan.sarai()};
    private SharedPreferences preferences;
    private Spinner planSpinner;
    private Spinner mealSpinner;
    private Button dateButton;
    private LinearLayout entriesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("registro_consumo", Context.MODE_PRIVATE);
        setContentView(createScreen());
        updateMealOptions();
    }

    private View createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(12));
        root.setBackgroundColor(Color.rgb(250, 251, 247));

        TextView title = textView("Registro diario de consumo", 24, true);
        title.setTextColor(GREEN);
        root.addView(title, matchWrap());

        TextView subtitle = textView("Marca cada grupo conforme lo consumas.", 15, false);
        subtitle.setTextColor(Color.DKGRAY);
        root.addView(subtitle, matchWrap(0, 0, 0, 14));

        root.addView(label("Plan"), matchWrap());
        planSpinner = new Spinner(this);
        String[] planNames = new String[]{"Rubén", "Saraí"};
        planSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, planNames));
        root.addView(planSpinner, matchWrap(0, 0, 0, 10));

        root.addView(label("Día"), matchWrap());
        dateButton = new Button(this);
        dateButton.setAllCaps(false);
        dateButton.setTextColor(Color.WHITE);
        dateButton.setBackgroundColor(GREEN);
        dateButton.setOnClickListener(view -> chooseDay());
        root.addView(dateButton, matchWrap(0, 0, 0, 10));

        root.addView(label("Comida"), matchWrap());
        mealSpinner = new Spinner(this);
        mealSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(this::renderEntries));
        root.addView(mealSpinner, matchWrap(0, 0, 0, 12));

        TextView instruction = textView("Grupos pendientes para esta comida", 16, true);
        instruction.setTextColor(GREEN);
        root.addView(instruction, matchWrap(0, 0, 0, 6));

        ScrollView scrollView = new ScrollView(this);
        entriesContainer = new LinearLayout(this);
        entriesContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(entriesContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Button resetButton = new Button(this);
        resetButton.setAllCaps(false);
        resetButton.setText("Reiniciar este día");
        resetButton.setTextColor(Color.WHITE);
        resetButton.setBackgroundColor(Color.rgb(115, 55, 30));
        resetButton.setOnClickListener(view -> confirmReset());
        root.addView(resetButton, matchWrap(0, 12, 0, 0));

        planSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(this::updateMealOptions));
        updateDateButton();
        return root;
    }

    private void updateMealOptions() {
        Plan plan = selectedPlan();
        mealSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, plan.meals));
        mealSpinner.setSelection(0);
        renderEntries();
    }

    private void renderEntries() {
        if (entriesContainer == null || mealSpinner.getSelectedItemPosition() < 0) {
            return;
        }

        entriesContainer.removeAllViews();
        Plan plan = selectedPlan();
        int mealIndex = mealSpinner.getSelectedItemPosition();

        for (FoodGroup foodGroup : plan.foodGroups) {
            String portion = foodGroup.portions[mealIndex];
            if (portion.isEmpty()) {
                continue;
            }

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(foodGroup.code + "  ·  " + foodGroup.name + "  ·  " + portion + " porción" + (portion.equals("1") ? "" : "es"));
            checkBox.setTextSize(18);
            checkBox.setPadding(dp(10), dp(8), dp(10), dp(8));
            checkBox.setTextColor(Color.rgb(35, 35, 35));
            checkBox.setBackgroundColor(LIGHT_GREEN);
            checkBox.setChecked(preferences.getBoolean(stateKey(plan, mealIndex, foodGroup.code), false));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> preferences.edit()
                    .putBoolean(stateKey(plan, mealIndex, foodGroup.code), isChecked)
                    .apply());
            entriesContainer.addView(checkBox, matchWrap(0, 0, 0, 8));
        }
    }

    private void chooseDay() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDay.set(year, month, dayOfMonth, 12, 0, 0);
            updateDateButton();
            renderEntries();
        }, selectedDay.get(Calendar.YEAR), selectedDay.get(Calendar.MONTH), selectedDay.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmReset() {
        Plan plan = selectedPlan();
        new AlertDialog.Builder(this)
                .setTitle("Reiniciar día")
                .setMessage("Se desmarcarán todas las comidas de " + plan.name + " para el día seleccionado.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Reiniciar", (dialog, which) -> {
                    SharedPreferences.Editor editor = preferences.edit();
                    for (int mealIndex = 0; mealIndex < plan.meals.length; mealIndex += 1) {
                        for (FoodGroup foodGroup : plan.foodGroups) {
                            editor.remove(stateKey(plan, mealIndex, foodGroup.code));
                        }
                    }
                    editor.apply();
                    renderEntries();
                })
                .show();
    }

    private Plan selectedPlan() {
        int selectedIndex = planSpinner == null ? 0 : planSpinner.getSelectedItemPosition();
        return plans[Math.max(selectedIndex, 0)];
    }

    private String stateKey(Plan plan, int mealIndex, String foodCode) {
        String dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDay.getTime());
        return plan.id + "_" + dayKey + "_" + mealIndex + "_" + foodCode;
    }

    private void updateDateButton() {
        String displayDate = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "MX")).format(selectedDay.getTime());
        dateButton.setText(displayDate.substring(0, 1).toUpperCase(new Locale("es", "MX")) + displayDate.substring(1));
    }

    private TextView label(String value) {
        TextView label = textView(value, 14, true);
        label.setTextColor(Color.rgb(55, 55, 55));
        return label;
    }

    private TextView textView(String value, int size, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(size);
        textView.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        return textView;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return matchWrap(0, 0, 0, 0);
    }

    private LinearLayout.LayoutParams matchWrap(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable action;

        private SimpleItemSelectedListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            action.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }

    private static final class FoodGroup {
        private final String code;
        private final String name;
        private final String[] portions;

        private FoodGroup(String code, String name, String... portions) {
            this.code = code;
            this.name = name;
            this.portions = portions;
        }
    }

    private static final class Plan {
        private final String id;
        private final String name;
        private final String[] meals;
        private final FoodGroup[] foodGroups;

        private Plan(String id, String name, String[] meals, FoodGroup[] foodGroups) {
            this.id = id;
            this.name = name;
            this.meals = meals;
            this.foodGroups = foodGroups;
        }

        private static Plan ruben() {
            return new Plan("ruben", "Rubén",
                    new String[]{"Desayuno", "Almuerzo", "Comida", "Cena", "Colación"},
                    new FoodGroup[]{
                            new FoodGroup("FR", "Frutas", "1", "", "", "", "1"),
                            new FoodGroup("VD", "Verduras", "", "1", "1", "1", "1"),
                            new FoodGroup("CSG", "Cereales sin grasa", "", "2", "2", "2", "1"),
                            new FoodGroup("AOA BG", "AOA bajos en grasa", "", "2", "5", "5", ""),
                            new FoodGroup("AOA AG", "AOA altos en grasa", "", "2", "", "", ""),
                            new FoodGroup("GR", "Grasas", "", "1", "2", "1", ""),
                            new FoodGroup("GCP", "Grasas con proteína", "1", "", "", "", "1")
                    });
        }

        private static Plan sarai() {
            return new Plan("sarai", "Saraí",
                    new String[]{"Almuerzo", "Colación matutina", "Comida", "Colación vespertina", "Cena"},
                    new FoodGroup[]{
                            new FoodGroup("FR", "Frutas", "", "1", "", "1", ""),
                            new FoodGroup("VD", "Verduras", "1", "", "1", "", "1"),
                            new FoodGroup("CSG", "Cereales sin grasa", "2", "1", "2", "1", "2"),
                            new FoodGroup("AOA BG", "AOA bajos en grasa", "", "", "3", "", "3"),
                            new FoodGroup("AOA AG", "AOA altos en grasa", "2", "", "", "", ""),
                            new FoodGroup("LBG", "Lácteos bajos en grasa", "0.5", "", "", "1", ""),
                            new FoodGroup("GR", "Grasas", "2", "", "2", "", "2"),
                            new FoodGroup("GCP", "Grasas con proteína", "", "1", "", "1", "")
                    });
        }
    }
}
