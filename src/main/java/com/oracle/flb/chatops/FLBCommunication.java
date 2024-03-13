package com.oracle.flb.chatops;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FLBCommunication {

  private List<String> rawEvents = new ArrayList<String>();
  private String command = null;
  private String FLBNode = null;

  /*
   * This class exists largely to act as a carrier for the data elements we need
   * to pass around. In includes the ability to populate an array of strings
   * reflecting event related details we may wish to pass around.
   */
  // @Override
  public List<String> getRawEvents() {
    return this.rawEvents;
  }

  public void addRawEvent(String event) {
    if (this.rawEvents == null) {
      rawEvents = new ArrayList<String>();
    }
    this.rawEvents.add(event);
  }

  public void setRawEvents(List<String> rawEvents) {
    this.rawEvents = rawEvents;
  }

  public String getCommand() {
    return this.command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getFLBNode() {
    return this.FLBNode;
  }

  public void setFLBNode(String FLBNode) {
    this.FLBNode = FLBNode;
  }

  public boolean canAction() {
    return ((command != null) && (FLBNode != null) && (command.length() > 0) && (FLBNode.length() > 0));
  }

  /*
   * Takes the array of string and formats it into a readable form.
   */
  private String rawEventsStr() {
    String formattedStr = "{[";
    Iterator iter = rawEvents.iterator();
    int ctr = 0;
    while (iter.hasNext()) {
      if (formattedStr.length() > 2) {
        formattedStr = formattedStr + ",";
      }
      String obj = (String) iter.next();
      {
        formattedStr = formattedStr + "{" + Integer.toString(ctr) + ":'" + obj + "'}\n";
      }
      ctr++;
    }
    return formattedStr + "]}";
  }

  /*
   * This constructs a JSON string that reflects the key values we need to look at
   * for logging the object state
   */
  public String summaryString() {
    return "{\"isOk\" : \"" + canAction() + "\", \"command\":\"" + getCommand() + "\", \"FLBNode\"=\"" + getFLBNode()
        + "\"}";
  }

  /*
   * implementation of the standard method
   */
  public String toString() {
    return "{" +
        "\"Summary\" = " + summaryString() + ",\n" +
        ", rawEvents=" + rawEventsStr() +
        "}";
  }

}
