package freelancerwatermellon.serbianeidomnikey;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.smartcardio.ATR;
import android.smartcardio.Card;
import android.smartcardio.CardException;
import android.smartcardio.CardTerminal;
import android.smartcardio.TerminalFactory;
import android.smartcardio.ipc.CardService;
import android.smartcardio.ipc.ICardService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Constants;
import freelancerwatermellon.serbianeidomnikey.cardreadermanager.Util;
import freelancerwatermellon.serbianeidomnikey.dialogs.EnterKeyDialog;
import freelancerwatermellon.serbianeidomnikey.dialogs.IncludeImageDialog;
import freelancerwatermellon.serbianeidomnikey.dialogs.ProgressDialogCustom;
import freelancerwatermellon.serbianeidomnikey.eid.EidCard;
import freelancerwatermellon.serbianeidomnikey.eid.EidCardApollo;
import freelancerwatermellon.serbianeidomnikey.eid.EidCardGemalto;
import freelancerwatermellon.serbianeidomnikey.eid.EidInfo;
import freelancerwatermellon.serbianeidomnikey.utils.Random;

public class MainActivity extends Activity {
    private static final String TAG = "SrpskaLK";

    // State definitions
    private static final int STATE_NO_READER = 1000; // No reader connected
    private static final int STATE_READER = 1001; // Reader is  connected
    private static final int STATE_NO_CARD = 1002; // No card in the reader
    private static final int STATE_CARD = 1003; // Card inserted in the reader
    private static final int STATE_CARD_UNKNOWN = 1004; // Card inserted in the reader

    private static final String EVENT_START_TASKS = "start_tasks"; // Card inserted in the reader

    private static final String EVENT_READER_DISCONNECTED = "reader_disconnected"; // Card inserted in the reader
    private static final String EVENT_READER_CONNECTED = "reader_connected"; // Card inserted in the reader
    private static final String EVENT_CARD_DISCONNECTED = "card_disconnected"; // Card inserted in the reader
    private static final String EVENT_CARD_CONNECTED = "card_connected"; // Card inserted in the reader
    private static final String EVENT_CARD_CONNECTED_UNKNOWN = "card_connected_unknown"; // Card inserted in the reader

    // Async Tasks
    PollReaderPresent pollReaderPresent = null;
    PollCardPresent pollCardPresent = null;
    GetCardATR getCardATR = null;

    TextView mPrezime;
    TextView mIme;
    TextView mImeRoditelja;
    TextView mDatumRodjenja;
    TextView mMestoRodjenja;
    TextView mAdresa;
    TextView mJMBG;
    TextView mPOL;
    TextView mIzdaje;
    TextView mBrojDok;
    TextView mDatumIzd;
    TextView mVaziDo;
    ImageView mImgView;
    Button mPDFButton;
    Bundle mData_bundle = null;
    ScrollView mScrView;
    private int state;  // state variable
    private ICardService mService = null;
    private TerminalFactory mFactory = null;
    private CardTerminal mReader = null;
    private Card mCard = null;

    private ServiceConnection mBackendServiceConnection;
    private boolean mServiceBound = false;

    // Local broadcast receiver
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private Context mContext = null;
    private TextView tv_reader;
    private TextView tv_card;

    private IncludeImageDialog includeImgDialog;
    private EnterKeyDialog enterKeyDialog = null;

    private TextView mATR = null;
    private ReadAllTask mReadAllTask = null;
    private EidInfo evI;

    private ProgressDialogCustom progressDialog;
    private boolean includе_photo = false;
    private boolean backend_service_running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        tv_reader = (TextView) findViewById(R.id.tv_reader_indicator);
        tv_card = (TextView) findViewById(R.id.tv_card_indicator);

