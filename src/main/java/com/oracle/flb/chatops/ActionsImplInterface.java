package com.oracle.flb.chatops;

import java.util.Map;

/*
 * This interface defines the operations needed for the Action 
 * object which will connect and communicate with the relevant 
 * social channel to communicate with the ops team
 */
public interface ActionsImplInterface {

  /*
   * Get the implementation to add any specific environment values needed. By
   * using the approach of passing in and out the map, we can enrich or override
   * based on the actions implementation
   */
  Map<String, String> addProperties(Map<String, String> config);

  /*
   * This is the method that when prompted will interogate the social channel for
   * any responses to the provided issue which can be be translated back to a
   * command for Fluent Bit to receive over HTTP.
   * The result needs to contain GLBNode: followed by the node to address (without
   * spaces) and FLBCmd:followed by the name of the command to be run by the FLB
   * Node (again no white space allowed)
   */
  FLBCommunication checkForAction();

  /*
   * Sends the message to the social channel, returning a boolean flag indicating
   * whether it was successful or not.
   */
  boolean sendMessage(String message);

}