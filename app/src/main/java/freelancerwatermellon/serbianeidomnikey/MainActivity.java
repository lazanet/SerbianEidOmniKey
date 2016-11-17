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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
    private static final String EVENT_READING_FAILED = "reading_failure"; // Reading initiated but failed
    private static final String EVENT_READING_SUCCESS = "reading_success"; // Reading initiated and completed successfully

    // Async Tasks
    PollReaderPresent pollReaderPresent = null;
    PollCardPresent pollCardPresent = null;
    GetCardATR getCardATR = null;
    ActionMachine actionMachine;
    Queue<String> action_fifo = new LinkedList<String>();
    // UI VARIABLES
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
    private ReadAllTask readAllTask = null;
    private ServiceConnection mBackendServiceConnection;
    private ICardService mService = null;
    private TerminalFactory mFactory = null;
    private CardTerminal mReader = null;
    private Card mCard = null;
    private int state;  // state variable
    private boolean includе_photo;
    private boolean mServiceBound = false;
    // Local broadcast receiver
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private Context mContext = null;
    private TextView tv_reader;
    private TextView tv_card;

    private IncludeImageDialog includeImgDialog;
    private EnterKeyDialog enterKeyDialog = null;

    private TextView mATR = null;

    private EidInfo evI;

    private ProgressDialogCustom progressDialog;
    private boolean backend_service_running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        tv_reader = (TextView) findViewById(R.id.tv_reader_indicator);
        tv_card = (TextView) findViewById(R.id.tv_card_indicator);
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
        // create omnikey reader management service
        bindBackendService();
        state=STATE_NO_READER;
        // Launch Action Machine
        launchActionMachine();

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