        tv_reader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Constants.BackendServiceAction);
                MainActivity.this.startService(intent);
                Log.e("BACKEND SERVICE STARTER", "STARTOVO SERVIS");
                Util.logInfo("Started service com.theobroma.cardreadermanager.backendservice from BroadcastReceiver");
            }
        });

        mImgView = (ImageView) findViewById(R.id.imageView1);
        mImgView.setEnabled(false);
        mPrezime = (TextView) findViewById(R.id.prezime_lk);
        mIme = (TextView) findViewById(R.id.ime_lk);
        mImeRoditelja = (TextView) findViewById(R.id.ime_roditelja_lk);
        mDatumRodjenja = (TextView) findViewById(R.id.datum_rodjenja_lk);
        mMestoRodjenja = (TextView) findViewById(R.id.mesto_rodjenja_lk);
        mAdresa = (TextView) findViewById(R.id.adresa_lk);
        mJMBG = (TextView) findViewById(R.id.jmbg_lk);
        mPOL = (TextView) findViewById(R.id.pol_lk);
        mIzdaje = (TextView) findViewById(R.id.izdao_lk);
        mBrojDok = (TextView) findViewById(R.id.broj_l_karte_lk);
        mDatumIzd = (TextView) findViewById(R.id.datum_izdavanja_lk);
        mVaziDo = (TextView) findViewById(R.id.vazi_do_lk);
        mPDFButton = (Button) findViewById(R.id.btn_save_to_pdf);
        mPDFButton.setVisibility(View.GONE);
        mPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if user is registered
//                Log.e(TAG, getDeviceID(mContext));
                String key = MyApplication.getInstance().getPrefManager().getKey();
                if (!checkKey(key)) {
                    // Ask for key
                    enterKeyDialog = new EnterKeyDialog(mContext);
                    enterKeyDialog.setDialogMessage("Broj: " + getDeviceID(mContext));
                    enterKeyDialog.setPositiveButtonListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String entered_key = enterKeyDialog.getEnteredValue();
                            if (checkKey(entered_key)) {
                                MyApplication.getInstance().getPrefManager().setKey(entered_key);
                                generatePDF();
                            }
                        }
                    });
                    enterKeyDialog.setNegativeButtonListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing...
                        }
                    });

                    enterKeyDialog.create().show();
                } else {
                    // Key is valid
                    generatePDF();
                }
            }
        });

        progressDialog = new ProgressDialogCustom(this);
        progressDialog.setCancelable(false);

        mBackendServiceConnection = new BackendServiceConnection();

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter

                if (intent.getAction().equals(EVENT_START_TASKS)) {
                    pollReaderPresent = new PollReaderPresent();
                    pollReaderPresent.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    pollCardPresent = new PollCardPresent();
                    pollCardPresent.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    Log.e(TAG, "START TASKS");
                } else if (intent.getAction().equals(EVENT_READER_DISCONNECTED)) {
                    state = STATE_NO_READER;
                    // get ready for reader insertion
                    tv_reader.setBackgroundColor(getResources().getColor(R.color.absent));
                    tv_card.setBackgroundColor(getResources().getColor(R.color.absent));
                    tv_reader.setText(R.string.reader_absent);
                    tv_card.setText(R.string.card_absent);
                } else if (intent.getAction().equals(EVENT_READER_CONNECTED)) {
                    state = STATE_READER;
                    // get ready for card insertion
                    tv_reader.setBackgroundColor(getResources().getColor(R.color.present));
                    tv_reader.setText(R.string.reader_present);
                } else if (intent.getAction().equals(EVENT_CARD_DISCONNECTED)) {
                    if (state == STATE_READER || state == STATE_CARD || state == STATE_CARD_UNKNOWN) {
                        state = STATE_NO_CARD;
                        tv_card.setBackgroundColor(getResources().getColor(R.color.absent));
                        tv_card.setText(R.string.card_absent);
                        // manage card variables properly
                    }
                } else if (intent.getAction().equals(EVENT_CARD_CONNECTED_UNKNOWN)) {
                    if (state == STATE_READER || state == STATE_NO_CARD) {
                        state = STATE_CARD_UNKNOWN;
                        tv_card.setBackgroundColor(getResources().getColor(R.color.unknown));
                        tv_card.setText(R.string.card_unknown);
                        // The card is inserted. Check Card ATR
                        getCardATR = new GetCardATR();
                        getCardATR.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else if (intent.getAction().equals(EVENT_CARD_CONNECTED)) {
                    if (state == STATE_CARD_UNKNOWN) {
                        state = STATE_CARD;
                        tv_card.setBackgroundColor(getResources().getColor(R.color.present));
                        tv_card.setText(R.string.card_present);
                        // The card is inserted so we are ready to read the data from it
                        // Ask user for start reading...
                        includeImgDialog.create().show();
                    }
                }
            }
        };


        state = STATE_NO_READER;
        includeImgDialog = new IncludeImageDialog(this);
        includeImgDialog.setPositiveButtonListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                includе_photo = true;
                mImgView.setVisibility(View.VISIBLE);
                mPDFButton.setVisibility(View.GONE);
                progressDialog.showDialog("Čitanje podataka...");
                mReadAllTask = new ReadAllTask();
                mReadAllTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        includeImgDialog.setNegativeButtonListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                includе_photo = false;
                mImgView.setVisibility(View.GONE);
                mPDFButton.setVisibility(View.GONE);
                progressDialog.showDialog("Čitanje podataka...");
                mReadAllTask = new ReadAllTask();
                mReadAllTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        // Create service and try to bind to it
        Util.logDebug("GOING TO BIND SERVICE");
        bindBackendService();
    }

    public void bindBackendService() {
        Intent intent = new Intent();
        intent.setAction(Constants.BackendServiceAction);
        intent.setPackage(getApplicationContext().getPackageName());
        this.mContext.startService(intent);
        this.mContext.bindService(intent, this.mBackendServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindBackendService() {
        try {
            this.mContext.unbindService(this.mBackendServiceConnection);
        } catch (IllegalArgumentException e) {
        }

    }

    public boolean serviceBound() {
        return this.mServiceBound;
    }

    private void generatePDF() {
        String file_name = Environment.getExternalStorageDirectory()
                + File.separator + evI.getSurname()
                + evI.getPersonalNumber() + ".pdf";
        new PdfGenerator(mData_bundle, getApplicationContext())
                .generatePDF(file_name);
        Toast.makeText(getApplicationContext(), "Сачуван ПДФ", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(EVENT_START_TASKS));

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(EVENT_READER_DISCONNECTED));

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(EVENT_READER_CONNECTED));

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(EVENT_CARD_CONNECTED));

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(EVENT_CARD_DISCONNECTED));

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(EVENT_CARD_CONNECTED_UNKNOWN));

