package ru.recoilme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;


public abstract class RestActivity extends Activity{

	public ResultReceiver mReceiver;
	protected ProgressDialog mProgress;
	private static final String TAG = RestActivity.class.getName();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
            	if (mProgress!=null) {
            		mProgress.dismiss();
            	}
            	if (resultCode!=200) {
            		Toast.makeText(getApplicationContext(), R.string.error_noconnection, Toast.LENGTH_SHORT).show();
            	}
                if (resultData != null && resultData.containsKey(RestService.REST_RESULT)) {
                	Log.d(TAG,"mode:"+resultData.getString("EXTRA_MODE")+"result:"+resultData.getString("REST_RESULT"));
                    onRESTResult(resultCode, resultData.getString("REST_RESULT"),resultData.getString("EXTRA_MODE"));
                }
                else {
                	Log.d(TAG,"mode:result:null");
                    onRESTResult(resultCode, null,"");
                }
            }
       };
	}
	
	public String restPostRequest(String url,String mode,Bundle params) {
		if(mProgress!=null) { 
			mProgress.dismiss(); 
		}
		mProgress = new ProgressDialog(this);
		mProgress.setCancelable(true);
		mProgress.show();
		mProgress.setContentView(R.layout.emptydialog);
		
        Intent intent = new Intent(this, RestService.class);
        intent.setData(Uri.parse(url));


        intent.putExtra(RestService.EXTRA_HTTP_VERB, 0x2);
        intent.putExtra(RestService.EXTRA_PARAMS,params);
        intent.putExtra(RestService.EXTRA_MODE, mode);
        intent.putExtra(RestService.EXTRA_RESULT_RECEIVER, mReceiver);
        startService(intent);
        return "";
	}

	public String restGetRequest(String url,String mode) {
		if(mProgress!=null) { 
			mProgress.dismiss(); 
		}
		mProgress = new ProgressDialog(this);
		mProgress.setCancelable(true);
		mProgress.show();
		mProgress.setContentView(R.layout.emptydialog);
		
        Intent intent = new Intent(this, RestService.class);
        intent.setData(Uri.parse(url));
        intent.putExtra(RestService.EXTRA_HTTP_VERB, 0x1);
        intent.putExtra(RestService.EXTRA_MODE, mode);
        intent.putExtra(RestService.EXTRA_RESULT_RECEIVER, mReceiver);
        startService(intent);
        return "";
	}
	
	public abstract void onRESTResult(int code, String result, String mode);
	
	@Override 
	protected void onPause() { 
		super.onPause(); 
		if(mProgress!=null) { 
			mProgress.dismiss(); 
		} 
	}
}
