package gu.humphrey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.params.HttpParams;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import sun.net.www.http.HttpClient;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.*;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.SparkBase.port;
import static spark.SparkBase.staticFileLocation;

public class Main {


    public static void main(String[] args) {

        port(Integer.valueOf(args[0]));

        staticFileLocation("/public");

        get("/hello", (req, res) -> "Hello World");

        get("/hook", (req, res) -> {
                    System.out.println("got get on /hook");
                    if ("subscribe".equals(req.queryParams("hub.mode")) &&
                            ("1234567890".equals(req.queryParams("hub.verify_token")))) {
                        System.out.println("Validating passed");
                        res.status(HttpServletResponse.SC_OK);
                        return req.queryParams("hub.challenge");
                    } else {
                        System.out.println("Failed validation. Make sure the validation tokens match.");
                        res.status(HttpServletResponse.SC_FORBIDDEN);
                        return "token code not match challenge code";
                    }

                }


        );

        //all message are POST
        post("/hook", (req, res) -> {
                    System.out.println("got post on /hook");

                    String body = req.body();
                    System.out.println("body:" + body);

                    Gson gson = new Gson();
                    Map<String, Object> map = gson.fromJson(body, Map.class);
                    if("page".equals(map.get("object"))) {

                        //body:{"object":"page",
                        // "entry":[{"id":"587172308136817","time":1479336429601,"messaging":[{"sender":{"id":"1392645960775398"},"recipient":{"id":"587172308136817"},"timestamp":1479336429569,"message":{"mid":"mid.1479336429569:eefeed0d71","seq":22,"text":"Hello"}}]}]}

                        CloseableHttpAsyncClient httpAsyncClients = getHttpAsyncClients();

                        String answer = buildAnswer(body);

                        if (answer != null) {
                            HttpPost httpPost = new HttpPost(POST_END_POINT + PAGE_TOKEN);
                            StringEntity params = new StringEntity(buildAnswer(body));
                            httpPost.setHeader("Content-type", "application/json");
                            httpPost.setEntity(params);

                            httpAsyncClients.execute(httpPost, new FutureCallback<HttpResponse>() {
                                @Override
                                public void completed(HttpResponse httpResponse) {
                                    System.out.println("Sent message successfully!");
                                }

                                @Override
                                public void failed(Exception e) {
                                    System.out.println("Sent message failed " + e);
                                }

                                @Override
                                public void cancelled() {

                                }
                            });
                        }
                    }

                    return "";
                }


        );

        get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("message", "Hello World!");

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());


    }

    public static String buildAnswer(String inComingResponse){
        //body:{"object":"page","entry":[{"id":"587172308136817","time":1479336429601,"messaging":[{"sender":{"id":"1392645960775398"},"recipient":{"id":"587172308136817"},"timestamp":1479336429569,"message":{"mid":"mid.1479336429569:eefeed0d71","seq":22,"text":"Hello"}}]}]}
        //String body = "{\"object\":\"page\",\"entry\":[{\"id\":\"587172308136817\",\"time\":1479336429601,\"messaging\":[{\"sender\":{\"id\":\"1392645960775398\"},\"recipient\":{\"id\":\"587172308136817\"},\"timestamp\":1479336429569,\"message\":{\"mid\":\"mid.1479336429569:eefeed0d71\",\"seq\":22,\"text\":\"Hello\"}}]}]}";

        String answer = "{\"recipient\": {\"id\": \"recipientId\"},\"message\": {\"text\": \"messageText\"}}";
        Gson gson = new Gson();
        Map map = gson.fromJson(inComingResponse, Map.class);

        List entries = (List)map.get("entry");

        if(entries != null && entries.size()>0){
            Map entry = (Map)entries.get(0);
            List messagings = (List)entry.get("messaging");
            if(messagings!= null && messagings.size()>0){
                Map messaging = (Map)messagings.get(0);
                Map sender = (Map)messaging.get("sender");
                String recipientId = (String)sender.get("id");
                System.out.println("sender ID:" + recipientId);

                Map message = (Map)messaging.get("message");
                if(message != null) {
                    String text = (String) message.get("text");
                    if(text != null && text.length()>0) {
                        answer = answer.replace("recipientId", recipientId);


                        answer = answer.replace("messageText", getAnswer(text));

                        System.out.println("answer json:" + answer);
                        return answer;
                    }
                }
            }
        }

        return null;
    }

    public static String getAnswer(String text){
        text = text.toLowerCase();
        if(text.contains("quote")){
            return getQuote();
        }

        if(text.contains("music")){
            return "Never stop playing, start FREE for 3 months. http://www.apple.com/music/";
        }

        if(text.contains("price")) {
            if (text.contains("iphone")) {
                return "iPhone7 start from $649, http://www.apple.com/shop/buy-iphone/iphone-7";
            }

            if(text.contains("ipad")){
                return "iPad start from $269, http://www.apple.com/ipad/";
            }

            if(text.contains("mac")){
                return "Macbook Pro start from $1499, http://www.apple.com/mac/";
            }

            return "try type: 'iPhone Price', 'iPad Price', 'Mac Price'";
        }

        return "Nice to meet you! try type 'iPhone Price', 'iPad Price', 'Mac Price', or type 'Quotes' for inspiration or type 'music' for exciting!";


    }

    public static String getQuote(){
        int index = random.nextInt(Quotes.quotes.length-1);
        return Quotes.quotes[index];
    }


    private static CloseableHttpAsyncClient httpAsyncClients = HttpAsyncClients.createDefault();

    private static CloseableHttpAsyncClient getHttpAsyncClients(){
        httpAsyncClients.start();;
        return httpAsyncClients;
    }



    //for test only, token should not be public in Prod
    private static String PAGE_TOKEN="EAAEsjishk4QBAIhxXPb0YmadkB1YjsJto4NmpQkoewjhTkGZC9J4Xfs4V3w9egrCsZB8FWntFfG9iDRIrN9vHBwVmP1PVR7wg9ZAQ9EalhdWFoVrrk9TuZA2So6PCDC3XhSMeCG5snFubRIZChwsCVLmPUdGlCwAPEZBJrFyfj6gZDZD";
    private static String POST_END_POINT = "https://graph.facebook.com/v2.6/me/messages?access_token=";

    private static Random random = new Random();
}
