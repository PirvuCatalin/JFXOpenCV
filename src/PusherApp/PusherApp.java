package PusherApp;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import java.io.IOException;


public class PusherApp {


    public static void main(String[] args) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://fcm.googleapis.com/fcm/send");
        post.setHeader("Content-type", "application/json");
        post.setHeader("Authorization", "key=AAAAFYWN4_0:APA91bGA2f7Rvae4JcvJcsdqKoTvWnquVVp3KE5BAWZQ4tC5BCN2xRqOPqMZvGH4XAaz2d67SHqKzEshlfv8yrNMvg52jhmE8MiGshzTKOXETWBzdHP9OsI728lBd6YnXqFFhcPK3bxO");

        JSONObject message = new JSONObject();
        message.put("to", "f2ZgB-C7_0o:APA91bHMIhorrcMCsDsAdrcJfDaH17WFvDIB8yls58uS4Z4vcYHP9oOiUA8orm0NZ8w1rWByZSNJEVeyIrHRRUWq2fKdLpjJ4CVIDLzkdymUxl3HgFGIwcveQzukoapaPAtDKW38l5nP");
        message.put("priority", "high");

        JSONObject notification = new JSONObject();
        notification.put("title", "Java");
        notification.put("body", "Notificação do Java");

        message.put("notification", notification);

        post.setEntity(new StringEntity(message.toString(), "UTF-8"));
        HttpResponse response = client.execute(post);
        System.out.println(response);
        System.out.println(message);
    }
}
