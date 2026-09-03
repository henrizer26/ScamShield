package com.example.scamshield;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import com.example.scamshield.utils.AppPrefs;

public class SettingsFragment extends Fragment {

    private static final String LANG_ENGLISH = "en";
    private static final String LANG_SWAHILI = "sw";

    private TextView tvLanguage;
    private TextView tvTheme;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tvLanguage = view.findViewById(R.id.tvLanguage);
        tvTheme = view.findViewById(R.id.tvTheme);

        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> showLanguageDialog());
        view.findViewById(R.id.rowTheme).setOnClickListener(v -> showThemeDialog());

        updateSummaries();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
    }

    private void updateSummaries() {
        tvLanguage.setText(isSwahili() ? R.string.language_swahili : R.string.language_english);
        tvTheme.setText(AppPrefs.isDarkTheme(requireContext())
                ? R.string.theme_dark
                : R.string.theme_light);
    }

    private boolean isSwahili() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (!locales.isEmpty()) {
            return LANG_SWAHILI.equals(locales.get(0).getLanguage());
        }
        // No explicit choice yet - report whatever the resources actually resolved to.
        return LANG_SWAHILI.equals(
                getResources().getConfiguration().getLocales().get(0).getLanguage());
    }

    private void showLanguageDialog() {
        String[] options = {
                getString(R.string.language_english),
                getString(R.string.language_swahili)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_choose_language)
                .setSingleChoiceItems(options, isSwahili() ? 1 : 0, (dialog, which) -> {
                    dialog.dismiss();
                    // Recreates the activity, so every screen picks up the new locale.
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(
                            which == 1 ? LANG_SWAHILI : LANG_ENGLISH));
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showThemeDialog() {
        String[] options = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        boolean dark = AppPrefs.isDarkTheme(requireContext());

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_choose_theme)
                .setSingleChoiceItems(options, dark ? 1 : 0, (dialog, which) -> {
                    dialog.dismiss();
                    AppPrefs.setDarkTheme(requireContext(), which == 1);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
