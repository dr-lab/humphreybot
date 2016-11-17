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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    if("page".equals(map.get("object"))){

                        //body:{"object":"page",
                        // "entry":[{"id":"587172308136817","time":1479336429601,"messaging":[{"sender":{"id":"1392645960775398"},"recipient":{"id":"587172308136817"},"timestamp":1479336429569,"message":{"mid":"mid.1479336429569:eefeed0d71","seq":22,"text":"Hello"}}]}]}
                    }

                    CloseableHttpAsyncClient httpAsyncClients = HttpAsyncClients.createDefault();


                    HttpPost httpPost = new HttpPost(POST_END_POINT+PAGE_TOKEN);
                    StringEntity params =new StringEntity(buildAnswer(body));
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
                    return "body:" + body;
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

        String answer = "{recipient: {id: \"recipientId\"},message: {text: \"messageText\"}}";
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

                answer.replace("recipientId", recipientId);
                answer.replace("messageText", "nice to meet you!");
                return answer;
            }
        }

        return "error answer";
    }



    //for test only, token should not be public in Prod
    private static String PAGE_TOKEN="EAAEsjishk4QBAMvZCAC7NU4mSpMRhKsZCoUE41YVYDSc86f7tM4NEM7hJUZABDUi7aOStStDuUKbUlKoXJcSH1xjvodc7bZAbnZAgQdhd94tbWi44yiVx8uJHUyBVRhfWBcXdZC5MO6utPFOY05ixLOd9keDs724ZAMvZBO2bpgC4wZDZD";
    private static String POST_END_POINT = "https://graph.facebook.com/v2.6/me/messages?access_token=";


}