//        // Create service and try to bind to it
//        Util.logDebug("GOING TO BIND SERVICE");
//        bindBackendService();
    }

    private void launchActionMachine() {
        // Start ActionMachine
        Util.logError("Starting Action Machine...");
        state = STATE_NO_READER;
        mReader = null;
        mFactory = null;
        actionMachine = new ActionMachine();
        actionMachine.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        sendAction(EVENT_START_TASKS);
    }

    private void startPollingReader() {
        pollReaderPresent = new PollReaderPresent();
        pollReaderPresent.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopPollingReader() {
        if (pollReaderPresent != null)
            pollReaderPresent.cancel(true);
    }

    private void startPollingCard() {
        pollCardPresent = new PollCardPresent();
        pollCardPresent.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopPollingCard() {
        if (pollCardPresent != null)
            pollCardPresent.cancel(true);
    }

    private void fireReadAllTask() {
        readAllTask = new ReadAllTask();
        readAllTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fireGetATRTask() {
        getCardATR = new GetCardATR();
        getCardATR.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private synchronized void sendAction(String action) {
        action_fifo.add(action);
    }

    private synchronized String getAction() {
        return action_fifo.remove();
    }

    private synchronized boolean hasAction() {
        return !action_fifo.isEmpty();
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

    public boolean isServiceBound() {
        return mServiceBound;
    }

    public void setServiceBound(boolean mServiceBound) {
        this.mServiceBound = mServiceBound;
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
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.releaseService();
        }

        Util.logDebug("OnDestroy");
        stopPollingCard();
        stopPollingReader();

        unbindBackendService();
    }

    private synchronized CardTerminal getFirstReader() {
        if (mFactory == null) {
            try {
                mFactory = mService.getTerminalFactory();
            } catch (Exception e) {

                Util.logError("unable to get terminal factory");
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

            Util.logError(e.toString());
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

    private class ActionMachine extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String action;
            while (true) {
                if (hasAction()) {
                    action = getAction();
                    Util.logDebug("Processing action..." + action);
                    if (EVENT_START_TASKS.equals(action)) {
                        // Start polling for reader
                        startPollingReader();
                        // Start polling for card
                        startPollingCard();
                    } else if (EVENT_READER_DISCONNECTED.equals(action)) {
                        state = STATE_NO_READER;
                    } else if (EVENT_READER_CONNECTED.equals(action)) {
                        state = STATE_READER;
                    } else if (EVENT_CARD_DISCONNECTED.equals(action)) {
                        if (state == STATE_READER || state == STATE_CARD || state == STATE_CARD_UNKNOWN) {
                            state = STATE_NO_CARD;
                            // No card
                        }
                    } else if (EVENT_CARD_CONNECTED_UNKNOWN.equals(action)) {
                        if (state == STATE_READER || state == STATE_NO_CARD) {
                            state = STATE_CARD_UNKNOWN;
                            // Card present, but unknown
                            // The card is inserted. Check Card ATR
                            fireGetATRTask();
                        }
                    } else if (EVENT_CARD_CONNECTED.equals(action)) {
                        if (state == STATE_CARD_UNKNOWN) {
                            state = STATE_CARD;
                            // Card present
                            // start reading
                            fireReadAllTask();
                        }
                    } else if (EVENT_READING_FAILED.equals(action)) {
                        stopPollingCard();
                        stopPollingReader();
                        actionFail();
                        Util.logDebug("Disconnecting card...");
                        if (mCard != null) {
                            try {
                                mCard.disconnect(true);
                            } catch (CardException e) {
                                e.printStackTrace();
                            }
                        }
                        cancel(true);
                    } else if (EVENT_READING_SUCCESS.equals(action)) {
                        stopPollingCard();
                        stopPollingReader();
                        if (mCard != null) {
                            try {
                                mCard.disconnect(true);
                            } catch (CardException e) {
                                e.printStackTrace();
                            }
                        }
                        actionSuccess();
                        cancel(true);
                    }
                }
                try {
                    if (isCancelled()) {
                        throw new InterruptedException();
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }

    class BackendServiceConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName className, IBinder service) {
            setServiceBound(true);
            Util.logDebug("bound to backend service");
            mService = CardService.getInstance(MainActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            setServiceBound(false);
            Util.logDebug("unbound from backend service");
        }
    }


    private class PollReaderPresent extends AsyncTask<Void, Void, Void> {
        private Boolean isReaderPresent = false;
        private int absent_count = 0;

        @Override
        protected Void doInBackground(Void... params) {
            sendAction(EVENT_READER_DISCONNECTED);
            while (true) {

                mReader = getFirstReader();
                if (mReader == null) {
                    if (isReaderPresent) {
                        sendAction(EVENT_READER_DISCONNECTED); // reader got disconnected
                    }
                    isReaderPresent = false;

                    Util.logError("Reader null");
                } else {
                    if (!isReaderPresent) {
                        sendAction(EVENT_READER_CONNECTED); // reader connected
                    }
                    isReaderPresent = true;
                    //Util.logError( "Reader not null");
                }
                if (!isReaderPresent) absent_count++;
                if (absent_count >= READER_ABSENT_MAX_SECS) {
                    sendAction(EVENT_READING_FAILED);
                }
                try {
                    if (isCancelled()) {
                        throw new InterruptedException();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                    Util.logError(e.toString());
                    return null;
                }
            }
        }
    }

    private class PollCardPresent extends AsyncTask<Void, Void, Void> {
        private Boolean isCardPresent = false;
        private int absent_count = 0;

        @Override
        protected Void doInBackground(Void... params) {
            // Create event to inform activity that reader is absent
            sendAction(EVENT_CARD_DISCONNECTED);

            while (true) {

                if (mReader == null) {
                    // reader disconnected so mark card absent
                    if (isCardPresent) {
                        sendAction(EVENT_CARD_DISCONNECTED);
                    }
                    isCardPresent = false;
                } else {
                    try {
                        if (mReader.isCardPresent()) {
                            if (!isCardPresent) {
                                sendAction(EVENT_CARD_CONNECTED_UNKNOWN);
                            }
                            isCardPresent = true;
                        } else {
                            if (isCardPresent) {
                                sendAction(EVENT_CARD_DISCONNECTED);
                            }
                            isCardPresent = false;
                        }
                    } catch (CardException e) {
                        e.printStackTrace();
                        sendAction(EVENT_READING_FAILED);
                    }
                }
                if (!isCardPresent) absent_count++;
                if (absent_count >= CARD_ABSENT_MAX_SECS) {
                    sendAction(EVENT_READING_FAILED);
                }
                try {
                    if (isCancelled()) {
                        throw new InterruptedException();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //isReaderPresent = false;

                    Util.logError(e.toString());
                    return null;
                }
            }
        }
    }

    private class GetCardATR extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {

                Util.logDebug("Trying to get ATR...");
                if (mReader.isCardPresent()) {
                        /* Connect to the reader. This returns a card object.
                         * "*" indicates that either protocol T=0 or T=1 can be
						 * used. */

                    Util.logDebug("Card is present...");
                    if (mCard != null) {
                        mCard.disconnect(true);
                    }

                    mCard = mReader.connect("*");

                    Util.logDebug("Connected...");
                    ATR atr = mCard.getATR();
                    {
                        Util.logDebug(byteArrayToString(atr.getBytes()));
                        Util.logDebug(byteArrayToString(EidCardApollo.CARD_ATR));
                        Util.logDebug(byteArrayToString(EidCardGemalto.CARD_ATR));
                    }
                    if (Arrays.equals(atr.getBytes(), EidCardGemalto.CARD_ATR) || Arrays.equals(atr.getBytes(), EidCardApollo.CARD_ATR)) {
                        sendAction(EVENT_CARD_CONNECTED);
                    }
                }
            } catch (CardException e) {

                Util.logError(e.toString());
                sendAction(EVENT_READING_FAILED);
            }
            return null;
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
                sendAction(EVENT_READING_FAILED);
            } else {
                sendAction(EVENT_READING_SUCCESS);
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
                else {
                    Util.logDebug("Eid Photo is NULL");
                }
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
