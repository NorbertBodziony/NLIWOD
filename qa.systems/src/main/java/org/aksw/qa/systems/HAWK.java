package org.aksw.qa.systems;

import java.net.URI;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashSet;

import org.aksw.qa.commons.datastructure.IQuestion;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HAWK extends ASystem {
    Logger log = LoggerFactory.getLogger(HAWK.class);

    private Decoder based64Decoder = Base64.getDecoder();

    public String name() {
        return "hawk";
    };

    public void search(IQuestion question) {
        String questionString;
        if (!question.getLanguageToQuestion().containsKey("en")) {
            return;
        }
        questionString = question.getLanguageToQuestion().get("en");
        log.debug(this.getClass().getSimpleName() + ": " + questionString);
        try {

            HttpClient client = HttpClientBuilder.create().build();
            URI iduri = new URIBuilder().setScheme("http").setHost("139.18.2.164:8181").setPath("/search")
                    .setParameter("q", questionString).build();
            HttpGet httpget = new HttpGet(iduri);
            HttpResponse idresponse = client.execute(httpget);

            String id = responseparser.responseToString(idresponse);
            JSONParser parser = new JSONParser();

            URI quri = new URIBuilder().setScheme("http").setHost("139.18.2.164:8181").setPath("/status")
                    .setParameter("UUID", id.substring(1, id.length() - 2)).build();

            int j = 0;
            do {
                Thread.sleep(50);
                HttpGet questionpost = new HttpGet(quri);
                HttpResponse questionresponse = client.execute(questionpost);
                JSONObject responsejson = (JSONObject) parser.parse(responseparser.responseToString(questionresponse));
                if (responsejson.containsKey("answer")) {
                    JSONArray answerlist = (JSONArray) responsejson.get("answer");
                    HashSet<String> result = new HashSet<String>();
                    for (int i = 0; i < answerlist.size(); i++) {
                        JSONObject answer = (JSONObject) answerlist.get(i);
                        result.add(answer.get("URI").toString());
                    }
                    question.setGoldenAnswers(result);
                    if (responsejson.containsKey("final_sparql_base64")) {
                        String sparqlQuery = responsejson.get("final_sparql_base64").toString();
                        sparqlQuery = new String(based64Decoder.decode(sparqlQuery), Charsets.UTF_8);
                        question.setSparqlQuery(sparqlQuery);
                    }
                }
                j = j + 1;
            } while (j < 500);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
