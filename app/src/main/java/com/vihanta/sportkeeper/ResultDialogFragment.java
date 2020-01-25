package com.vihanta.sportkeeper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;


public class ResultDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = ResultDialogFragment.class.getSimpleName();

    @Inject
    TrailModel mModel;
    @Inject Utils utils;

    private Button saveButton;
    private Button cancelButton;

    private TextView time;
    private TextView distance;
    private TextView speed;
    private Spinner typesSelection;

    private EditText title;
    private EditText type;

    private LocalBroadcastManager broadcaster;


    RequestQueue queue;

    private View.OnClickListener cancel = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d(TAG, "canceled");
            dismiss();
        }
    };
    private View.OnClickListener save = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d(TAG, "save");

            final String s = mModel.toJson( title.getText().toString(),
                                            typesSelection.getSelectedItem().toString() );

            String url = "http://ippe.kapsi.fi/sportkeeper/insert";
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>()
                    {
                        @Override
                        public void onResponse(String response) {
                            // response
                            Log.d(TAG,"Request OK");

                            dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setMessage("Activity Saved!")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            /*Intent intent = new Intent(MapsActivity.SAVED_ACTION);
                                            broadcaster.sendBroadcast(intent);*/
                                            dismiss();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();


                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            Log.e(TAG, "errori");
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams()
                {
                    Map<String, String>  params = new HashMap<String, String>();
                    params.put("json", s);
                    //params.put("geo", mModel.);

                    return params;
                }
            };
            queue.add(postRequest);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ((SKapp)getActivity().getApplication()).getmMainComponent().inject(this);
        // Inflate the layout to use as dialog or embedded fragment
        final View view = inflater.inflate(R.layout.result, container, false);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        cancelButton =  (Button) view.findViewById(R.id.dialog_cancel);
        saveButton=  (Button) view.findViewById(R.id.dialog_save);
        time  =  (TextView) view.findViewById(R.id.dialog_time);
        distance =  (TextView) view.findViewById(R.id.dialog_dist);
        speed    =  (TextView) view.findViewById(R.id.dialog_speed);

        title =  (EditText) view.findViewById(R.id.dialog_title);
        //type =  (EditText) view.findViewById(R.id.dialog_type);


        typesSelection = view.findViewById(R.id.types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.getContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                mModel.getTypeNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typesSelection.setAdapter(adapter);

        saveButton.setOnClickListener(save);
        cancelButton.setOnClickListener(cancel);

        queue = Volley.newRequestQueue(getContext());
        typesSelection.setOnItemSelectedListener(this);

        broadcaster = LocalBroadcastManager.getInstance(getActivity().getBaseContext());

        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle("Activity Results");

    }

    @Override
    public void onStart() {
        super.onStart();

        typesSelection.setSelection(0);
        setModelValuesToUI();
    }

    /** The system calls this only when creating the layout in a dialog. */
   /* @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);


        return dialog;
    }*/

    private void setModelValuesToUI(){
        Log.d(TAG, mModel.getHumanReadableTime());
        time.setText(mModel.getHumanReadableTime());
        distance.setText(String.format(Locale.US, "%.2f",mModel.getDistance()));
        speed.setText(String.format(Locale.US, "%.2f",mModel.getSpeed()));
        title.setText(String.format(Locale.US, "%s for %s",
                typesSelection.getSelectedItem().toString(),
                time.getText()));
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        parent.getItemAtPosition(position);
        setModelValuesToUI();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
