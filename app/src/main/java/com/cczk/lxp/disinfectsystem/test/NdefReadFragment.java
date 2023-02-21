package com.cczk.lxp.disinfectsystem.test;

import android.app.Fragment;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.cczk.lxp.disinfectsystem.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NdefReadFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private String mTitle;

    private ListView lvRecord;

    private List<Map<String, Object>> listRecord = new ArrayList<Map<String, Object>>();
    private SimpleAdapter simpleAdapter;

    public NdefReadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title fragment title.
     * @return A new instance of fragment NdefReadFragment.
     */
    public static NdefReadFragment newInstance(String title) {
        NdefReadFragment fragment = new NdefReadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frgm_ndef_read, container, false);
    }

    @Override
    public void onViewCreated(View view,Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvRecord = view.findViewById(R.id.lvRecord);
                simpleAdapter = new SimpleAdapter(getContext(), listRecord, R.layout.layout_item,
                new String[]{"Record", "Content"}, new int[]{R.id.tvRecord, R.id.tvContent});
        lvRecord.setAdapter(simpleAdapter);

        view.findViewById(R.id.btnRead).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listRecord.clear();
                try {
                    //寻卡
                    byte[] snr = new byte[12];
                    short st = NFC2TestActivity.card.rf_scard((byte) 1, snr);
                    if (st < 0) {
                        ShowToast(NFC2TestActivity.card.GetErrMessage((short) 0, st));
                        return;
                    }
                    //检测是否为NDEF格式
                    st = NFC2TestActivity.card.rf_Tag_CheckNdef();
                    if (st < 0) {
                        ShowToast(NFC2TestActivity.card.GetErrMessage((short) 0, st));
                        return;
                    } else if (st == 0) { //标签为默认初始化状态，即不是NDEF格式
                        ShowToast("Tag is default initial state.");
                        return;
                    }

                    //读取数据
                    byte[] data = new byte[256];
                    st = NFC2TestActivity.card.rf_Tag_ReadNdef(data);
                    if (st < 0) {
                        ShowToast(NFC2TestActivity.card.GetErrMessage((short) 0, st));
                        return;
                    }

                    byte[] ndefData = new byte[st];
                    System.arraycopy(data, 0, ndefData, 0, st);

                    //解析数据
                    NdefMessage ndefMessage = new NdefMessage(ndefData);
                    ParseNdefMessage(ndefMessage);
                } catch (Exception e) {
                    listRecord.clear();
                    ShowToast(e.getMessage());
                } finally {
                    simpleAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void ShowToast(String str) {
        Toast toast = Toast.makeText(getActivity(), str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void ParseNdefMessage(NdefMessage ndefMessage) throws Exception {
        try {
            int number = 0;
            NdefRecord[] ndefRecords = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : ndefRecords) {
                StringBuffer sRecord = new StringBuffer();
                StringBuffer sContent = new StringBuffer();
                Map<String, Object> item = new HashMap<String, Object>();
                number++;

                byte[] payload = ndefRecord.getPayload();
                String type = new String(ndefRecord.getType());

                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    //TextRecord
                    String textCoding = ((payload[0] & 0x80) == 0) ? "utf-8" : "utf-16";
                    int codeLength = payload[0] & 0x3f;
                    String text = new String(payload, codeLength + 1, payload.length - codeLength - 1, textCoding);

                    sRecord.append("# Record " + number + ": Text record");
                    sContent.append("Type: " + type + "\n");
                    sContent.append("Encoding: " + textCoding + "\n");
                    sContent.append("Text: " + text);
                } else if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_URI)) {
                    //URI Record / Tel Record
                    String uri = Abbreviations[payload[0]] + new String(payload, 1, payload.length - 1, "utf-8");
                    if (uri.startsWith("tel:")) {
                        sRecord.append("# Record " + number + ": Tel record");
                        sContent.append("Type: " + type + "\n");
                        sContent.append("Tel: " + uri.substring(4));
                    } else {
                        sRecord.append("# Record " + number + ": Uri record");
                        sContent.append("Type: " + type + "\n");
                        sContent.append("Uri: " + uri);
                    }
                } else if (ndefRecord.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE && Arrays.equals(ndefRecord.getType(), "android.com:pkg".getBytes())) {
                    //Android App Record
                    String packageName = new String(payload, 0, payload.length, "utf-8");

                    sRecord.append("# Record " + number + ": Android App record");
                    sContent.append("Type: " + type + "\n");
                    sContent.append("Package Name: " + packageName);
                } else {
                    throw new Exception("NDEF record not parsed by this demo app");
                }

                item.put("Record", sRecord.toString());
                item.put("Content", sContent.toString());

                listRecord.add(item);
            }
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * URI abbreviations, as defined in NDEF URI record specifications.
     */
    public static String[] Abbreviations = new String[]{
            new String(),
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://",
            "sftp://",
            "smb://",
            "nfs://",
            "ftp://",
            "dav://",
            "news:",
            "telnet://",
            "imap:",
            "rtsp://",
            "urn:",
            "pop:",
            "sip:",
            "sips:",
            "tftp:",
            "btspp://",
            "btl2cap://",
            "btgoep://",
            "tcpobex://",
            "irdaobex://",
            "file://",
            "urn:epc:id:",
            "urn:epc:tag:",
            "urn:epc:pat:",
            "urn:epc:raw:",
            "urn:epc:",
            "urn:nfc:"
    };
}