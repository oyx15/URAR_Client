package edu.ece671.reportclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ReportDetails extends Activity{
	EditText AccidentType;
	RadioGroup InjuredType;
	RadioButton Serious;
	RadioButton NotSerious;
	RadioButton NotInjured;
	RadioGroup Others;
	RadioButton Yes;
	RadioButton No;
	//CheckBox SendPicture;
	Button Submit;
	public String Accident;
	public String Injure;
	public String OthersInjure;
	public String Message;
	public static final String BUNDLE_REPORT = "report message";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report_details);
		AccidentType = (EditText)findViewById(R.id.accident);

		InjuredType = (RadioGroup)findViewById(R.id.injuredType);
		InjuredType.setOnCheckedChangeListener(InjuredTypeClickListener);
		Serious = (RadioButton)findViewById(R.id.serious);
		NotSerious = (RadioButton)findViewById(R.id.notSerious);
		NotInjured = (RadioButton)findViewById(R.id.notInjured);
		
		Others = (RadioGroup)findViewById(R.id.othersInjureType);
		Others.setOnCheckedChangeListener(OthersClickListener);
		Yes = (RadioButton)findViewById(R.id.yes);
		No = (RadioButton)findViewById(R.id.no);
		
		//SendPicture = (CheckBox)findViewById(R.id.sendPicture);
		View reportDetails = (Button)findViewById(R.id.submit);
		reportDetails.setOnClickListener(submitClickListener);
	}
    
	private RadioGroup.OnCheckedChangeListener InjuredTypeClickListener = new RadioGroup.OnCheckedChangeListener(){

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			if(checkedId == Serious.getId()){
				Injure = "2"; //it means injured seriously
			}else if(checkedId == NotSerious.getId()){
				Injure = "1"; //it means injured not very seriously
			}else if(checkedId == NotInjured.getId()){
				Injure = "0"; //it means not injured
			}
		}
		
	};
	
	private RadioGroup.OnCheckedChangeListener OthersClickListener = new RadioGroup.OnCheckedChangeListener(){

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			if(checkedId == Yes.getId()){
				OthersInjure = "1"; //it means there is someone injured
			}else if(checkedId == No.getId()){
				OthersInjure = "0"; //it means there is no one else injured
			}
		}	
	};
    
    private OnClickListener submitClickListener = new OnClickListener(){
    	@Override
    	public void onClick(View v){
    		// TODO Auto-generated method stub
    		Accident = AccidentType.getText().toString();
    		Message = "@" + Accident + "@" + Injure + "@" + OthersInjure;
    		AlertDialog builder = new AlertDialog.Builder(ReportDetails.this).
    				setMessage("Do you want to send this report now?")
    			    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
    			    	public void onClick(DialogInterface dialog, int id) {
    			    		Intent intent = new Intent(getApplicationContext(), ShowLocation.class);
    			    		Bundle myBundle = new Bundle();
    			    		myBundle.putString(BUNDLE_REPORT, Message);
    			    		intent.putExtras(myBundle);
    			    		startActivity(intent); //call the activity
    			    		Toast.makeText(getApplicationContext(), "You submitted a report.", Toast.LENGTH_SHORT).show();
    			    		//finish();
    			    		}
    			    	})
    			    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
    			    	public void onClick(DialogInterface dialog, int id) {
    			    		// User cancelled the dialog
    			    		Toast.makeText(getApplicationContext(), "You cancelled this report.", Toast.LENGTH_SHORT).show();
                            dialog.cancel();
    			   			//finish();
    			    		}
    			   		}).create();
    			builder.show();
    		}
    };
}
