
package com.oracle.flb.chatops;

import io.helidon.http.Status;
import io.helidon.logging.common.HelidonMdc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.client.Entity.json;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 * A simple JAX-RS resource to greet you. Examples:
 *
 * Get default greeting message:
 * curl -X GET http://localhost:8080/simple-greet
 *
 * The message is returned as a JSON object.
 */
@ApplicationScoped
@Path("/social")
public class FLBSocialCommandResource {
    private static final String FALSE = "FALSE";
    private static final String FLB_SOCIAL = "FLBSocial"; // used for the logging naming
    private static final String NAME = "name";
    private static final String TESTFLB = "TESTFLB";
    private static final String TESTFLB_TAG = "TESTFLB_TAG";
    private static final String TESTFLB_PORT = "TESTFLB_PORT";
    private static final String TESTFLB_COMMAND = "TESTFLB_COMMAND";
    private static final String TESTFLB_NODE = "TESTFLB_NODE";
    private static final int DEFAULT_RETRY_COUNT = 2;
    private static final int DEFAULT_RETRY_INTERVAL = 60;
    private static final String FLB_DEFAULT_PORT = "2020";
    private static final String PORT = "OPS_PORT";
    private static final String RETRYINTERVAL = "OPS_RETRYINTERVAL";
    private static final String RETRYCOUNT = "OPS_RETRYCOUNT";

    private static final Logger LOGGER = Logger.getLogger(FLBSocialCommandResource.class.getName());
    private String myFLBPort = FLB_DEFAULT_PORT;
    private int myRetryDelay = 60;
    private int myRetryCount = 0;
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH-mm-ss");
    private Map<String, String> myEnvs = new HashMap<>();

    private static String createFLBPayload(String command, String tag) {
        return "{\"command\":\"" + command +
                "\", \"time\":\"" + dtf.format(LocalDateTime.now()) +
                "\", \"tagged\":\"" + tag +
                "\"}\n";
    }

    private static boolean signalFLBNode(String node, String command, String tag) {
        boolean sent = true;
        Client client = null;
        String svr = "http://" + node;
        LOGGER.info("Sending to FLB Node " + svr + " command " + createFLBPayload(command, tag) + " tagged as " + tag);
        try {
            client = ClientBuilder.newClient();
            Response resp = client.target(svr).path("command").request(MediaType.APPLICATION_JSON)
                    .buildPost(json(createFLBPayload(command, tag))).invoke();

            sent = (resp.getStatus() >= 200) && (resp.getStatus() < 300);
            resp.close();
            client.close();
        } catch (Exception err) {
            LOGGER.warning(err.toString());
        } finally {
            if (client != null) {
                client.close();
            }
        }

        return sent;

    }

