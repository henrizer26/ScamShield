package com.example.scamshield;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.utils.AppPrefs;
import com.example.scamshield.utils.ProtectionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;

/** The app's settings screen. */
public class ProfileFragment extends Fragment {

    private static final String LANG_ENGLISH = "en";
    private static final String LANG_SWAHILI = "sw";

    private TextView tvLanguage;
    private TextView tvTheme;
    private TextView tvProtectionStatus;
    private SwitchMaterial switchProtection;
    private boolean updatingProtectionSwitch;

    private final ActivityResultLauncher<String[]> protectionPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (getContext() == null) return;
                if (ProtectionManager.hasRequiredPermissions(requireContext())) {
                    ProtectionManager.enable(requireContext());
                    Toast.makeText(getActivity(), R.string.protection_enabled_toast,
                            Toast.LENGTH_SHORT).show();
                } else {
                    ProtectionManager.disable(requireContext());
                    Toast.makeText(getActivity(), R.string.protection_permission_denied,
                            Toast.LENGTH_LONG).show();
                }
                updateProtectionState();
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvLanguage = view.findViewById(R.id.tvLanguage);
        tvTheme = view.findViewById(R.id.tvTheme);
        tvProtectionStatus = view.findViewById(R.id.tvProtectionStatus);
        switchProtection = view.findViewById(R.id.switchProtection);

        switchProtection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingProtectionSwitch) return;
            if (isChecked) {
                enableProtection();
            } else {
                ProtectionManager.disable(requireContext());
                updateProtectionState();
            }
        });

        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> showLanguageDialog());
        view.findViewById(R.id.rowTheme).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.rowNotifications).setOnClickListener(v -> openNotificationSettings());
        view.findViewById(R.id.rowPrivacy).setOnClickListener(v ->
                showInfoDialog(R.string.settings_privacy, R.string.privacy_message));
        view.findViewById(R.id.rowHelp).setOnClickListener(v ->
                showInfoDialog(R.string.settings_help, R.string.help_message));
        view.findViewById(R.id.rowAbout).setOnClickListener(v ->
                showInfoDialog(R.string.settings_about, R.string.about_message));
        view.findViewById(R.id.rowReset).setOnClickListener(v -> confirmReset());

        updateSummaries();
        updateProtectionState();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
        updateProtectionState();
    }

    private void updateSummaries() {
        tvLanguage.setText(isSwahili() ? R.string.language_swahili : R.string.language_english);
        tvTheme.setText(AppPrefs.isDarkTheme(requireContext())
                ? R.string.theme_dark
                : R.string.theme_light);
    }

    private void enableProtection() {
        if (ProtectionManager.hasRequiredPermissions(requireContext())) {
            ProtectionManager.enable(requireContext());
            Toast.makeText(getActivity(), R.string.protection_enabled_toast,
                    Toast.LENGTH_SHORT).show();
            updateProtectionState();
            return;
        }

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECEIVE_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        protectionPermissionLauncher.launch(permissions.toArray(new String[0]));
    }

    private void updateProtectionState() {
        if (ProtectionManager.isEnabled(requireContext())
                && !ProtectionManager.hasRequiredPermissions(requireContext())) {
            ProtectionManager.disable(requireContext());
        }
        boolean active = ProtectionManager.isActive(requireContext());
        updatingProtectionSwitch = true;
        switchProtection.setChecked(active);
        updatingProtectionSwitch = false;

        if (active) {
            tvProtectionStatus.setText(R.string.protection_active);
        } else if (!ProtectionManager.hasRequiredPermissions(requireContext())) {
            tvProtectionStatus.setText(R.string.protection_needs_permission);
        } else {
            tvProtectionStatus.setText(R.string.protection_inactive);
        }
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

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_choose_theme)
                .setSingleChoiceItems(options, AppPrefs.isDarkTheme(requireContext()) ? 1 : 0,
                        (dialog, which) -> {
                            dialog.dismiss();
                            AppPrefs.setDarkTheme(requireContext(), which == 1);
                        })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showInfoDialog(int titleRes, int messageRes) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
        }
        startActivity(intent);
    }

    /**
     * Clears what the app keeps on the phone and starts again at the intro.
     */
    private void confirmReset() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_message)
                .setPositiveButton(R.string.settings_reset, (dialog, which) -> {
                    ProtectionManager.disable(requireContext());
                    new DatabaseHelper(requireContext()).deleteAllScams();
                    AppPrefs.reset(requireContext());
                    Toast.makeText(getActivity(), R.string.toast_data_cleared,
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(requireContext(), SplashActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
