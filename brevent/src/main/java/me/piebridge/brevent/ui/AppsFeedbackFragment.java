package me.piebridge.brevent.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/2/5.
 */
public class AppsFeedbackFragment extends DialogFragment implements DialogInterface.OnClickListener {

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
        builder.setTitle(R.string.menu_feedback);
        boolean hasEmailClient = BreventActivity.hasEmailClient(getActivity());
        builder.setMessage(getString(R.string.feedback_message,
                hasEmailClient ? getString(R.string.feedback_message_email) : ""));
        builder.setPositiveButton(R.string.feedback_github, this);
        if (hasEmailClient) {
            builder.setNegativeButton(R.string.feedback_email, this);
        }
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
            openFeedback();
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            BreventActivity.sendEmail(getActivity(), null, Build.FINGERPRINT);
        }
    }

    private void openFeedback() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.FEEDBACK));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        startActivity(intent);
    }

}
