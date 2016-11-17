package gu.humphrey;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * Created by HGU on 11/16/16.
 */
public class MainTest {
    /**
     * Test message response
     */
    @Test
    public void testMessageResponse(){
        //body:{"object":"page","entry":[{"id":"587172308136817","time":1479336429601,"messaging":[{"sender":{"id":"1392645960775398"},"recipient":{"id":"587172308136817"},"timestamp":1479336429569,"message":{"mid":"mid.1479336429569:eefeed0d71","seq":22,"text":"Hello"}}]}]}
        String body = "{\"object\":\"page\",\"entry\":[{\"id\":\"587172308136817\",\"time\":1479336429601,\"messaging\":[{\"sender\":{\"id\":\"1392645960775398\"},\"recipient\":{\"id\":\"587172308136817\"},\"timestamp\":1479336429569,\"message\":{\"mid\":\"mid.1479336429569:eefeed0d71\",\"seq\":22,\"text\":\"Hello\"}}]}]}";


        Gson gson = new Gson();
        Map map = gson.fromJson(body, Map.class);

        List entries = (List)map.get("entry");

        if(entries != null && entries.size()>0){
            Map entry = (Map)entries.get(0);
            List messagings = (List)entry.get("messaging");
            if(messagings!= null && messagings.size()>0){
                Map messaging = (Map)messagings.get(0);
                Map sender = (Map)messaging.get("sender");
                String senderId = (String)sender.get("id");
                System.out.println("sender ID:"+senderId);
            }
        }
    }

    //for test only, token should not be public in Prod
    private static String PAGE_TOKEN="EAAEsjishk4QBAMvZCAC7NU4mSpMRhKsZCoUE41YVYDSc86f7tM4NEM7hJUZABDUi7aOStStDuUKbUlKoXJcSH1xjvodc7bZAbnZAgQdhd94tbWi44yiVx8uJHUyBVRhfWBcXdZC5MO6utPFOY05ixLOd9keDs724ZAMvZBO2bpgC4wZDZD";
    private static String POST_END_POINT = "https://graph.facebook.com/v2.6/me/messages?access_token=";

    @Test
    public void sendMessage() throws UnsupportedEncodingException {
        CloseableHttpAsyncClient httpAsyncClients = HttpAsyncClients.createDefault();

        String answer = "{recipient: {id: \"1392645960775398\"},message: {text: \"Nice to meet you!\"}}";
        HttpPost httpPost = new HttpPost(POST_END_POINT+PAGE_TOKEN);
        StringEntity params =new StringEntity(answer);
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
                System.out.println("Sent message canceled! ");
            }
        });
    }
}
