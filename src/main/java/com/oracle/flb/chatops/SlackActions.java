package com.oracle.flb.chatops;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

public class SlackActions implements ActionsImplInterface {

  private static final String BEARER_SLACK_HDR = "Bearer ";

  private static final String METADATA_SLACK_ATTRIBUTE = "include_all_metadata";

  private static final String LIMIT_SLACK_ATTRIBUTE = "limit";

  private static final String OLDEST_SLACK_ATTRIBUTE = "oldest";

  private static final String CHANNEL_SLACK_ATTRIBUTE = "channel";

  private static final String SLACKTIMELIMIT = "SLACK_TIME_LIMIT";

  private static final String SLACKMSGLIMIT = "SLACK_MSG_LIMIT";

  private static final String SLACKTOKEN = "SLACK_TOKEN";

  private static final String SLACKCHANNELID = "SLACK_CHANNEL_ID";

  private static final int DEFAULTMSGLIMIT = 5;

  private static final String FIND_COMMANDS_JSONPATH = "$.messages[*].text";
  private static final String FIND_OK_JSONPATH = "$.ok";

  private static final String FLB = "FLBCmd:";
  private static final String FLBNODE = "FLBNode:";

  private static final String SLACK_HISTORY_URL = "https://slack.com/api/conversations.history";
  private static final String SLACK_WRITE_URL = "https://slack.com/api/chat.postMessage";
  // https://api.slack.com/methods/chat.postMessage

  // curl -d "channel=CMM13QSBB" -d "limit=3" -d "include_all_metadata=false" -H
  // "Authorization: Bearer
  // xoxb-YOUR TOKEN HERE -X POST
  // https://slack.com/api/conversations.history

  private String myChannelId;
  private String mySlackToken;
  private boolean myIncludeMetadata = false;
  private int myMsgCountLimit;
  private Integer myMsgTimeLimit = null;
  FLBCommunication myResults = new FLBCommunication();
  private ReadContext myJsonCtx = null;
  private String myAlertId = "";

  private long myCreated = Instant.now().getEpochSecond();
  private long myLastCheck = myCreated;

  static final Logger LOGGER = Logger.getLogger(SlackActions.class.getName());

  /*
   * A static method that will propulate the received map with any additional name
   * value pairs that maybe needed or overridden by this type of action. The
   * enriched result is returned to the caller
   */
  public static Map<String, String> addPropertiesStatic(Map<String, String> config) {

    String channelId = config.getOrDefault(SLACKCHANNELID, null);
    if ((channelId == null) || (channelId.trim().length() == 0)) {
      channelId = System.getenv(SLACKCHANNELID);
      if ((channelId == null) || (channelId.trim().length() == 0)) {
        LOGGER.info("SLACKCHANNELID is not set");
      }
    }
    config.put(SLACKCHANNELID, channelId);

    String mySlackToken = config.getOrDefault(SLACKTOKEN, null);
    if ((mySlackToken == null) || (mySlackToken.trim().length() == 0)) {
      mySlackToken = System.getenv(SLACKTOKEN);
      if ((mySlackToken == null) || (mySlackToken.trim().length() == 0)) {

        LOGGER.info("SlackToken is not set");
      }
    }
    config.put(SLACKTOKEN, mySlackToken);

    int msgCountLimit = DEFAULTMSGLIMIT;
    try {
      msgCountLimit = Integer.parseInt(config.getOrDefault(SLACKMSGLIMIT, Integer.toString(DEFAULTMSGLIMIT)));
    } catch (NumberFormatException err) {
      LOGGER.info("Ignoring config msg limit value - couldnt process");
    }
    config.put(SLACKMSGLIMIT, Integer.toString(msgCountLimit));

    int msgTimeLimit = 0;
    if (config.containsKey(SLACKTIMELIMIT)) {
      try {
        msgTimeLimit = new Integer(config.get(SLACKTIMELIMIT));
      } catch (NumberFormatException err) {
        LOGGER.info("Ignoring config time limit value - couldnt process");
      }
    }
    config.put(SLACKTIMELIMIT, Integer.toString(msgTimeLimit));

    return config;
  }

  /*
   * Initialise this instance of the implementation setting up object variables
   * from the environment properties collected
   */
  SlackActions(Map<String, String> config) {

    myChannelId = System.getenv(SLACKCHANNELID);

    mySlackToken = System.getenv(SLACKTOKEN);
    myMsgCountLimit = Integer.parseInt(config.getOrDefault(SLACKMSGLIMIT, Integer.toString(DEFAULTMSGLIMIT)));
    myMsgTimeLimit = new Integer(config.get(SLACKTIMELIMIT));

  }

  /*
   * Instance version of the addProperties - only here as historially we executed
   * this task against the object instance. Rather than just doing it once on the
   * class
   */
  @Override
  public Map<String, String> addProperties(Map<String, String> config) {
    return SlackActions.addPropertiesStatic(config);
  }

  /*
   * Setup the data structure containing the payload that provides Slack with the
   * details of a request to send a message to be writtien into the slack channel
   */
  private MultivaluedHashMap<String, String> setConversationWriteParams(String message) {
    MultivaluedHashMap<String, String> entity = new MultivaluedHashMap<>();

    entity.add(LIMIT_SLACK_ATTRIBUTE, Integer.toString(myMsgCountLimit));
    entity.add(CHANNEL_SLACK_ATTRIBUTE, myChannelId);
    entity.add("text", message);

    // alternative to
    // https://api.slack.com/methods/chat.postMessage
    return entity;
  }

