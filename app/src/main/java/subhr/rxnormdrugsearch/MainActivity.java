package subhr.rxnormdrugsearch;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    Button btnGetDrugInfo;
    Button btnRefresh;
    EditText editText;
    TextView responseView;
    String uri;
    ProgressBar progressBar;

    String rxcui;
    String drugname;
    String synonym;
    String tty;
    String lang;
    String suppress;
    String umlscui;

    boolean flag = false;
    private static final String BASE_URI = "https://rxnav.nlm.nih.gov/REST/drugs?name=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(getResources().getString(R.string.main_heading));

        btnGetDrugInfo = (Button) findViewById(R.id.btn_drug_info);
        btnRefresh = (Button) findViewById(R.id.btn_refresh);
        editText = (EditText) findViewById(R.id.editText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        responseView = (TextView) findViewById(R.id.responseView);

        btnGetDrugInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = true;
                getDrugInfo();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = false;
                refresh();
            }
        });
    }

    private void refresh() {
        responseView.setText("");
        editText.setText("");
    }

    private void getDrugInfo() {
        String drugName = editText.getText().toString();
        if(drugName.equals("")) {
            if(!flag) {
                responseView.setText("");
            } else {
                responseView.setText(getResources().getString(R.string.invalid_input));
                responseView.setTextColor(getResources().getColor(R.color.red));
                responseView.setGravity(Gravity.CENTER);
            }

        } else {
            uri = BASE_URI + drugName;
            Log.d(TAG,"uri: " + uri);
            responseView.setGravity(Gravity.LEFT);
            new RetrieveDataTask().execute();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        getDrugInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class RetrieveDataTask extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            responseView.setText("");
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(uri);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if(response == null) {
                responseView.setText(getResources().getString(R.string.response_error));
                responseView.setTextColor(getResources().getColor(R.color.red));
                responseView.setGravity(Gravity.CENTER);
            }

            progressBar.setVisibility(View.GONE);
            parseXMLData(response);
        }
    }

    private void parseXMLData(String response) {
        try {
            StringBuilder out = new StringBuilder();
            InputStream in_s = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(in_s);

            Element element=doc.getDocumentElement();
            element.normalize();

            NodeList nList = doc.getElementsByTagName("conceptProperties");
            if(nList.getLength() == 0) {
                responseView.setText(getResources().getString(R.string.response_empty));
                responseView.setTextColor(getResources().getColor(R.color.red));
                responseView.setGravity(Gravity.CENTER);
            }
            else {
                for (int i=0; i<nList.getLength(); i++) {
                    Node node = nList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element2 = (Element) node;
                        rxcui = "rxcui: " + getValue("rxcui", element2) + "\n";
                        out.append(rxcui);

                        drugname = "drugName: " + getValue("name", element2) + "\n";
                        out.append(drugname);

                        synonym = "synonym: " + getValue("synonym", element2) + "\n";
                        out.append(synonym);

                        tty = "tty: " + getValue("tty", element2) + "\n";
                        out.append(tty);

                        lang = "language: " + getValue("language", element2) + "\n";
                        out.append(lang);

                        suppress = "suppress: " + getValue("suppress", element2) + "\n";
                        out.append(suppress);

                        umlscui = "umlscui: " + getValue("umlscui", element2) + "\n";
                        out.append(umlscui);

                        out.append("\n");
                        out.append("\n");
                    }
                }//end of for loop

                responseView.setText(out.toString());
                responseView.setEnabled(false);
                responseView.setTextColor(getResources().getColor(R.color.colorPrimary));
                responseView.setGravity(Gravity.LEFT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getValue(String tag, Element element) {
        String result = "";
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        if(node!=null) {
            result = node.getNodeValue();
        }

        return result;
    }
}