    /*
     * So we can examine data structures such as the environment variables very
     * easily, this prints the contents of a map in a very readable manner
     */
    private static String prettyPrintMap(Map map) {
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<String, String>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            sb.append(entry.getKey());
            sb.append('=').append('"');
            sb.append(entry.getValue());
            sb.append('"');
            if (iter.hasNext()) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /*
     * This inner class provides the thread mechanisms to allow us to separate out
     * the looking for a social channel response, and allow the inbound request to
     * be returned, so we don't create any issues with HTTP timeouts, or creating
     * back pressure as we wait on the user to act in the social channel
     */
    private static class ThreadedChannelChecker implements Runnable {
        private String id = "";
        private int retries = 0;
        private int delay = 0;
        Map<String, String> env = null;

        ThreadedChannelChecker(String instanceId, int retries, int delaySecs, Map<String, String> env) {
            this.id = instanceId;
            this.retries = retries;
            this.delay = delaySecs * 1000;
            this.env = env;
            LOGGER.finer("Establishing Checker thread with id=" + this.id + " retries " + this.retries
                    + " delay Seconds = " + delaySecs);
        }

        /**
         * @param alertId
         * @return FLBCommunication
         */
        private FLBCommunication checkForAction(
                ActionsImplInterface action) {
            FLBCommunication comms = null;
            LOGGER.info("checkForAction commencing");
            try {
                comms = action.checkForAction();
                if (comms.getFLBNode() != null) {
                    LOGGER.info("---------WE HAVE A NODE -----------" + comms.getFLBNode());
                }
                if (comms.getCommand() != null) {
                    LOGGER.info("---------WE HAVE A Command -----------" + comms.getCommand());
                }

            } catch (Exception error) {
                LOGGER.warning("checkForAction error:" + error.toString());
            }

            return comms;
        }

        @Override
        public void run() {
            int counter = 0;
            try {
                ActionsImplInterface action = SocialActionsFactory.getSocialChannelsActionImpl(env);
                // new SlackActions(env);
                while (counter < retries) {
                    FLBCommunication comms = checkForAction(action);
                    if ((comms != null) && comms.canAction()) {
                        boolean sent = signalFLBNode(comms.getFLBNode(), comms.getCommand(), id);
                        if (sent) {
                            LOGGER.info("Actioning:\n" + comms.summaryString());
                        } else {
                            LOGGER.info("Actioning failed:\n" + comms.summaryString());
                        }
                        action.sendMessage("Managed to send command to Node");
                        break;
                    }
                    counter++;
                    Thread.sleep(delay);
                    LOGGER.finer(
                            "Thread ID: " + Thread.currentThread().getName()
                                    + ">>>>>>>>>>>>>> checked for action, sleeping for " + delay);
                }
            } catch (Throwable thrown) {
                LOGGER.warning("Checker Run error:" + thrown.toString());
            }
            LOGGER.info("Thread : " + Thread.currentThread().getName() + " ending <<<<<<<<<<<<<");

        }

    }

    /*
     * Constructor for this resource which the container will interact with. We
     * collect all the necessary environmental params ready. soon as a call is
     * received we can get
     */
    FLBSocialCommandResource() {
        HelidonMdc.set(NAME, FLB_SOCIAL);
        myEnvs.putAll(System.getenv());
        LOGGER.finer("FLBSocialCommandResource - Envs obtained:" + prettyPrintMap(myEnvs));

        try {
            int aPort = Integer.parseInt(myEnvs.getOrDefault(PORT, FLB_DEFAULT_PORT));
            myFLBPort = Integer.toString(aPort);
        } catch (NumberFormatException numErr) {
            LOGGER.warning("FLBSocialCommandResource - Couldn't process port override");
        }

        try {
            myRetryCount = Integer.parseInt(myEnvs.getOrDefault(RETRYCOUNT, Integer.toString(DEFAULT_RETRY_COUNT)));
        } catch (NumberFormatException numErr) {
            LOGGER.warning("FLBSocialCommandResource - Couldn't process retry count - using default");
            myRetryCount = DEFAULT_RETRY_COUNT;
        }

        try {
            myRetryDelay = Integer
                    .parseInt(myEnvs.getOrDefault(RETRYINTERVAL, Integer.toString(DEFAULT_RETRY_INTERVAL)));
        } catch (NumberFormatException numErr) {
            LOGGER.warning("FLBSocialCommandResource - Couldn't process retry interval - using default");
            myRetryDelay = DEFAULT_RETRY_INTERVAL;
        }

        myEnvs = SocialActionsFactory.addProperties(myEnvs);
    }

    /**
     * @return String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = FLBCommandMetrics.ALL_SOCIALS_NAME, absolute = true, description = FLBCommandMetrics.ALL_SOCIALS_DESCRIPTION)
    @Timed(name = FLBCommandMetrics.SOCIALS_TIMER_NAME, description = FLBCommandMetrics.SOCIALS_TIMER_DESCRIPTION, unit = MetricUnits.HOURS, absolute = true)
    public String getNoAlertId() {
        ThreadedChannelChecker checker = new ThreadedChannelChecker(dtf.format(LocalDateTime.now()), myRetryCount,
                myRetryDelay, myEnvs);
        checker.run();
        return "{getNoAlertId= " + checker.id + "}";

    }

    /**
     * @return String
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = FLBCommandMetrics.ALL_SOCIALS_NAME, absolute = true, description = FLBCommandMetrics.ALL_SOCIALS_DESCRIPTION)
    @Timed(name = FLBCommandMetrics.SOCIALS_TIMER_NAME, description = FLBCommandMetrics.SOCIALS_TIMER_DESCRIPTION, unit = MetricUnits.HOURS, absolute = true)
    public String postAlertNoId(String entity) {
        ThreadedChannelChecker checker = new ThreadedChannelChecker(dtf.format(LocalDateTime.now()), myRetryCount,
                myRetryDelay, myEnvs);
        checker.run();
        return "{postAlertNoId= " + checker.id + "}";

    }

    /**
     * @return String
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = FLBCommandMetrics.ALL_SOCIALS_NAME, absolute = true, description = FLBCommandMetrics.ALL_SOCIALS_DESCRIPTION)
    @Timed(name = FLBCommandMetrics.SOCIALS_TIMER_NAME, description = FLBCommandMetrics.SOCIALS_TIMER_DESCRIPTION, unit = MetricUnits.HOURS, absolute = true)
    public String putAlertNoId(String entity) {
        ThreadedChannelChecker checker = new ThreadedChannelChecker(dtf.format(LocalDateTime.now()), myRetryCount,
                myRetryDelay, myEnvs);
        checker.run();
        return "{PutNoAlertId= " + checker.id + "}";

    }

    @Path("/{alertId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(name = FLBCommandMetrics.ALL_SOCIALS_NAME, absolute = true, description = FLBCommandMetrics.ALL_SOCIALS_DESCRIPTION)
    @Timed(name = FLBCommandMetrics.SOCIALS_TIMER_NAME, description = FLBCommandMetrics.SOCIALS_TIMER_DESCRIPTION, unit = MetricUnits.HOURS, absolute = true)
    public String postWithAlertId(@PathParam("alertId") String alertId) {
        ThreadedChannelChecker checker = new ThreadedChannelChecker(dtf.format(LocalDateTime.now()), myRetryCount,
                myRetryDelay, myEnvs);
        checker.run();
        return "{postWithAlertId= " + checker.id + "}";

    }

    /**
     * @return String
     */
    @Path("/testFLB")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = FLBCommandMetrics.ALL_SOCIALS_NAME, absolute = true, description = FLBCommandMetrics.ALL_SOCIALS_DESCRIPTION)
    @Timed(name = FLBCommandMetrics.SOCIALS_TIMER_NAME, description = FLBCommandMetrics.SOCIALS_TIMER_DESCRIPTION, unit = MetricUnits.HOURS, absolute = true)
    public String postTestFLB(String entity) {
        boolean testFLB = Boolean.parseBoolean(myEnvs.getOrDefault(TESTFLB, FALSE));
        LOGGER.info("Test FLB allowed = " + testFLB);
        if (testFLB) {

            signalFLBNode(myEnvs.getOrDefault(TESTFLB_NODE, "localhost:8090"),
                    myEnvs.getOrDefault(TESTFLB_COMMAND, "test"), myEnvs.getOrDefault(TESTFLB_TAG, "command"));
        }
        return "{TestFLBDisabled= \"" + !testFLB + "\"}";

    }
}
