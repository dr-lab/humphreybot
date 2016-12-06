package gu.humphrey;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
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


        /**
         * wechat
         *
         */

        get("/wechat", (req, res)->{
            String echoStr = req.queryParams("echostr");
            String signature = req.queryParams("signature");
            String timestamp = req.queryParams("timestamp");
            String nonce = req.queryParams("nonce");

            System.out.println("echoStr:"+echoStr);
            System.out.println("signature:"+signature);
            System.out.println("timestamp:"+timestamp);
            System.out.println("nonce:"+nonce);

            String token = "1234567890";
            String[] tmpArray = new String[]{token, timestamp, nonce};
            Arrays.sort(tmpArray);

            String tmpStr = tmpArray[0]+tmpArray[1]+tmpArray[2];
            System.out.println("tmpStr:"+tmpStr);

            tmpStr = DigestUtils.sha1Hex(tmpStr);
            System.out.println("tmpStr+sha1:"+tmpStr);


            if( tmpStr.equals(signature)){
                System.out.println("signature match return original echoStri:"+echoStr);
                return echoStr;
            }else{
                System.out.println("signature NOT match:"+echoStr);
                return "error"+echoStr;
            }

        });

        post("/wechat", (req, res) -> {
            String postData = req.body();
            System.out.println("Incoming data in XML:");
            System.out.println(postData);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            ByteArrayInputStream input =  new ByteArrayInputStream(postData.getBytes("UTF-8"));
            Document doc = dBuilder.parse(input);

            System.out.println("parsing xml data successfully");

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Normalize doc successfully");

            Element xmlElement = doc.getDocumentElement();

            //Element xmlElement = doc.getElementById("xml");
            String fromUserName = xmlElement.getElementsByTagName("FromUserName").item(0).getFirstChild().getNodeValue();
            String toUsername = xmlElement.getElementsByTagName("ToUserName").item(0).getFirstChild().getNodeValue();
            String content = xmlElement.getElementsByTagName("Content").item(0).getFirstChild().getNodeValue();


            System.out.println("fromUserName:"+fromUserName);
            System.out.println("toUsername:"+toUsername);
            System.out.println("content:"+content);

            System.out.println("Response data in XML:");
            String response =  "<xml> <ToUserName><![CDATA["+fromUserName+"]]></ToUserName> " +
                    "<FromUserName><![CDATA["+toUsername+"]]></FromUserName> " +
                    "<CreateTime>"+Calendar.getInstance().getTimeInMillis()+"</CreateTime> " +
                    "<MsgType><![CDATA[text]]></MsgType> " +
                    "<Content><![CDATA["+getAnswer(content)+"]]></Content> " +
                    "<FuncFlag>0</FuncFlag> </xml>";
            System.out.println("Response data in XML:"+response);
            return response;
         });


        /**
         * Facebook
         */

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

        if(text.contains("buy")) {
            if (text.contains("iphone")) {
                return "iPhone7 start from $649, http://www.apple.com/shop/buy-iphone/iphone-7";
            }

            if(text.contains("ipad")){
                return "iPad start from $269, http://www.apple.com/ipad/";
            }

            if(text.contains("mac")){
                return "Macbook Pro start from $1499, http://www.apple.com/mac/";
            }

            return "try type: 'Buy iPhone', 'Buy iPad', 'Buy Mac'";
        }

        return "Nice to meet you! try type 'Buy iPhone', 'Buy iPad', 'Buy Mac', or type 'Quotes' for inspiration or type 'Music' for exciting!";


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
