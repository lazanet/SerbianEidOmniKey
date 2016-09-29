package freelancerwatermellon.serbianeidomnikey.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import freelancerwatermellon.serbianeidomnikey.R;


/**
 * Created by 1 on 1/28/2016.
 */
public class EnterKeyDialog {
    AlertDialog.Builder builder;
    private String dialogMessage;
    private View v;
    private EditText et_key;

    public EnterKeyDialog(Context context) {
        builder = new AlertDialog.Builder(context);

        // Use the Builder class for convenient dialog construction
        builder.setTitle(R.string.enter_key_dialog_title);
        builder.setMessage(dialogMessage);
        builder.setCancelable(false);
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        v = inflater.inflate(R.layout.enter_key_dialog, null);
        et_key = (EditText) v.findViewById(R.id.et_key);
        builder.setView(v);

    }

    public void setDialogMessage(String msg) {
        dialogMessage = msg;
        builder.setMessage(dialogMessage.toUpperCase());
    }

    public String getEnteredValue() {
        return et_key.getText().toString();
    }

    public void setPositiveButtonListener(DialogInterface.OnClickListener listener) {
        builder.setPositiveButton(R.string.ok, listener);
    }

    public void setNegativeButtonListener(DialogInterface.OnClickListener listener) {
        builder.setNegativeButton(R.string.cancel, listener);
    }

    public Dialog create() {
        // Create the AlertDialog object and return
        return builder.create();
    }
}
