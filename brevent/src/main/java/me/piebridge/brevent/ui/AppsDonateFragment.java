package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;
import me.piebridge.donation.DonateActivity;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsDonateFragment extends DialogFragment implements DialogInterface.OnClickListener,
        DialogInterface.OnShowListener {

    private Dialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog == null) {
            mDialog = createDialog();
        }
        return mDialog;
    }

    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.root_donate_required);
        builder.setPositiveButton(R.string.root_donate_alipay, this);
        builder.setNegativeButton(R.string.root_donate_email, this);
        builder.setNeutralButton(R.string.root_donate_later, this);
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(this);
        return alertDialog;
    }

    @Override
    public void onStop() {
        super.onStop();
        mDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (DialogInterface.BUTTON_POSITIVE == which) {
            openAlipay();
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            BreventApplication application = (BreventApplication) getActivity().getApplication();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.root_donate_email_subject,
                    Long.toHexString(application.getId())));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.root_donate_email_text));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {BuildConfig.EMAIL});
            BreventActivity.sendEmail(getActivity(), intent);
        } else if (DialogInterface.BUTTON_NEUTRAL == which) {
            dismiss();
        }
    }

    private void openAlipay() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATE_ALIPAY));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (getActivity() == null) {
            return;
        }
        BreventApplication application = (BreventApplication) getActivity().getApplication();
        if (application.isUnsafe() || application.getPackageManager()
                .getLaunchIntentForPackage(DonateActivity.PACKAGE_ALIPAY) == null) {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
        if (!BreventActivity.hasEmailClient(application)) {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        }
    }

}
