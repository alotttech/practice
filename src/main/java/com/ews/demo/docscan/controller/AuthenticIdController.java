package com.ews.demo.docscan.controller;

import com.google.gson.Gson;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Controller
public class AuthenticIdController {
    // AUthenticID stuff
    public class AidLoginReq {
        public String username;
        public String password;

        public AidLoginReq(String u, String p) {
            username = u;
            password = p;
        }
    }
    public class AidLoginRsp {
        public String jwt;
    }

    public class AidVerifyReq {
        public String channel;
        public String channelDetails;
        //public String channelConfiguration;
        //public String channelResponse;
        //public  String transactionDetails;
        public String postbackURL;
        public String redirectURL;

        public AidVerifyReq(String c, String d) {
            channel = c;
            channelDetails = d;
        }
    }
    public class AidVerifyRsp {
        public String requestID;
        public String url;
    }

    public String jwt = "";
    @GetMapping("/authenticid/verify")
    public String aidVerify(@RequestParam(name = "phone", required = false, defaultValue = "") String phoneNo, Model model) {
        try {
            Gson gson = new Gson();
            model.addAttribute("phoneNo", phoneNo);

            String aidRootUrl = "https://cfweb.amalgam.ascendant.cloud/api/";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            AidLoginReq aidLoginReq = new AidLoginReq("ming.chen@earlywarning.com", "!CLM21#gninraw@ylrae");
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(aidLoginReq), headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> result = restTemplate.postForEntity(aidRootUrl + "/login", request, String.class);
            assertThat(result.getStatusCode(), equalTo(HttpStatus.OK));
            assertThat(result.getBody(), notNullValue());
            AidLoginRsp response = gson.fromJson(result.getBody(), AidLoginRsp.class);
            assertThat(response.jwt, notNullValue());

            jwt = response.jwt;
            AidVerifyReq aidVerifyReq = null;
            if (phoneNo.isBlank() || phoneNo.isEmpty())
                aidVerifyReq = new AidVerifyReq("URL", "");
            else
                aidVerifyReq = new AidVerifyReq("PHONE", '+'+phoneNo);
            aidVerifyReq.postbackURL = "http://localhost:8081/authenticid/aidPostCallback";
            aidVerifyReq.redirectURL = "http://localhost:8081/authenticid/aidRedirect";

            String token = "Bearer " + jwt;
            headers.set("Authorization", token);
            String jsonStr = gson.toJson(aidVerifyReq);
            request = new HttpEntity<>(gson.toJson(aidVerifyReq), headers);

            result = restTemplate.postForEntity(aidRootUrl + "/verify", request, String.class);

            assertThat(result.getStatusCode(), equalTo(HttpStatus.OK));
            assertThat(result.getBody(), notNullValue());
            AidVerifyRsp rsp = gson.fromJson(result.getBody(), AidVerifyRsp.class);
            assertThat(rsp.requestID, notNullValue());
            assertThat(rsp.url, notNullValue());

            model.addAttribute("reqId", rsp.requestID);
            model.addAttribute("qrLink", rsp.url);
            model.addAttribute("displayQR", true);
            //model.addAttribute("message", "Please scan QR code!");
            model.addAttribute("message", rsp.url);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", e.getMessage());
        }

        return "authenticid"; //view
    }

    public class PostCallbackResponse {
        public String requestID;
        public String requestStatus;
        public Transaction[] transactions;
    }

    public class Transaction {
        public String transactionID;
        public String transactionStatus;
        public String transactionSubStatus;
        public int sequenceNumber;
        public String statusColor;
        public String segmentName;
        public ActionCodes actionCodes;
        public Pii pii;
        // passIDEThreshold : [] ,
        // failIDEThreshold : [] ,
        // uncertainIDEThreshold : []
    }

    public class ActionCodes {
        public double code;
        public String actionMessage;
        public String cat;
    }
    public class Pii {
        public String addressCity;
        public String addressPostalCode;
        public String addressState;
        public String addressStreet1;
        public String dateOfBirth;
        public String gender;
        public String idExpirationDate;
        public String idNumber;
        public String imageFront;
        public String imageSelfie;
        public String nameFirstname;
        public String nameFullname;
        public String nameLastname;
        public String selfieMatchscore;
        public String documentName;
        public String documentIssuerName;
        public String documentIssuerCode;
        public String documentClassName;
    }

    @GetMapping("/authenticid/aidRedirect")
    public String aidRedirect(Model model) {
        return "authenticid"; //view
    }

    @PostMapping("/authenticid/aidPostCallback")
    public String aidPostCallback(@RequestBody String response, Model model) {
        Gson gson = new Gson();

        PostCallbackResponse postCallbackResponse = gson.fromJson(response, PostCallbackResponse.class);
        assertThat(postCallbackResponse.requestID, notNullValue());
        assertThat(postCallbackResponse.requestStatus, notNullValue());
        assertThat(postCallbackResponse.transactions, notNullValue());
        assertThat(postCallbackResponse.transactions[0].pii.nameFullname, notNullValue());
        model.addAttribute("message", postCallbackResponse.transactions[0].pii.nameFullname);
        return "authenticid"; //view
    }

    @RequestMapping(value="/authenticid/aidGetStatus", method=RequestMethod.GET, produces="text/plain")
    @ResponseBody
    public String aidGetStatus(@RequestParam(name = "reqId", required = true, defaultValue = "") String reqId) {
        String result = "Failed to Get Status";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", reqId);
            String token = "Bearer " + jwt;
            headers.set("Authorization", token);

            ResponseEntity<String> entity = new RestTemplate().exchange(
                    "https://cfweb.amalgam.ascendant.cloud/api/status", HttpMethod.GET, new HttpEntity<String>(headers),
                    String.class);
            PostCallbackResponse postCallbackResponse = new Gson().fromJson(entity.getBody(), PostCallbackResponse.class);

            assertThat(postCallbackResponse.requestID, notNullValue());
            assertThat(postCallbackResponse.requestStatus, notNullValue());
            assertThat(postCallbackResponse.transactions, notNullValue());
            if (postCallbackResponse.transactions[0].pii == null)
                result = postCallbackResponse.requestStatus;
            else
                result = postCallbackResponse.transactions[0].pii.nameFullname;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping(value = "/authenticid/aidGetStatus2")
    public String aidGetStatus2(@RequestParam String reqId, @RequestParam int duration, Model model) {
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        while(duration > 0) {
            try {
                Thread.sleep(5000);
                duration -= 5;

                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Token", reqId);
                ResponseEntity<PostCallbackResponse> entity = new RestTemplate().exchange(
                        "https://cfweb.amalgam.ascendant.cloud/api/status", HttpMethod.GET, new HttpEntity<Object>(headers),
                        PostCallbackResponse.class);

                PostCallbackResponse postCallbackResponse = entity.getBody();
                assertThat(postCallbackResponse.requestID, notNullValue());
                assertThat(postCallbackResponse.requestStatus, notNullValue());
                assertThat(postCallbackResponse.transactions, notNullValue());
                assertThat(postCallbackResponse.transactions[0].pii.nameFullname, notNullValue());
                model.addAttribute("message", postCallbackResponse.requestStatus);
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("message", e.getMessage());
            }
            return "authenticid";
        }

        model.addAttribute("message", "TIMEOUT -- ABORT!!!");
        return "authenticid";
    }
}
