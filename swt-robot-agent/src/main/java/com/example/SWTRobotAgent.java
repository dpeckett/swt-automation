package com.example;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// SWTRobotAgent is a Java agent that listens for commands on a socket and interacts with the
// application's UI using SWTBot.
public class SWTRobotAgent {
  private static final Logger logger = LoggerFactory.getLogger(SWTRobotAgent.class);

  private static ServerSocket serverSocket;
  private static Display display;
  // The top level bot that interacts with the Display.
  private static SWTBot bot;
  // A child bot that interacts with the selected shell.
  private static SWTBot childBot;
  private static ScheduledExecutorService scheduler;

  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      int port = Integer.parseInt(agentArgs);
      serverSocket = new ServerSocket(port);

      scheduler = Executors.newScheduledThreadPool(2);

      scheduler.submit(
          () -> {
            logger.info("Waiting for display to be available");
            waitForDisplay();

            scheduler.scheduleAtFixedRate(
                SWTRobotAgent::monitorDisplay, 0, 10, TimeUnit.MILLISECONDS);

            bot = new SWTBot();
            childBot = bot;

            logger.info("SWTRobotAgent listening for commands on port {}", port);
            listenForCommands();
          });
    } catch (Exception e) {
      logger.error("Failed to start SWTRobotAgent", e);
      shutdownScheduler();
    }
  }

  private static void waitForDisplay() {
    while (true) {
      try {
        display = SWTUtils.display();
        if (display != null) {
          break;
        }
      } catch (Exception ignored) {
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private static void listenForCommands() {
    try {
      while (true) {
        try (Socket socket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

          String command;
          while ((command = in.readLine()) != null) {
            processCommand(command, out);
          }

        } catch (SocketException e) {
          if (serverSocket.isClosed()) {
            // We are shutting down.
            return;
          } else {
            logger.warn("Error while listening for commands", e);
          }
          return;
        } catch (Exception e) {
          logger.warn("Error while listening for commands", e);
        }
      }
    } finally {
      shutdownScheduler();
    }
  }

  // TODO: replace this with a real API. I'd probably use gRPC for this.
  private static void processCommand(String command, PrintWriter out) {
    String[] parts = command.split(" ");
    try {
      switch (parts[0]) {
        case "SWITCH_SHELL":
          switchShell(parts[1], out);
          break;
        case "CLICK_BUTTON":
          clickButton(parts[1], out);
          break;
        default:
          out.println("UNKNOWN COMMAND");
          logger.warn("Received unknown command: {}", parts[0]);
      }
    } catch (Exception e) {
      out.println("ERROR: " + e.getMessage());
      logger.error("Error processing command '{}'", command, e);
    }
  }

  private static void switchShell(String shellTitle, PrintWriter out) {
    try {
      SWTBotShell shell = bot.shell(shellTitle);
      childBot = shell.bot();
      out.println("OK");
      logger.info("Switched to shell with title '{}'", shellTitle);
    } catch (WidgetNotFoundException e) {
      out.println("ERROR: Shell with title '" + shellTitle + "' not found");
      logger.warn("Shell with title '{}' not found", shellTitle, e);
    }
  }

  private static void clickButton(String buttonLabel, PrintWriter out) {
    try {
      SWTBotButton button = childBot.button(buttonLabel);
      button.click();
      out.println("OK");
      logger.info("Clicked button with label '{}'", buttonLabel);
    } catch (WidgetNotFoundException e) {
      out.println("ERROR: Button with label '" + buttonLabel + "' not found");
      logger.warn("Button with label '{}' not found", buttonLabel, e);
    }
  }

  private static void monitorDisplay() {
    if (display != null && display.isDisposed()) {
      logger.info("Display disposed. Stopping SWTRobotAgent");
      closeServerSocket();
      shutdownScheduler();
    }
  }

  private static void closeServerSocket() {
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      logger.error("Failed to close server socket", e);
    }
  }

  private static void shutdownScheduler() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      logger.info("Scheduler shutdown completed");
    }
  }
}
