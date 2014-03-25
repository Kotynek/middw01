package cz.ctu.fit.ddw;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CreoleRegister;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.jsoup.Jsoup;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;

public class GateClient {

    private static SerialAnalyserController annotationPipeline = null;
    private static boolean isGateInitilised = false;

    private String sendHttpRequest(String method, String urlString) {
        try {
            URL url = new URL(URLDecoder.decode(urlString, "UTF-8"));
            HttpURLConnection conection = (HttpURLConnection) url.openConnection();
            conection.setRequestMethod(method);
            conection.setRequestProperty("User-Agent", "Mozilla/5.0");
            StringBuffer response;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    conection.getInputStream()))) {
                String line;
                response = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
            return Jsoup.parse(response.toString()).text();
        } catch (IOException e) {
        }
        return "";
    }

    public ResultList search(String string) {
        String link = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
        String term = string;
        String charset = "UTF-8";
        URL url;
        try {
            url = new URL(link + URLEncoder.encode(term, charset));
            Reader r = new InputStreamReader(url.openStream(), charset);
            ResultList results = new Gson().fromJson(r, ResultList.class);
            return results;
        } catch (IOException e) {
        }
        return null;
    }

    public void run() throws IOException {

        if (!isGateInitilised) {

            // initialise GATE
            initialiseGate();
        }

        try {
            // create an instance of a Document Reset processing resource
            ProcessingResource documentResetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");

            // create an instance of a English Tokeniser processing resource
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");

            // create an instance of a Sentence Splitter processing resource
            ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter");

            ProcessingResource GazetteerPR = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");

            // locate the JAPE grammar file
            File japeOrigFile = new File("C:\\Users\\Kotas\\Desktop\\MI-DDW\\grammar.jape");
            java.net.URI japeURI = japeOrigFile.toURI();

            // create feature map for the transducer
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                // set the grammar location
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                // set the grammar encoding
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }

            // create an instance of a JAPE Transducer processing resource
            ProcessingResource japeTransducerPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            // create corpus pipeline
            annotationPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            // add the processing resources (modules) to the pipeline
            annotationPipeline.add(documentResetPR);
            annotationPipeline.add(tokenizerPR);
            annotationPipeline.add(sentenceSplitterPR);
            annotationPipeline.add(GazetteerPR);
            annotationPipeline.add(japeTransducerPR);

            String pageContents = "";
            System.out.println("Enter the term:");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String what = br.readLine();
            ResultList results = search(what);
            //ResultList results = search("sport");
            if (results != null) {
                for (int i = 0; i < results.getResponseData().getResults().size(); i++) {
                    String url = results.getResponseData().getResults().get(i).getUrl();
                    try {
                        String text = sendHttpRequest("GET", url);
                        pageContents += "\n" + text;
                    } catch (Exception e) {

                    }

                }
            }

            Document document = Factory.newDocument(pageContents);
            //System.out.println(pageContents);
            // create a corpus and add the document
            Corpus corpus = Factory.newCorpus("");
            corpus.add(document);

            // set the corpus to the pipeline
            annotationPipeline.setCorpus(corpus);

            //run the pipeline
            annotationPipeline.execute();

            for (Document doc : corpus) {
                // get the default annotation set
                AnnotationSet as_default = doc.getAnnotations();

                FeatureMap fm = null;
                // get all Token annotations
                AnnotationSet annSetTokensCountry = as_default.get("Person", fm);
                int personCount = annSetTokensCountry.size();
                System.out.println(personCount);
                ArrayList tokenAnnotations = new ArrayList(annSetTokensCountry);

                ArrayList<String> list;
                list = new ArrayList<>(20);

                // looop through the Token annotations
                for (int j = 0; j < tokenAnnotations.size(); ++j) {

                    // get a token annotation
                    Annotation token = (Annotation) tokenAnnotations.get(j);

                    // get the underlying string for the Token
                    Node isaStart = token.getStartNode();
                    Node isaEnd = token.getEndNode();
                    String underlyingString = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    list.add(underlyingString);
                    FeatureMap annFM = token.getFeatures();
                }
                System.out.println(list.toString());
                System.out.println("ok");
            }

        } catch (GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initialiseGate() {

        try {
            // set GATE home folder
            // Eg. /Applications/GATE_Developer_7.0
            File gateHomeFile = new File("C:\\Program Files\\GATE_Developer_7.1");
            Gate.setGateHome(gateHomeFile);

            // set GATE plugins folder
            // Eg. /Applications/GATE_Developer_7.0/plugins
            File pluginsHome = new File("C:\\Program Files\\GATE_Developer_7.1\\plugins");
            Gate.setPluginsHome(pluginsHome);

            // set user config file (optional)
            // Eg. /Applications/GATE_Developer_7.0/user.xml
            //Gate.setUserConfigFile(new File("/Applications/GATE_Developer_7.0", "user.xml"));
            // initialise the GATE library
            Gate.init();

            // load ANNIE plugin
            CreoleRegister register = Gate.getCreoleRegister();
            URL annieHome = new File(pluginsHome, "ANNIE").toURL();
            register.registerDirectories(annieHome);

            // flag that GATE was successfuly initialised
            isGateInitilised = true;

        } catch (MalformedURLException | GateException ex) {
            Logger.getLogger(GateClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
