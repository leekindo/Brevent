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
public class AppsRootFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private Dialog mDialog;

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
        builder.setMessage(R.string.unsupported_disable_root);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNeutralButton(R.string.root_donate_later, this);
        return builder.create();
    }

    @Override
    public void onStop() {
        super.onStop();
        mDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (DialogInterface.BUTTON_POSITIVE == which) {
            ((BreventActivity) getActivity()).openSettings();
        } else if (DialogInterface.BUTTON_NEUTRAL == which) {
            dismiss();
        }
    }

}
