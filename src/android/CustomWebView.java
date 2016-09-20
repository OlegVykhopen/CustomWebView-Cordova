package cordova-plugin-custom-web-view.CustomWebView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.MailTo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.GeolocationPermissions;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

@SuppressLint("SetJavaScriptEnabled")
public class CustomWebView extends WebView{

    private String TAG = "WizWebView";
    private CallbackContext create_cb;
    private CallbackContext load_cb;
    private Context mContext;
	private ProgressDialog mDialog;
	private CordovaWebView mView;
	public static Uri mCapturedImageURI = null;

	public static ValueCallback<Uri> mUploadMessage;
	private CordovaInterface mCordova;
	private CordovaPlugin mCordovaPlugin;
	public final static int FILECHOOSER_RESULTCODE = 1;

    static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER =
            new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER);

	@SuppressLint("NewApi") public WizWebView(String viewName, JSONObject settings, CordovaInterface cordova, CallbackContext callbackContext, CordovaWebView _webView) {
        // Constructor method
        super(cordova.getActivity());

        mContext = cordova.getActivity();
        mCordova = cordova;
        mCordovaPlugin = new CordovaPlugin();
        mView = _webView;

        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage("Loading...");
        mDialog.show();


        Log.d("WizWebView", "[WizWebView] *************************************");
        Log.d("WizWebView", "[WizWebView] building - new Wizard View");
        Log.d("WizWebView", "[WizWebView] -> " + viewName);
        Log.d("WizWebView", "[WizWebView] *************************************");

        // Hold create callback and execute after page load
        this.create_cb = callbackContext;

        // Set invisible by default, developer MUST call show to see the view
        this.setVisibility(View.VISIBLE);

        //  WizWebView Settings
        WebSettings webSettings = this.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setGeolocationDatabasePath( ((ContextWrapper) cordova).getFilesDir().getPath() );
    	webSettings.setPluginState(PluginState.ON);
    	webSettings.setAllowContentAccess(true);
    	webSettings.setAllowFileAccess(true);
    	webSettings.setDefaultTextEncodingName("utf-8");

        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            Level16Apis.enableUniversalAccess(webSettings);
        }

        this.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            // Only for Kitkat and newer versions
            this.evaluateJavascript("window.name = '" + viewName + "';", null);
        } else {
            this.loadUrl("javascript:window.name = '" + viewName + "';");
        }

        ViewGroup frame = (ViewGroup) ((Activity) cordova).findViewById(android.R.id.content);

        // Creating a new RelativeLayout fill its parent by default
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        // Default full screen
        frame.addView(this, rlp);

        //this.setPadding(999, 0, 0, 0);

        // Set a transparent background
        this.setBackgroundColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 11) this.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        this.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				//Log.e("url", url);
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
		});

        // Override url loading on WebViewClient
        this.setWebViewClient(new WebViewClient () {
            @Override
            public boolean shouldOverrideUrlLoading(WebView wView, String url) {
                Log.d("WizWebView", "[WizWebView] ****** " + url);

                mDialog.show();

                if (url.startsWith("mailto:")) {
					Log.e("shouldOverrideUrlLoading", "mailto: " + url);
					MailTo mt = MailTo.parse(url);
					Intent i = newEmailIntent(mContext, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
					mContext.startActivity(i);
					mView.reload();
				} else if (url.startsWith("tel:")) {
					Log.e("shouldOverrideUrlLoading", "tel: " + url);
					mContext.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
				}

                return false;
            }


            @SuppressLint("NewApi") @Override
            public void onPageFinished(WebView wView, String url) {

            	Log.e("WebView",String.valueOf(create_cb));

                WizViewManagerPlugin.updateViewList();

                mDialog.dismiss();

                if (create_cb != null) {
                    create_cb.success("{\"url\":\""+url+"\",\"title\":\""+wView.getTitle()+"\"}");
                    Log.d(TAG, "View created and loaded");
                    // Callback used, don't call it again.
                    create_cb = null;
                }else{
                	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        // Only for Kitkat and newer versions
                		mView.evaluateJavascript("webViewFinishLoad('" + url + "','" + wView.getTitle() + "');", null);
                    } else {
                    	mView.loadUrl("javascript:webViewFinishLoad('" + url + "','" + wView.getTitle() + "');");
                    }
                }

                if (load_cb != null) {
                    load_cb.success();
                    Log.d(TAG, "View finished load");
                    // Callback used, don't call it again.
                    load_cb = null;
                }
            }

            public void onReceivedError(WebView view, int errorCod, String description, String failingUrl) {
            	mDialog.dismiss();
                Log.e(TAG, "Error: Cannot load " + failingUrl + " \n Reason: " + description);
                if (create_cb != null) {
                    create_cb.error(description);
                    // Callback used, don't call it again.
                    create_cb = null;
                }
            }
        });
        this.setWebChromeClient(new WebChromeClient() {

        	// openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){

            	Log.e("openFileChooser", "1");

                // Update message
                mUploadMessage = uploadMsg;

                try{

                    // Create AndroidExampleFolder at sdcard

                    File imageStorageDir = new File(
                                           Environment.getExternalStoragePublicDirectory(
                                           Environment.DIRECTORY_PICTURES)
                                           , "AndroidExampleFolder");

                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }

                    // Create camera captured image file path and name
                    File file = new File(
                                    imageStorageDir + File.separator + "IMG_"
                                    + String.valueOf(System.currentTimeMillis())
                                    + ".jpg");

                    mCapturedImageURI = Uri.fromFile(file);

                    // Camera capture image intent
                    final Intent captureIntent = new Intent(
                                                  android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");

                    // Create file chooser intent
                    Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                    // Set camera intent to file chooser
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                                           , new Parcelable[] { captureIntent });

                    // On select image call onActivityResult method of activity
                    mCordova.getActivity().startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

                  }
                 catch(Exception e){
                     Toast.makeText(((ContextWrapper) mContext).getBaseContext(), "Exception:"+e,
                                Toast.LENGTH_LONG).show();
                 }

            }



            // openFileChooser for Android < 3.0
            @SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> uploadMsg){
                openFileChooser(uploadMsg, "");
            }

            //openFileChooser for other Android versions
            @SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                       String acceptType,
                                       String capture) {

                openFileChooser(uploadMsg, acceptType);
            }



            // The webPage has 2 filechoosers and will send a
            // console message informing what action to perform,
            // taking a photo or updating the file

            public boolean onConsoleMessage(ConsoleMessage cm) {

                onConsoleMessage(cm.message(), cm.lineNumber(), cm.sourceId());
                return true;
            }

            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                //Log.d("androidruntime", "Show console messages, Used for debugging: " + message);

            }

        	@Override
        	public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
        		super.onGeolocationPermissionsShowPrompt(origin, callback);
        		try{
        			callback.invoke(origin, true, false);
        		}catch (Exception e) {
                    Log.e("GeolocationPermissions failed", "" + e);
                }
        	}

		});


        // Analyse settings object
        if (settings != null) {
            this.setLayout(settings, create_cb);
        } else {
            // Apply Defaults
            this.setLayoutParams(COVER_SCREEN_GRAVITY_CENTER);
        }

        Log.d(TAG, "Create complete");
    } // ************ END CONSTRUCTOR **************

    private void pickFile(){
    	Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
    	chooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
    	chooserIntent.setType("image/*");

    	((Activity)mContext).startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
    }


    public static Intent newEmailIntent(Context context, String address, String subject, String body, String cc) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
		intent.putExtra(Intent.EXTRA_TEXT, body);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_CC, cc);
		intent.setType("message/rfc822");
		return intent;
	}

    public void setLayout(JSONObject settings, CallbackContext callback) {
        Log.d(TAG, "Setting up layout...");

        String url;

        // Set default settings to max screen
        ViewGroup parent = (ViewGroup) this.getParent();

        // Size
        int _parentHeight = parent.getHeight();
        int _parentWidth = parent.getWidth();
        int _height = _parentHeight;
        int _width = _parentWidth;

        // Margins
        int _x = 0;
        int _y = 0;

        if (settings.has("height")) {
            try {
                _height = Math.round(Float.valueOf(settings.getString("height")).floatValue());
            } catch (JSONException e) {
                // ignore
                Log.e(TAG, "Error obtaining 'height' in settings");
            }
        }

        if (settings.has("width")) {
            try {
                _width = Math.round(Float.valueOf(settings.getString("width")).floatValue());
            } catch (JSONException e) {
                // ignore
                Log.e(TAG, "Error obtaining 'width' in settings");
            }
        }

        if (settings.has("x")) {
        	Log.e("X ",String.valueOf(settings));
            try {
                _x = Math.round(Float.valueOf(settings.getString("x")).floatValue());
            } catch (JSONException e) {
                // ignore
                Log.e(TAG, "Error obtaining 'x' in settings");
            }
        }

        if (settings.has("y")) {
            try {
                _y = Math.round(Float.valueOf(settings.getString("y")).floatValue());
            } catch (JSONException e) {
                // ignore
                Log.e(TAG, "Error obtaining 'y' in settings");
            }
        }


        DisplayMetrics localDisplayMetrics = mContext.getResources().getDisplayMetrics();
        int i = localDisplayMetrics.widthPixels;
        int j = localDisplayMetrics.heightPixels;


        FrameLayout.LayoutParams newLayoutParams = new FrameLayout.LayoutParams(-1, -1);
        //newLayoutParams.setMargins(_y, _x, i - (_y + _width), j - (_x + _height));
        newLayoutParams.setMargins(_x, _y, 0, 0);
        //newLayoutParams.setMargins(_left, _top, _right, _bottom);
        newLayoutParams.height = _height;
        newLayoutParams.width = _width;

        this.setLayoutParams(newLayoutParams);

        Log.e(TAG, "new layout -> width: " + newLayoutParams.width + " - height: " + newLayoutParams.height + " - margins: " + newLayoutParams.leftMargin + "," + newLayoutParams.topMargin + "," + newLayoutParams.rightMargin + "," + newLayoutParams.bottomMargin);

        if (settings.has("src")) {
            try {
                url = settings.getString("src");
                load(url, callback);
            } catch (JSONException e) {
                // default
                // nothing to load
                Log.e(TAG, "Loading source from settings exception : " + e);
            }
        } else {
            Log.d(TAG, "No source to load");
        }
    }

    public void load(String source, CallbackContext callbackContext) {
        // Link up our callback
        load_cb = callbackContext;

        // Check source extension
        try {
            URL url = new URL(source);    // Check for the protocol
            url.toURI();                  // Extra checking required for validation of URI

            // If we did not fall out here then source is a valid URI, check extension
            if (url.getPath().length() > 0) {
                // Not loading a straight domain, check extension of non-domain path
            	this.loadUrl(source);
/*                String ext = MimeTypeMap.getFileExtensionFromUrl(url.getPath());
                Log.d(TAG, "URL ext: " + ext);
                if (validateExtension("." + ext)) {
                    // Load this
                    this.loadUrl(source);
                } else {
                    // Check if file type is in the helperList
                    if (requiresHelper("." + ext)) {
                        // Load this
                        this.loadUrl("http://docs.google.com/gview?embedded=true&url=" + source);
                    } else {
                        // Not valid extension in whitelist and cannot be helped
                        Log.e(TAG, "Not a valid file extension!");
                        if (load_cb != null) {
                            load_cb.error("Not a valid file extension.");
                            load_cb = null;
                        }
                    }
                }*/
                return;

            } else {
                // URL has no path, for example - http://google.com
                Log.d(TAG, "load URL: " + source);
                this.loadUrl(source);
            }

        } catch (MalformedURLException ex1) {
            // Missing protocol, assume local file

            // Check cache for latest file
            File cache = mContext.getApplicationContext().getCacheDir();
            File file = new File(cache.getAbsolutePath() + "/" + source);
            if (file.exists()) {
                // load it
                Log.d(TAG, "load: " + "file:///" + cache.getAbsolutePath() + "/" + source);
                source = ("file:///" + cache.getAbsolutePath() + "/" + source);
            } else {
                // Check file exists in bundle assets
                AssetManager mg = mContext.getResources().getAssets();
                try {
                    mg.open("www/" + source);
                    Log.d(TAG, "load: file:///android_asset/www/" + source);
                    source = "file:///android_asset/www/" + source;
                } catch (IOException ex) {
                    // Not in bundle assets. Try full path
                    file = new File(source);
                    if (file.exists()) {
                        Log.d(TAG, "load: file:///" + source);
                        source = "file:///" + source;
                        file = null;
                    } else {
                        // File cannot be found
                        Log.e(TAG, "File: " + source + " cannot be found!");
                        if (load_cb != null) {
                            load_cb.error("File: " + source + " cannot be found!");
                            load_cb = null;
                        }
                        return;
                    }
                }
            }
            this.loadUrl(source);
        } catch (URISyntaxException ex2) {
            Log.e(TAG, "URISyntaxException loading: file://" + source);
            if (load_cb != null) {
                load_cb.error("URISyntaxException loading: file://" + source);
                load_cb = null;
            }
        }
    }

    private boolean validateExtension(String candidate) {
        for (String s: WizViewManagerPlugin.whitelist) {
            // Check extension exists in whitelist
            if (s.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresHelper(String candidate) {
        for (String s: WizViewManagerPlugin.helperList) {
            // Check extension exists in helperList
            if (s.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }


    @TargetApi(16)
    private static class Level16Apis {
        static void enableUniversalAccess(WebSettings settings) {
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
    }


}

