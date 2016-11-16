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
                  System.out.println("got request on /hook");
                  if ("subscribe".equals(req.queryParams("hub.mode"))) {
                      System.out.println("Validating hub.mode");
                      if ("1234567890".equals(req.queryParams("hub.verify_token"))) {
                          System.out.println("Validating passed");
                          res.status(HttpServletResponse.SC_OK);
                          return req.queryParams("hub.challenge");
                      } else {
                          System.out.println("Failed validation. Make sure the validation tokens match.");
                          res.status(HttpServletResponse.SC_FORBIDDEN);
                          return "token code not match challenge code";
                      }
                  }

                  String body = req.body();
                  System.out.println("body:"+body);

                  return "body:"+body;
              }


      );

    get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("message", "Hello World!");

            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());


  }

}
