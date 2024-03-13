package com.oracle.flb.chatops;

import java.util.Map;

/*
 * Simple class to hide the creation of tbe correct implementation of the Actions class
 * Currently only have 1 implementation - slack
 * We've not adopted a reflection approach as makes it harder to make the code compile
 * to native using FraalVM which requires extra work to handle reflection
 */
public class SocialActionsFactory {
  private static boolean propsNotPrimed = true;

  /*
   * Produce the appropriate implementation of the Actions interface
   * if this is the first execution then go use the static actions method to
   * capture all the additional configuration properties The map are the
   * accumulated properties so far. In future we cna use this to drive the
   * creation of the right implementation
   */
  public static ActionsImplInterface getSocialChannelsActionImpl(Map<String, String> env) {
    if (propsNotPrimed) {
      SlackActions.addPropertiesStatic(env);
      propsNotPrimed = false;
    }
    return new SlackActions(env);
  }

  /*
   * provide a static abstracted method to call the load environment variables
   * with the specific or defaulted values for the relevant actions implementation
   */
  public static Map<String, String> addProperties(Map<String, String> env) {
    if (propsNotPrimed) {
      return SlackActions.addPropertiesStatic(env);
    }
    return env;
  }
}