//        if (pollReaderPresent == null && backend_service_running) {
//            // start tasks
//            Intent startTasks = new Intent(EVENT_START_TASKS);
//            LocalBroadcastManager.getInstance(mContext).sendBroadcast(startTasks);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        if (mService != null) {
            mService.releaseService();
        }
        if (pollReaderPresent != null)
            pollReaderPresent.cancel(true);

        if (pollCardPresent != null)
            pollCardPresent.cancel(true);

        unbindBackendService();
    }

    private CardTerminal getFirstReader() {
        if (mFactory == null) {
            try {
                mFactory = mService.getTerminalFactory();
            } catch (Exception e) {
                Log.e(TAG, "unable to get terminal factory");
                //showToast("Error: unable to get terminal factory");
                return null;
            }
        }

        CardTerminal firstReader = null;
        try {
            /* Get the available card readers as list. */
            List<CardTerminal> readerList = mFactory.terminals().list();
            if (readerList.size() == 0) {
                return null;
            }

			/* Establish a connection with the first reader from the list. */
            firstReader = readerList.get(0);
        } catch (CardException e) {
            Log.e(TAG, e.toString());
            showToast("Error: " + e.toString());
        }
        return firstReader;
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message,
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        //Log.e(TAG, "T O A S T");
    }

    private String byteArrayToString(byte[] array) {
        String hex = "";
        for (int i = 0; i < array.length; i++) {
            hex += "0x" + Integer.toHexString(array[i] & 0x000000ff) + " ";
        }
        return hex;
    }

    public String getDeviceID(Context context) {
//        TelephonyManager mngr = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
//        String imei = mngr.getDeviceId();
//        if (imei != null) imei  = "";

        String imei = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (imei.length() >= 16)
            imei = imei.substring(0, 15); // ignore last hex digit
        return imei;
    }

    private Boolean checkKey(String key) {
        String imei = getDeviceID(mContext);
        Boolean key_is_ok = false;
        Long key_long = 0L;

        Long imei_long = Long.parseLong(imei, 16);
        if (key != "")
            key_long = Long.parseLong(key, 16);

        Random ran = new Random(imei_long);
        Long xoro = ran.nextLong();
        xoro = xoro >>> 4;

        // Log.e("KEY", key_long.toString());
        // Log.e("IMEI", imei_long.toString());
        // Log.e("XORO", xoro.toString());

        // Log.e("KEY_EXPECTED", ((Long)(imei_long^xoro)).toString());

        if ((xoro ^ imei_long ^ key_long) == 0) {
            key_is_ok = true;
        } else
            key_is_ok = false;

        return key_is_ok;
    }

    class BackendServiceConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName className, IBinder service) {

            MainActivity.this.mServiceBound = true;
            Util.logDebug("bound to backend service");
            mService = CardService.getInstance(MainActivity.this);
            // start tasks
            if (pollReaderPresent == null) {
                Intent startTasks = new Intent(EVENT_START_TASKS);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(startTasks);
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            MainActivity.this.mServiceBound = false;
            Util.logDebug("unbound from backend service");
        }
    }


    private class PollReaderPresent extends AsyncTask<Void, Boolean, Void> {

        private Boolean isReaderPresent = false;

        @Override
        public Void doInBackground(Void... params) {

            // Create event to inform activity that reader is absent
            publishProgress(isReaderPresent);

            while (true) {
                mReader = getFirstReader();
                if (mReader == null) {
                    if (isReaderPresent) {
                        publishProgress(false); // reader got disconnected
                    }
                    isReaderPresent = false;
                    Log.e(TAG, "Reader null");
                } else {
                    if (!isReaderPresent) {
                        publishProgress(true); // reader connected
                    }
                    isReaderPresent = true;
                    //Log.e(TAG, "Reader not null");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //isReaderPresent = false;
                    return null;
                }
            }

        }

        @Override
        public void onProgressUpdate(Boolean... params) {
            Boolean reader_present = params[0];
            if (reader_present) {
                Intent readerPresent = new Intent(EVENT_READER_CONNECTED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(readerPresent);
                // Log.e(TAG, "PRESENT");
            } else {
                Intent readerAbsent = new Intent(EVENT_READER_DISCONNECTED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(readerAbsent);
                // Log.e(TAG, "ABSENT");
            }
        }
    }

    private class PollCardPresent extends AsyncTask<Void, Boolean, Void> {

        private Boolean isCardPresent = false;

        @Override
        public Void doInBackground(Void... params) {

            // Create event to inform activity that reader is absent
            publishProgress(isCardPresent);

            while (true) {

                if (mReader == null) {
                    // reader disconnected so mark card absent
                    if (isCardPresent) {
                        publishProgress(false);
                    }
                    isCardPresent = false;
                } else {
                    try {
                        if (mReader.isCardPresent()) {
                            if (!isCardPresent) {
                                publishProgress(true);
                            }
                            isCardPresent = true;
                        } else {
                            if (isCardPresent) {
                                publishProgress(false);
                            }
                            isCardPresent = false;
                        }
                    } catch (CardException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //isReaderPresent = false;
                    return null;
                }
            }
        }

        @Override
        public void onProgressUpdate(Boolean... params) {
            Boolean card_present = params[0];
            if (card_present) {
                Intent cardPresent = new Intent(EVENT_CARD_CONNECTED_UNKNOWN);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(cardPresent);
                Log.e(TAG, "CARD UNKNOWN");
            } else {
                Intent cardAbsent = new Intent(EVENT_CARD_DISCONNECTED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(cardAbsent);
                Log.e(TAG, "CARD ABSENT");
            }
        }
    }

    private class GetCardATR extends AsyncTask<Void, Boolean, Boolean> {
        @Override
        public Boolean doInBackground(Void... params) {
            try {
                Log.e(TAG, "Trying to get ATR...");
                if (mReader.isCardPresent()) {
                        /* Connect to the reader. This returns a card object.
                         * "*" indicates that either protocol T=0 or T=1 can be
						 * used. */
                    Log.e(TAG, "Card is present...");
                    if (mCard != null) {
                        mCard.disconnect(true);
                    }

                    mCard = mReader.connect("*");

                    Log.e(TAG, "Connected...");
                    ATR atr = mCard.getATR();

                    Log.e(TAG, byteArrayToString(atr.getBytes()));
                    Log.e(TAG, byteArrayToString(EidCardApollo.CARD_ATR));
                    Log.e(TAG, byteArrayToString(EidCardGemalto.CARD_ATR));


                    if (Arrays.equals(atr.getBytes(), EidCardGemalto.CARD_ATR) || Arrays.equals(atr.getBytes(), EidCardApollo.CARD_ATR)) {
                        return true;
                    } else {
                        return false;
                    }

                } else {
                    return false;
                }

            } catch (CardException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                // Known Serbian Eid ATR
                Intent cardPresent = new Intent(EVENT_CARD_CONNECTED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(cardPresent);
                // Log.e(TAG, "CARD CONNECTED");
            } else {

            }
        }

        @Override
        public void onCancelled(Boolean unused) {
            try {
                mCard.disconnect(true);
            } catch (CardException e) {
                e.printStackTrace();
            }

        }
    }

    private class ReadAllTask extends
            AsyncTask<Void, Void, ElementaryFileContent> {

        @Override
        protected ElementaryFileContent doInBackground(Void... params) {

            ElementaryFileContent result = new ElementaryFileContent();
            result.e = null;
            result.evI = null;
            result.eidPhoto = null;

            try {
                EidCard card = EidCard.fromCard(mCard);

                EidInfo evcrInf = card.readEidInfo();
                if (includе_photo)
                    result.eidPhoto = card.readEidPhoto();

                result.evI = evcrInf;

            } catch (Exception e) {

                result.e = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ElementaryFileContent result) {

            progressDialog.hideDialog();
            if (result.e != null) {

            } else {
                evI = result.evI;

                // Intent intent = new Intent(getApplicationContext(),
                // SlideActivity.class);

                mData_bundle = evI.toBundle();

                mData_bundle.putString("place_full",
                        evI.getPlaceFull("ulaz %s", "%s. sprat", "br. %s"));
                mData_bundle.putString("place_full_pdf",
                        evI.getPlaceFull_pdf("ulaz %s", "%s. sprat", "br. %s"));
                mData_bundle.putString("address_date", evI.getAddressDate());
                mData_bundle.putString("place_of_birth_full", evI.getPlaceOfBirthFull());
                mData_bundle.putString("place_of_birth_full_pdf", evI.getPlaceOfBirthFull_pdf());
                mData_bundle.putString("name_full", evI.getNameFull());


                // put photo

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (result.eidPhoto != null)
                    result.eidPhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);

                mData_bundle.putByteArray("eid_photo", stream.toByteArray());

                mPrezime.setText(evI.getSurname());
                mIme.setText(evI.getGivenName());
                mImeRoditelja.setText(evI.getParentGivenName());
                mDatumRodjenja.setText(evI.getDateOfBirth());
                mMestoRodjenja.setText(evI.getPlaceOfBirthFull());
                mAdresa.setText(evI.getPlace());
                mAdresa.setText(evI.getPlaceFull("ulaz %s", "%s. sprat",
                        "br. %s"));
                mJMBG.setText(evI.getPersonalNumber());
                mPOL.setText(evI.getSex());
                mIzdaje.setText(evI.getIssuingAuthority());
                mBrojDok.setText(evI.getDocRegNo());
                mDatumIzd.setText(evI.getIssuingDate());
                mVaziDo.setText(evI.getExpiryDate());

                if (result.eidPhoto != null)
                    mImgView.setImageBitmap(result.eidPhoto);
                mImgView.setEnabled(true);

                mPDFButton.setVisibility(View.VISIBLE);

            }
        }
    }

    private class ElementaryFileContent {
        public Exception e;
        EidInfo evI;
        Bitmap eidPhoto;
    }

}
