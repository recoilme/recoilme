package ru.recoilme;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


public class RestService extends IntentService {
    private static final String TAG = RestService.class.getName();
    
    public static final int GET    = 0x1;
    public static final int POST   = 0x2;
    public static final int PUT    = 0x3;
    public static final int DELETE = 0x4;
    public static final int UPLOAD = 0x5;
    public static final int CUSTOM = 0x6;

    public static final int SOCKET_TIMEOUT = 0x30;
    
    public static final String EXTRA_HTTP_VERB       = "EXTRA_HTTP_VERB";
    public static final String EXTRA_PARAMS          = "EXTRA_PARAMS";
    public static final String EXTRA_RESULT_RECEIVER = "EXTRA_RESULT_RECEIVER";
    public static final String EXTRA_MODE            = "EXTRA_MODE";
    public static final String REST_RESULT 			 = "REST_RESULT";

    public RestService() {
        super(TAG);

    }

    public void setMyHandler(MyHandler handler) {
        this.myHandler = handler;
    }

    public interface MyHandler {
         void customRequest(Uri action, Bundle extras, String mode, ResultReceiver receiver);
    }

    private MyHandler myHandler = null;
    
    @Override
    protected void onHandleIntent(Intent intent) {
        // When an intent is received by this Service, this method
        // is called on a new thread.
        
        Uri    action = intent.getData();
        Bundle extras = intent.getExtras();
        
        if (extras == null || action == null || !extras.containsKey(EXTRA_RESULT_RECEIVER)) {
            // Extras contain our ResultReceiver and data is our REST action.  
            // So, without these components we can't do anything useful.
            Log.e(TAG, "You did not pass extras or data with the Intent.");
            
            return;
        }
        
        // We default to GET if no verb was specified.
        int            verb     = extras.getInt(EXTRA_HTTP_VERB, GET);
        Bundle         params   = extras.getParcelable(EXTRA_PARAMS);
        ResultReceiver receiver = extras.getParcelable(EXTRA_RESULT_RECEIVER);
        String 		   mode     = extras.getString(EXTRA_MODE);
        
        try {            
            // Here we define our base request object which we will
            // send to our REST service via HttpClient.
            HttpRequestBase request = null;
            
            // Let's build our request based on the HTTP verb we were
            // given.
            switch (verb) {
                case GET: {
                    request = new HttpGet();
                    attachUriWithQuery(request, action, params);
                }
                break;
                
                case DELETE: {
                    request = new HttpDelete();
                    attachUriWithQuery(request, action, params);
                }
                break;
                
                case POST: {
                    request = new HttpPost();
                    request.setURI(new URI(action.toString()));
                    
                    // Attach form entity if necessary. Note: some REST APIs
                    // require you to POST JSON. This is easy to do, simply use
                    // postRequest.setHeader('Content-Type', 'application/json')
                    // and StringEntity instead. Same thing for the PUT case 
                    // below.
                    HttpPost postRequest = (HttpPost) request;
                    
                    if (params != null) {
                        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(paramsToList(params),"UTF-8");
                        postRequest.setEntity(formEntity);
                    }
                }
                break;
                
                case PUT: {
                    request = new HttpPut();
                    request.setURI(new URI(action.toString()));
                    
                    // Attach form entity if necessary.
                    HttpPut putRequest = (HttpPut) request;
                    
                    if (params != null) {
                        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(paramsToList(params),"UTF-8");
                        putRequest.setEntity(formEntity);
                    }
                }
                break;
                case UPLOAD: {
                    //TODO Upload request
                }
                default: {
                    if (myHandler!=null)
                        myHandler.customRequest(action,params,mode,receiver);
                    //if (MyHandler!=null)
                        //MyHandler.customRequest(action,params,mode,receiver);
                }
                break;
            }

            
            if (request != null && verb != CUSTOM) {
            	HttpClient client;
            	HttpParams httpparams = new BasicHttpParams();
            	HttpConnectionParams.setConnectionTimeout(httpparams, SOCKET_TIMEOUT*1000);
                HttpConnectionParams.setSoTimeout(httpparams, SOCKET_TIMEOUT*1000);
            	HttpClientParams.setRedirecting(httpparams, true);
            	SchemeRegistry schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
                ClientConnectionManager manager = new ThreadSafeClientConnManager(httpparams, schemeRegistry);
                
                client = new DefaultHttpClient(manager,httpparams);

                Log.d(TAG, "Executing request: "+ verbToString(verb) +": "+ action.toString());
                
                // Finally, we send our request using HTTP. This is the synchronous
                // long operation that we need to run on this thread.
                HttpResponse response = client.execute(request);
                
                HttpEntity responseEntity = response.getEntity();
                StatusLine responseStatus = response.getStatusLine();
                int        statusCode     = responseStatus != null ? responseStatus.getStatusCode() : 0;
               
                if (responseEntity != null) {
                	String result = EntityUtils.toString(responseEntity);
                    Bundle resultData = new Bundle();
                    resultData.putString(REST_RESULT, result);
                    resultData.putString(EXTRA_MODE, mode);
                    receiver.send(statusCode, resultData);
                }
                else {
                    receiver.send(statusCode, null);
                }
            }
        }

        catch (URISyntaxException e) {
            Log.e(TAG, "URI syntax was incorrect. "+ verbToString(verb) +": "+ action.toString(), e);
            receiver.send(0, null);
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "A UrlEncodedFormEntity was created with an unsupported encoding.", e);
            receiver.send(0, null);
        }
        catch (ClientProtocolException e) {
            Log.e(TAG, "There was a problem when sending the request.", e);
            receiver.send(0, null);
        }
        catch (IOException e) {
            Log.e(TAG, "There was a problem when sending the request.", e);
            receiver.send(0, null);
        }
        catch (Exception ex)
        {
             Log.e("Debug", "error: " + ex.getMessage(), ex);
             receiver.send(0, null);
        }
    }

    //public abstract  void customRequest(Uri action, Bundle extras, String mode, ResultReceiver receiver);


    private static void attachUriWithQuery(HttpRequestBase request, Uri uri, Bundle params) {
        try {
            if (params == null) {
                // No params were given or they have already been
                // attached to the Uri.
                request.setURI(new URI(uri.toString()));
            }
            else {
                Uri.Builder uriBuilder = uri.buildUpon();
                
                // Loop through our params and append them to the Uri.
                for (BasicNameValuePair param : paramsToList(params)) {
                    uriBuilder.appendQueryParameter(param.getName(), param.getValue());
                }
                
                uri = uriBuilder.build();
                request.setURI(new URI(uri.toString()));
            }
        }
        catch (URISyntaxException e) {
            Log.e(TAG, "URI syntax was incorrect: "+ uri.toString(), e);
        }
    }
    
    private static String verbToString(int verb) {
        switch (verb) {
            case GET:
                return "GET";
                
            case POST:
                return "POST";
                
            case PUT:
                return "PUT";
                
            case DELETE:
                return "DELETE";

            case UPLOAD:
                return "UPLOAD";

            case CUSTOM:
                return "CUSTOM";
        } 
        return "";
    }
    
    public static List<BasicNameValuePair> paramsToList(Bundle params) {
        ArrayList<BasicNameValuePair> formList = new ArrayList<BasicNameValuePair>(params.size());
        SortedMap<String, String> paramsMap = new TreeMap<String, String>();
    	for (String key : params.keySet()) {
    		paramsMap.put(key, params.get(key).toString());
    	}

    	for (String key : paramsMap.keySet()) {
    		formList.add(new BasicNameValuePair(key, params.get(key).toString()));
    	}
        
        return formList;
    }


}
