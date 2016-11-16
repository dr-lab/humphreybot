package gu.humphrey;

import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static spark.Spark.get;
import static spark.SparkBase.port;
import static spark.SparkBase.staticFileLocation;

public class Main {

  public static void main(String[] args) {

    port(Integer.valueOf(args[0]));

    staticFileLocation("/public");

    get("/hello", (req, res) -> "Hello World");

      get("/hook", (req, res) -> {
          Set<String> headers = req.headers();
          System.out.println("headers=" + req.headers());
          headers.forEach((v)->{
                      System.out.println(v+"="+req.headers(v));
                  }
          );

          System.out.println("attributes=" + req.attributes());
          System.out.println("body=" + req.body());
          System.out.println("cookies="+req.cookies());
          System.out.println("host="+req.host());
          System.out.println("params=" + req.params());
          System.out.println("queryString="+req.queryString());
          System.out.println("queryMap="+req.queryMap());
          System.out.println("queryMap="+req.queryMap().toMap());
          System.out.println("queryParams="+req.queryParams());
          System.out.println("hub.mode="+req.queryParams("hub.mode")
                  + "; hub.verify_token="+req.queryParams("hub.verify_token")
                  + "; hub.challenge="+req.queryParams("hub.challenge"));
          if ("subscribe".equals(req.queryParams("hub.mode")) &&
                  "1234567890".equals(req.queryParams("hub.verify_token"))) {
              System.out.println("Validating webhook");
              res.status(HttpServletResponse.SC_OK);
              return req.queryParams("hub.challenge");
          } else {
              System.out.println("Failed validation. Make sure the validation tokens match.");
              res.status(HttpServletResponse.SC_FORBIDDEN);
              return "token code not match challenge code";
          }
      });

    get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("message", "Hello World!");

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());


  }

}
