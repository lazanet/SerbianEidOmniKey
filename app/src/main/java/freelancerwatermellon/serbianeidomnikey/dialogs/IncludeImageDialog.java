package freelancerwatermellon.serbianeidomnikey.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import freelancerwatermellon.serbianeidomnikey.R;


/**
 * Created by 1 on 1/28/2016.
 */
public class IncludeImageDialog {
    AlertDialog.Builder builder;

    public IncludeImageDialog(Context context) {
        builder = new AlertDialog.Builder(context);

        // Use the Builder class for convenient dialog construction
        builder.setTitle(R.string.include_image_dialog_title);
        builder.setMessage(R.string.include_image_dialog_msg);
        builder.setCancelable(false);
    }

    public void setPositiveButtonListener(DialogInterface.OnClickListener listener) {
        builder.setPositiveButton(R.string.include_positive, listener);
    }

    public void setNegativeButtonListener(DialogInterface.OnClickListener listener) {
        builder.setNegativeButton(R.string.include_negative, listener);
    }

    public Dialog create() {
        // Create the AlertDialog object and return
        return builder.create();
    }
}