  /*
   * This initialises the parameters that will tell Slack what data we want back.
   * The constrinats are a maximum number of responses, the responses can't be
   * older than 5 seconds ago
   * We're only going to look in a specific channel
   */
  private MultivaluedHashMap<String, String> setConversationReadParams() {
    MultivaluedHashMap<String, String> entity = new MultivaluedHashMap<>();

    LOGGER
        .finer("initialiseParams time from epoch:" + myLastCheck + " my limit is " + myMsgCountLimit);

    entity.add(CHANNEL_SLACK_ATTRIBUTE, myChannelId);
    entity.add(LIMIT_SLACK_ATTRIBUTE, Integer.toString(myMsgCountLimit));
    entity.add(METADATA_SLACK_ATTRIBUTE, Boolean.toString(myIncludeMetadata));
    entity.add(OLDEST_SLACK_ATTRIBUTE, Long.toString(myLastCheck - 5));
    myLastCheck = Instant.now().getEpochSecond();

    // alternative to
    // https://api.slack.com/methods/conversations.history
    return entity;
  }

  /*
   * We have a string which contains candidates for the command and node
   * identifiers. The payload is JSON but we're processing it as a string, so we
   * don't have to understand the JSON structure
   */
  private String extractCommand(String identifier, String candidate, String command) {
    String commandCandidate = candidate.substring(candidate.indexOf(identifier) + identifier.length()).trim();
    if (commandCandidate.length() > 0) {
      int space = commandCandidate.indexOf(" ");
      int quote = commandCandidate.indexOf("\"");
      if ((quote > 0) && (quote < space)) {
        space = quote;
      }

      if (space > 0) {
        commandCandidate = commandCandidate.substring(0, space);
      }
      commandCandidate = commandCandidate.trim();
      if (commandCandidate.length() > 0) {
        if (command != null) {
          LOGGER
              .info("extractCommand replacing " + command + " with " + commandCandidate);
        }
        command = commandCandidate;
      }
    }
    return command;
  }

  /*
   * This is the first step of processing the requested channel contents. As it is
   * returned back as an array we can iterate over it looking for an indication of
   * the construct wanted before we tease the specific string values out of the
   * message
   */
  private String findInResponse(String path, String identifier, String descriptor) {
    LOGGER
        .fine("checking response with " + path + " identifier " + identifier + " for descriptor " + descriptor);

    String command = null;
    List result = myJsonCtx.read(path);
    if (!result.isEmpty()) {

      Iterator iter = result.iterator();
      while (iter.hasNext()) {
        String test = (String) iter.next();
        LOGGER
            .finer("findInResponse found json candidate for " + descriptor + " content to scan>>>>>>" + test);

        if (test.contains(identifier)) {
          LOGGER.finer("findInResponse testing:" + test);
          command = extractCommand(identifier, test, command);
        }
      }
    }

    return command;

  }

  /*
   * Look for a bit ofg JSON to accelerate the process of retrieving the command
   * and node values
   */
  private boolean okResponse() {
    Boolean result = myJsonCtx.read(FIND_OK_JSONPATH);
    return result.booleanValue();
  }

  /*
   * This method orchestrates the process of trying to locate a response, and when
   * not found we will wait a configurable amount of time, and starting again.
   */
  @Override
  public FLBCommunication checkForAction() {
    Client client = null;
    try {
      client = ClientBuilder.newClient();
      WebTarget target = client.target(SLACK_HISTORY_URL);
      Response response = target.request()
          .header(HttpHeaders.AUTHORIZATION, BEARER_SLACK_HDR + mySlackToken)
          .post(Entity.form(setConversationReadParams()));

      String payload = response.readEntity(String.class);
      client.close();

      myResults.addRawEvent("raw payload ...\n" + payload);
      this.myJsonCtx = JsonPath.parse(payload);
      myResults.addRawEvent("JsonPath evaluation = " + okResponse());

      if (okResponse()) {
        myResults.setCommand(findInResponse(FIND_COMMANDS_JSONPATH, FLB, "executeCommand"));
        myResults.setFLBNode(findInResponse(FIND_COMMANDS_JSONPATH, FLBNODE, "executionNode"));

      }

    } catch (Exception error) {
      myResults.addRawEvent(
          "Slack call error for \n:" + error.toString() + myAlertId + "\n" + error.getStackTrace()[0].toString()
              + "\n");
    } finally {
      if (client != null) {
        client.close();
      }
    }
    return myResults;
  }

  /*
   * Provide the means to send messages to the social channel, so that we can
   * confirm when we've managed to signal Fluent Bit
   */
  public boolean sendMessage(String message) {
    boolean success = false;
    Client client = null;

    try {
      client = ClientBuilder.newClient();
      WebTarget target = client.target(SLACK_WRITE_URL);
      Response response = target.request()
          .header(HttpHeaders.AUTHORIZATION, BEARER_SLACK_HDR + mySlackToken)
          .post(Entity.form(setConversationWriteParams(message)));

      String payload = response.readEntity(String.class);
      LOGGER.finer("sendMessage: response=" + payload);
      client.close();
    } catch (Exception err) {
      LOGGER.warning("sendMessage caught error: " + err);
    }
    return success;

  }
}
