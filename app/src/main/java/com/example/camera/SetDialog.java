package com.example.camera;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import android.text.InputFilter;
import android.text.Spanned;
import android.widget.TextView;

import java.util.Set;

public class SetDialog extends AppCompatDialogFragment implements InputFilter{
    private EditText editextImages;
    private EditText editextFps;
    private EditText edittextFocusRange;
    private EditText edittextExposureTime;
    private DialogListener listener;

    private TextView textImages;
    private TextView textFps;
    private TextView textExposure;
    private TextView textFocus;

    private int min = 0, max = 12;

    private int minImages = 1, maxImages = 10;
    private int minFps = 1, maxFps = 4;
    private int minFocus = 1, maxFocus = 100000;
    private int minExposure = 1000, maxExposure = 300000000;

    public SetDialog() {
        this.min = 0;
        this.max = 0;
    }

    public SetDialog(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public SetDialog(String min, String max) {
        this.min = Integer.parseInt(min);
        this.max = Integer.parseInt(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (isInRange(0, 1000000000, input))
                return null;
        } catch (NumberFormatException nfe) { }
        return "";
    }

    private boolean isInRange(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }


    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog, null);

        Bundle bundle = getArguments();
//        String imageLink = bundle.getString("TEXT","");

        builder.setView(view)
                .setTitle("Settings")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("my logs", "fps: "+ editextFps.getText());

                        String images = editextImages.getText().toString();
                        String fps = editextFps.getText().toString();
                        String focusRange = edittextFocusRange.getText().toString();
                        String exposureTime = edittextExposureTime.getText().toString();
                        if(Integer.parseInt(images) > maxImages)
                            images = String.valueOf(maxImages);
                        if(Integer.parseInt(images) < minImages)
                            images = String.valueOf(minImages);
                        if(Integer.parseInt(fps) > maxFps)
                            fps = String.valueOf(maxFps);
                        if(Integer.parseInt(fps) < minFps)
                            fps = String.valueOf(minFps);
                        if(Integer.parseInt(focusRange) > maxFocus)
                            focusRange = String.valueOf(maxFocus);
                        if(Integer.parseInt(focusRange) < minFocus)
                            focusRange = String.valueOf(minFocus);
                        if(Integer.parseInt(exposureTime) > maxExposure)
                            exposureTime = String.valueOf(maxExposure);
                        if(Integer.parseInt(exposureTime) < minExposure)
                            exposureTime = String.valueOf(minExposure);

                        listener.applyParameters(images, fps, focusRange, exposureTime);
                    }
                });

        editextImages = view.findViewById(R.id.editText4);
        editextFps = view.findViewById(R.id.editText);
        edittextFocusRange = view.findViewById(R.id.editText2);
        edittextExposureTime = view.findViewById(R.id.editText3);

        textImages = view.findViewById(R.id.textView3);
        textImages.setText("ImagesCnt (" + String.valueOf(minImages) + " - " + String.valueOf(maxImages) + ")");
        textFps = view.findViewById(R.id.textView);
        textFps.setText("Fps (" + String.valueOf(minFps) + " - " + String.valueOf(maxFps) + ")");
        textExposure = view.findViewById(R.id.textView4);
        textExposure.setText("ExposureTime (" + String.valueOf(minExposure) + " - " + String.valueOf(maxExposure) + ")");
        textFocus = view.findViewById(R.id.textView2);
        textFocus.setText("FocusRange (" + String.valueOf(minFocus) + " - " + String.valueOf(maxFocus) + ")");

        editextImages.setFilters(new InputFilter[]{ new SetDialog(minImages, maxImages)});
        editextFps.setFilters(new InputFilter[]{ new SetDialog(minFps, maxFps)});
        edittextFocusRange.setFilters(new InputFilter[]{ new SetDialog(minFocus, maxFocus)});
        edittextExposureTime.setFilters(new InputFilter[]{ new SetDialog(minExposure, maxExposure)});

        editextImages.setText(bundle.getString("images",""));
        editextFps.setText(bundle.getString("fps",""));
        edittextFocusRange.setText(bundle.getString("focusRange",""));
        edittextExposureTime.setText(bundle.getString("exposureTime",""));

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (DialogListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement DialogListener");
        }

    }

    public interface DialogListener {
        void applyParameters(String images, String fps, String focusRange, String exposureTime);
    }
}
