/* FileNameEntry.java

   Copyright (c) 2010 Ethan Chen

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.intervigil.micdroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.EditText;

public class NameEntryDialogFragment extends DialogFragment {

    private static final String TAG = "NameEntryDialog";

    public static final String NAME_ENTRY_RENAME_FILE_KEY = "name_entry_dialog_rename_file_key";
    public static final String NAME_ENTRY_RENAME_FILE_NAME = "name_entry_dialog_rename_file_name";

    public interface NameEntryDialogListener {
        void onRename(String srcName, String destName);

        void onSave(String name);

        void onCancel();
    }

    private NameEntryDialogListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (NameEntryDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement NameEntryDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle icicle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.name_entry_fragment, null))
                .setPositiveButton(R.string.name_entry_btn_ok, mBtnListener)
                .setNegativeButton(R.string.name_entry_btn_cancel, mBtnListener)
                .setTitle(R.string.name_entry_title);
        return builder.create();
    }

    private boolean getIsRename() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getBoolean(NAME_ENTRY_RENAME_FILE_KEY);
        }
        return false;
    }

    private String getRenameSourceName() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(NAME_ENTRY_RENAME_FILE_NAME);
        }
        return null;
    }

    private DialogInterface.OnClickListener mBtnListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mListener == null) return;
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    EditText nameEditText = (EditText) getDialog().findViewById(R.id.name_entry_input);
                    if (nameEditText != null) {
                        String fileName = nameEditText.getText().toString();
                        if (getIsRename()) {
                            mListener.onRename(getRenameSourceName(), fileName);
                        } else {
                            mListener.onSave(fileName);
                        }
                    }
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    getDialog().cancel();
                    mListener.onCancel();
                    break;
            }
        }
    };
}
